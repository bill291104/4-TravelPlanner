# app/services/extract_service.py
from ..schemas.extract import DomainExtractRequest, KeywordExtractRequest
from ..llm.client import get_llm
from ..core.constants import Domains

from typing import List
from langchain_core.prompts import ChatPromptTemplate
from openai import APIError, RateLimitError

async def extract_domain(request: DomainExtractRequest) -> Domains:
    """LLM을 사용하여 주어진 텍스트가 어떤 도메인에 가장 적합한지 분류합니다.

    Args:
        request (DomainExtractRequest): 사용자의 텍스트, 프롬프트 템플릿, 분류할 도메인 목록을 포함합니다.

    Returns:
        Domains: 분류된 도메인 열거형 멤버.

    Raises:
        Exception: LLM API 호출에 실패하거나, LLM의 응답을 유효한 도메인으로 파싱할 수 없을 때 발생합니다.
    """
    template = ChatPromptTemplate.from_template(request.prompt_template)
    prompt = template.format_messages(
        context=request.context,
        domains=",\n".join(domain.value + ": " + domain.get_description() for domain in request.domains)
    )

    llm = get_llm(temperature=0.1)

    # 1. API 호출을 먼저 시도하고 그 결과를 변수에 저장합니다.
    try:
        response = await llm.ainvoke(prompt)
        response_content = response.content.strip()
    except (APIError, RateLimitError) as e:
        raise Exception(f"OpenAI API 호출 중 오류가 발생했습니다. 원본 오류: {e}")

    # 2. 이제 response_content 변수가 확실히 존재하므로, 그 값을 파싱합니다.
    try:
        return Domains(response_content)
    except ValueError:
        # 파싱 실패 시, 이미 선언된 response_content를 안전하게 사용합니다.
        domain_options = ", ".join([d.value for d in request.domains])
        error_message = (
            f"도메인 분류에 실패했습니다. "
            f"LLM이 반환한 값('{response_content}')이 유효한 도메인({domain_options}) 목록에 없습니다."
        )
        raise Exception(error_message)


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
    target_domain = request.target_domain
    prompt = template.format_messages(
        context=request.context,
        domain=target_domain.value + ": " + target_domain.get_description()
    )

    llm = get_llm(temperature=0.1)

    try:
        response = await llm.ainvoke(prompt)
        response_content = response.content
        # ✨ LLM 응답이 비어있거나, 파싱 결과가 없을 경우를 명시적으로 확인
        if not response_content:
            raise Exception("키워드 추출에 실패했습니다. LLM이 빈 응답을 반환했습니다.")

        keywords = [keyword.strip() for keyword in response_content.split(",") if keyword.strip()]

        if not keywords:
            raise Exception(
                f"키워드 파싱에 실패했습니다. LLM의 응답('{response_content}')에서 유효한 키워드를 찾을 수 없습니다."
            )

        return keywords
    except (APIError, RateLimitError) as e:
        # ✨ OpenAI API 관련 오류 발생 시, 원본 오류를 포함하여 예외를 다시 발생시킴
        raise Exception(f"키워드 추출 중 OpenAI API 호출에 오류가 발생했습니다. 원본 오류: {e}")