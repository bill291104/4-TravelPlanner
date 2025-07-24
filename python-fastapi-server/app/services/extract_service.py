# app/services/extract_service.py
import json
from ..schemas.extract import DomainExtractRequest, KeywordExtractRequest
from ..llm.client import get_llm
from ..core.constants import Domains

from typing import List
from langchain_core.prompts import ChatPromptTemplate
from openai import APIError, RateLimitError

from ..core.exceptions import (
    LLMAPIError,
    LLMParsingError,
    LLMResponseError,
    LLMServiceError,
    PromptTemplateError,
    UnsupportedDomainClassificationError
)

MAX_CONTENT_LENGTH_FOR_LLM = 10000

async def extract_domain(request: DomainExtractRequest) -> Domains:
    """LLM을 사용하여 주어진 텍스트가 어떤 도메인에 가장 적합한지 분류합니다.
    (A.1 PromptTemplateError, B.1 UnsupportedDomainClassificationError 적용)
    """
    template = ChatPromptTemplate.from_template(request.prompt_template)

    # (A.1) 필수 변수 누락 검증 (예시: context, domains 변수)
    if "{context}" not in request.prompt_template or "{domains}" not in request.prompt_template:
        raise PromptTemplateError(detail="프롬프트 템플릿에 'context' 또는 'domains' 변수가 누락되었습니다.")

    prompt = template.format_messages(
        context=request.context,
        domains=", ".join(d.value for d in request.domains) # 예: "place, restaurant"
    )

    llm = get_llm(temperature=0.1)

    try:
        response = await llm.ainvoke(prompt)
        response_content = response.content.strip()

        # LLM 응답이 비어있는 경우
        if not response_content:
            raise LLMResponseError(detail="도메인 분류에 실패했습니다. LLM이 빈 응답을 반환했습니다.")

    except (APIError, RateLimitError) as e:
        raise LLMAPIError(detail=f"OpenAI API 호출 중 오류가 발생했습니다. 원본 오류: {e}") from e
    except Exception as e:
        # LLM 서비스 관련 다른 예상치 못한 오류
        raise LLMServiceError(detail=f"도메인 추출 중 예상치 못한 LLM 서비스 오류: {e}") from e

    # --- JSON 파싱 로직 추가 domain용 ---
    # 이 부분은 extract_domain의 프롬프트가 JSON을 반환하도록 변경될 경우 필요
    # 현재 extract_domain은 단일 문자열을 기대하므로, 만약 프롬프트 템플릿이 JSON을 요구하도록 변경되지 않았다면
    # 해당 JSON 파싱 로직은 필요 없음.
    parsed_domain_name = None
    try:
        # 마크다운 코드 블록 제거
        if response_content.startswith("```json") and response_content.endswith("```"):
            response_content = response_content.removeprefix("```json").removesuffix("```").strip()
        elif response_content.startswith("```json\n") and response_content.endswith("\n```"):
            response_content = response_content[len("```json\n"):-len("\n```")].strip()

        parsed_json = json.loads(response_content) #JSON 문자를 파이썬 객체로 변환

        if isinstance(parsed_json, dict) and "domain" in parsed_json:
            parsed_domain_name = parsed_json["domain"].strip()
            print(f"DEBUG POINT 1: parsed_domain_name after strip: '{parsed_domain_name}' (type: {type(parsed_domain_name)})")
        else:
            # 예상한 JSON 구조가 아닐 경우
            raise LLMParsingError(f"LLM 응답 JSON에서 'domain' 키를 찾을 수 없거나 형식이 올바르지 않습니다: {response_content}")

    except json.JSONDecodeError as e:
        raise LLMParsingError(f"LLM 응답이 유효한 JSON 형식이 아닙니다. 원본 오류: {e}. 응답 내용: '{response_content}'") from e
    # --- JSON 파싱 로직 끝 ---

    # 2. (B.1) UnsupportedDomainClassificationError 적용 방안
    try:
        # parsed_domain_name는 'accom' 같은 순수 문자열
        if parsed_domain_name is None: # 혹시 모를 안전 장치(LLMParsingError 발생 후) 도달하면 안되지만 할 수도 있기에
            raise ValueError("LLM 응답에서 유효한 도메안 값을 추출할 수 없습니다.")

        classified_domain = Domains(parsed_domain_name)
    except ValueError:
        domain_options = ", ".join([d.value for d in request.domains])
        error_message = (
            f"도메인 분류에 실패했습니다. "
            f"LLM이 반환한 값('{parsed_domain_name}')이 유효한 도메인({domain_options}) 목록에 없습니다."
        )
        raise UnsupportedDomainClassificationError(
            classified_domain=parsed_domain_name, # 어떤 도메인이 분류 되었는지?
            detail=error_message
        )

    return classified_domain


