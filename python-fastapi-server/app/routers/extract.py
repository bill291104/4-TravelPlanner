import logging
from fastapi import APIRouter, HTTPException
from typing import List

# 서비스 함수와 스키마, 상수를 가져옵니다.
from ..services import extract_service
from ..schemas.extract import DomainExtractRequest, KeywordExtractRequest
from ..core.constants import Domains

logger = logging.getLogger(__name__)

# service에서 온 모든 오류가 routers의 try-except 블록을 통해 500 Internal Server Error로 바뀜
# "/extract" 접두사를 가진 라우터를 생성합니다.
router = APIRouter(
    prefix="/extract",
    tags=["Extraction (LLM)"]  # Swagger UI 문서에서 그룹화될 태그 이름
)

@router.post(
    "/domain",
    response_model=Domains,
    summary="텍스트에서 도메인 분류",
    description="주어진 텍스트(context)가 어떤 도메인에 가장 적합한지 LLM을 통해 분류합니다."
)
async def handle_extract_domain(request: DomainExtractRequest):
    """
    도메인 추출 서비스 함수를 호출하고 결과를 반환합니다.

    - **request**: `DomainExtractRequest` 스키마에 맞는 요청 본문.
    - **returns**: 분류된 `Domains` 열거형 멤버.
    """
    # 비동기 서비스 함수를 await로 호출합니다.
    result_domain = await extract_service.extract_domain(request)
    logger.info(f"{request.context} 에서 \nDomain: '{result_domain}' 을 추출했습니다.")
    return result_domain

@router.post(
    "/keyword",
    response_model=List[str],
    summary="텍스트에서 키워드 추출",
    description="주어진 텍스트(context)에서 검색에 유용한 키워드 목록을 LLM을 통해 추출합니다."
)
async def handle_extract_keywords(request: KeywordExtractRequest):
    """
    키워드 추출 서비스 함수를 호출하고 결과를 반환합니다.

    - **request**: `KeywordExtractRequest` 스키마에 맞는 요청 본문.
    - **returns**: 추출된 키워드 문자열의 리스트.
    """
    # 비동기 서비스 함수를 await로 호출합니다.
    result_keywords = await extract_service.extract_keywords(request)
    logger.info(f"{request.context} 에서 \nKeywords: '{result_keywords}' 을 추출했습니다.")
    return result_keywords
