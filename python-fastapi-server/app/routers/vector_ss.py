import logging
from fastapi import APIRouter, HTTPException
from typing import List

from ..schemas.vector_ss import SimilaritySearchRequest, SimilaritySearchResponse
from ..services import vector_ss_service

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/vector_ss",
    tags=["Vector Similarity Search"]
)

@router.post(
    "/pks",
    response_model=SimilaritySearchResponse,
    summary="추출된 키워드들로 유사도 검색 후 metadata 에서 pk들 추출",
    description="텍스틀 부터 추출한 도메인과 키워드로 유사도가 높은 meatdata의 pk를 찾아 좋은 답변을 할 수 있게 해준다."
)
async def get_pks_by_similarity_search(request: SimilaritySearchRequest):
    """
    유사도 검색 서비스 함수를 호출하고 결과를 반환합니다.

    - **request**: `SimilaritySearchRequest` 스키마에 맞는 요청 본문.
    - **returns**: 추출된 pk들 문자열 리스트.
    - **raises**: 서비스 계층에서 예외 발생 시 500 Internal Server Error.
    """
    logger.info(f"\nvector_ss 요청\n요청 내용: {request}\n")
    try:
        # 비동기 서비스 함수를 await로 호출합니다.
        result_pks = await vector_ss_service.similarity_search(request)
        logger.info(f"\nVectorDB 유사도 검색 완료\n==========결과==========\n{result_pks}\n")
        return result_pks
    except Exception as e:
        # 서비스에서 발생한 상세한 예외 메시지를 클라이언트에게 전달합니다.
        logger.error(e)
        raise HTTPException(
            status_code=500,
            detail=f"키워드 추출 중 서버 오류 발생: {e}"
        )