async def extract_keywords(request: KeywordExtractRequest) -> List[str]:
    """
    특정 도메인으로 분류된 텍스트에서 검색에 사용할 핵심 키워드를 여러 개 추출합니다.
    (A.1 PromptTemplateError 적용)
    """

    template = ChatPromptTemplate.from_template(request.prompt_template)

    # (A.1) 필수 변수 누락 검증 (예시: context, target_domain 변수)
    # '{domain}' 대신 '{target_domain}' 변수를 확인하도록 수정
    if "{context}" not in request.prompt_template or "{target_domain}" not in request.prompt_template:
        raise PromptTemplateError(detail="프롬프트 템플릿에 'context' 또는 'target_domain' 변수가 누락되었습니다.")

    target_domain_enum = request.target_domain # KeywordExtractRequest에서 받은 target_domain (Domains Enum)

    # 프롬프트에 전달할 'target_domain' 변수의 값 생성
    # 예: "restaurant: 레스토랑 관련 정보"
    formatted_target_domain_for_prompt = target_domain_enum.value + ": " + target_domain_enum.get_description()

    prompt = template.format_messages(
        context=request.context,
        target_domain=formatted_target_domain_for_prompt # <-- 여기 변수명 'domain' 대신 'target_domain' 사용
    )

    llm = get_llm(temperature=0.1)

    try:
        response = await llm.ainvoke(prompt)
        response_content = response.content

        if not response_content:
            raise LLMResponseError("키워드 추출에 실패했습니다. LLM이 빈 응답을 반환했습니다.")

        # --- JSON 파싱 로직 추가 시작 ---
        # 1. 마크다운 코드 블록 제거 (선택적이지만 LLM이 이렇게 응답하는 경우가 많으므로 처리)
        # LLM 응답이 "'''json\n...\n'''" 형태일 경우
        if response_content.startswith("'''json") and response_content.endswith("'''"):
            response_content = response_content.removeprefix("'''json").removesuffix("'''").split()
        elif response_content.startswith("```json\n") and response_content.endswith("\n```"): # 간혹 이런 경우도 있음
            response_content = response_content[len("```json\n"):-len("\n```")].strip()

        keywords_from_json = []
        try:
            parsed_json = json.loads(response_content) #JSON 문자를 파이썬 객체로 변환

            # LLM이 어떤 JSON 구조를 반환할지 명확하지 않으므로, 여러 가능성을 고려
            # 현재 응답은 {"restaurant": {"location": "부산", "type": "맛집"}} 형태
            # 각 도메인별 예상되는 JSON 구조에 따라 파싱 로직을 분기한다.

            # Domains Enum을 활용하여 각 도메인에 맞는 파싱 로직 구현
            if request.target_domain == Domains.RESTAURANT:
                # 예시: {"restaurant": {"location": "부산", "type": "맛집"}}
                if "restaurant" in parsed_json and isinstance(parsed_json["restaurant"], dict):
                    info = parsed_json["restaurant"]
                    for key, value in info.items():
                        if isinstance(value, str) and value.strip():
                            keywords_from_json.append(value.strip())
                # 또는 {"restaurant": ["부산 맛집", "서면"]} 형태일 경우
                elif "restaurant" in parsed_json and isinstance(parsed_json["restaurant"], list):
                    for item in parsed_json["restaurant"]:
                        if isinstance(item, str) and item.strip():
                            keywords_from_json.append(item.strip())

                elif request.target_domain == Domains.ACCOM:
                    # 예시: {"accommodation": {"type": "호텔", "city": "서울"}}
                    if "accommodation" in parsed_json and isinstance(parsed_json["accommodation"], dict):
                        info = parsed_json["accommodation"]
                        for key, value in info.items():
                            if isinstance(value, str) and value.strip():
                                keywords_from_json.append(value.strip())

                # 또는 {"accommodation": ["호텔", "서울 숙소"]} 형태일 경우를 대비
                elif "accommodation" in parsed_json and isinstance(parsed_json["accommodation"], list):
                    for item in parsed_json["accommodation"]:
                        if isinstance(item, str) and item.strip():
                            keywords_from_json.append(item.strip())

                elif request.target_domain == Domains.PLACE:
                    # 예시: {"place": {"name": "경복궁", "category": "궁궐"}}
                    if "place" in parsed_json and isinstance(parsed_json["place"], dict):
                        info = parsed_json["place"]
                        for key, value in info.items():
                            if isinstance(value, str) and value.strip():
                                keywords_from_json.append(value.strip())
                # 또는 {"place": ["경복궁", "남산타워"]} 형태일 경우를 대비
                elif "place" in parsed_json and isinstance(parsed_json["place"], list):
                    for item in parsed_json["place"]:
                        if isinstance(item, str) and item.strip():
                            keywords_from_json.append(item.strip())

                # 만약 LLM이 도메인 키 대신 일반적인 'keywords' 키를 반환할 경우를 대비 (fallback)
            elif "keywords" in parsed_json and isinstance(parsed_json["keywords"], list):
                for kw in parsed_json["keywords"]:
                    if isinstance(kw, str) and kw.strip():
                        keywords_from_json.append(kw.strip())

            keywords =[k for k in keywords_from_json if k] # 빈 문자열 제거
            if not keywords: # JSON을 파싱했으나 유효한 키워드가 없는 경우
                raise LLMParsingError(f"JSON 파싱 후 유효한 키워드를 찾을 수 없습니다. LLM 응답: '{response_content}'")

        except json.JSONDecodeError as e:
            raise LLMParsingError(f"LLM 응답이 유효한 JSON 형식이 아닙니다. 원본 오류: {e}. 응답 내용: '{response_content}'") from e
        # --- JSON 파싱 로직 끝 ---

        return keywords

    except (APIError, RateLimitError) as e:
        raise LLMAPIError(f"키워드 추출 중 OpenAI API 호출에 오류가 발생했습니다. 원본 오류: {e}") from e
    except LLMResponseError as e:
        # LLM 응답이 비어있을 때 발생하는 오류를 직접 잡아서 다시 던져.
        raise e
    except LLMParsingError as e:
        # LLM 응답 파싱 실패 오류를 여기서 직접 잡아서 다시 던짐
        raise e
    except Exception as e:
        # 위에서 잡지 못한 모든 다른 예상치 못한 오류를 잡음
        raise LLMServiceError(detail=f"키워드 추출 중 예상치 못한 LLM 서비스 오류: {e}") from e
