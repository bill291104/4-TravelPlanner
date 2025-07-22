# app/services/extract_service.py
from ..schemas.extract import DomainExtractRequest, KeywordExtractRequest
from ..llm.client import get_llm
from ..core.constants import Domains

from typing import List
from langchain_core.prompts import ChatPromptTemplate, PromptTemplate # InputValidationException 완전히 제거
from openai import APIError, RateLimitError

from ..core.exceptions import (
    LLMAPIError,
    LLMParsingError,
    LLMResponseError,
    InvalidInputError,
    LLMServiceError,
    PromptTemplateError,
    UnsupportedDomainClassificationError,
    AmbiguousClassificationError
)

MAX_CONTENT_LENGTH_FOR_LLM = 10000

async def extract_domain(request: DomainExtractRequest) -> Domains:
    """LLM을 사용하여 주어진 텍스트가 어떤 도메인에 가장 적합한지 분류합니다.
    (A.1 PromptTemplateError, B.1 UnsupportedDomainClassificationError 적용)
    """
    # 1. (A.1) PromptTemplateError 적용 방안: 프롬프트 템플릿 유효성 검증
    try:
        template = ChatPromptTemplate.from_template(request.prompt_template)
    except ValueError as e: # ValueError만 잡도록 수정
        raise PromptTemplateError(detail=f"제공된 프롬프트 템플릿 형식이 올바르지 않습니다: {e}") from e

    # (A.1) 필수 변수 누락 검증 (예시: context, domains 변수)
    if "{context}" not in request.prompt_template or "{domains}" not in request.prompt_template:
        raise PromptTemplateError(detail="프롬프트 템플릿에 'context' 또는 'domains' 변수가 누락되었습니다.")

    prompt = template.format_messages(
        context=request.context,
        domains=",\n".join(domain.value + ": " + domain.get_description() for domain in request.domains)
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

    # 2. (B.1) UnsupportedDomainClassificationError 적용 방안
    try:
        classified_domain = Domains(response_content)
    except ValueError:
        domain_options = ", ".join([d.value for d in request.domains])
        error_message = (
            f"도메인 분류에 실패했습니다. "
            f"LLM이 반환한 값('{response_content}')이 유효한 도메인({domain_options}) 목록에 없습니다."
        )
        raise LLMParsingError(detail=error_message)

    return classified_domain


async def extract_keywords(request: KeywordExtractRequest) -> List[str]:
    """특정 도메인으로 분류된 텍스트에서 검색에 사용할 핵심 키워드를 여러 개 추출합니다.
    (A.1 PromptTemplateError 적용)
    """
    # 1. (A.1) PromptTemplateError 적용 방안: 프롬프트 템플릿 유효성 검증
    try:
        template = ChatPromptTemplate.from_template(request.prompt_template)
    except ValueError as e:
        raise PromptTemplateError(detail=f"제공된 프롬프트 템플릿 형식이 올바르지 않습니다: {e}") from e

    # (A.1) 필수 변수 누락 검증 (예시: context, target_domain 변수)
    # '{domain}' 대신 '{target_domain}' 변수를 확인하도록 수정
    if "{context}" not in request.prompt_template or "{target_domain}" not in request.prompt_template: # <-- 여기 수정
        raise PromptTemplateError(detail="프롬프트 템플릿에 'context' 또는 'target_domain' 변수가 누락되었습니다.") # <-- 여기 메시지 수정

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

        keywords = [keyword.strip() for keyword in response_content.split(",") if keyword.strip()]

        if not keywords:
            raise LLMParsingError(
                f"키워드 파싱에 실패했습니다. LLM의 응답('{response_content}')에서 유효한 키워드를 찾을 수 없습니다."
            )

        return keywords

    except (APIError, RateLimitError) as e:
        raise LLMAPIError(f"키워드 추출 중 OpenAI API 호출에 오류가 발생했습니다. 원본 오류: {e}") from e
    except Exception as e:
        raise LLMServiceError(detail=f"키워드 추출 중 예상치 못한 LLM 서비스 오류: {e}") from e
