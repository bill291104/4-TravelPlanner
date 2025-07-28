# app/services/extract_service.py
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
    PromptTemplateError
)

async def extract_domain(request: DomainExtractRequest) -> Domains:
    """LLM을 사용하여 주어진 텍스트가 어떤 도메인에 가장 적합한지 분류합니다.

    Args:
        request (DomainExtractRequest): 사용자의 텍스트, 프롬프트 템플릿, 분류할 도메인 목록을 포함합니다.

    Returns:
        Domains: 분류된 도메인 열거형 멤버.

    Raises:
        Exception: LLM API 호출에 실패하거나, LLM의 응답을 유효한 도메인으로 파싱할 수 없을 때 발생합니다.
    """

    # 1. 템플릿 유효성 검사
    template = ChatPromptTemplate.from_template(request.prompt_template)

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

        if not response_content:
            raise LLMResponseError(detail="도메인 분류에 실패했습니다. LLM이 빈 응답을 반환했습니다.")

    except (APIError, RateLimitError) as e:
        raise LLMAPIError(detail=f"OpenAI API 호출 오류: {e}") from e

    except Exception as e:
        raise LLMServiceError(detail=f"LLM 서비스 예외 발생: {e}") from e

    # 3. 도메인 파싱
    try:
        return Domains(response_content)

    except ValueError:
        domain_options = ", ".join([d.value for d in request.domains])
        raise LLMParsingError(
            detail=f"'{response_content}'은 유효한 도메인({domain_options}) 중 하나가 아닙니다."
        )


async def extract_keywords(request: KeywordExtractRequest) -> List[str]:
    """특정 도메인으로 분류된 텍스트에서 검색에 사용할 핵심 키워드를 여러 개 추출합니다.

    Args:
        request (KeywordExtractRequest): 사용자의 텍스트, 프롬프트 템플릿, 대상 도메인을 포함합니다.

    Returns:
        List[str]: 추출된 키워드 문자열의 리스트.

    Raises:
        Exception: LLM API 호출에 실패하거나, 응답 형식이 예상과 다를 때 발생합니다.
    """
    template = ChatPromptTemplate.from_template(request.prompt_template)

    if "{context}" not in request.prompt_template or "{domain}" not in request.prompt_template:
        raise PromptTemplateError(detail="프롬프트에 'context' 또는 'target_domain' 변수가 누락되었습니다.")

    target_domain = request.target_domain
    prompt = template.format_messages(
        context=request.context,
        domain=target_domain.value + ": " + target_domain.get_description()
    )

    llm = get_llm(temperature=0.1)

    try:
        response = await llm.ainvoke(prompt)
        response_content = response.content.strip() # LLM은 종종 불필요한 공백/줄바꿈 포함하기에 문자열 앞뒤 공백, 줄바꿈(\n), 탭 등을 제거
        # ✨ LLM 응답이 비어있거나, 파싱 결과가 없을 경우를 명시적으로 확인
        if not response_content:
            raise LLMResponseError(detail="키워드 추출에 실패했습니다. LLM이 빈 응답을 반환했습니다.")

        keywords = [keyword.strip() for keyword in response_content.split(",") if keyword.strip()]

        if not keywords:
            raise LLMParsingError(
                detail=f"LLM 응답에서 유효한 키워드를 찾을 수 없습니다: '{response_content}'"
            )
        return keywords

    except (APIError, RateLimitError) as e:
        raise LLMAPIError(detail=f"키워드 추출 중 OpenAI API 오류: {e}") from e

    except Exception as e:
        raise LLMServiceError(detail=f"키워드 추출 중 예상치 못한 오류: {e}") from e