import logging
from typing import List

from fastapi import APIRouter, Depends, status, Query # HTTPException 임포트 제거
from ..schemas.embedding import EmbeddingRequest
from ..services import embedding_service
from ..dependencies import get_db_collection
from ..core.constants import Domains

# 라우터 파일의 try-except 블록 제거 : 서비스에서 발생한 에러를 라우터에서 다시 큰 except로 만들기 때문

# 이 파일에서 사용할 로거 인스턴스 생성
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/embedding", tags=["Embedding"])

# --- 배치 엔드포인트 ---
@router.post("/batch", status_code=status.HTTP_204_NO_CONTENT)
async def embedding_data_batch(
        requests: List[EmbeddingRequest],
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    # try-except 블록 제거: 서비스에서 발생한 예외는 main.py의 전역 핸들러로 전달
    await embedding_service.create_embeddings_batch(requests, db_collection)
    logger.info(f"{len(requests)}개의 문서를 '{domain.value}' 컬렉션에 성공적으로 임베딩했습니다.")

@router.delete("/batch", status_code=status.HTTP_204_NO_CONTENT)
async def delete_embedded_data_batch(
        domain: Domains,
        pks: List[int] = Query(..., description="삭제할 pk 목록"),
        db_collection = Depends(get_db_collection)
):
    # try-except 블록 제거
    await embedding_service.delete_embeddings_batch(pks, db_collection)
    logger.info(f"'{domain.value}' 컬렉션에서 {len(pks)}개의 문서를 성공적으로 삭제했습니다. (pks: {pks})")

# --- 단일 엔드포인트 ---

@router.post("", status_code=status.HTTP_204_NO_CONTENT)
async def embedding_travel_data(
        request: EmbeddingRequest,
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    # try-except 블록 제거
    await embedding_service.create_embedding(request, db_collection)
    logger.info(f"pk='{request.pk}' 문서를 '{domain.value}' 컬렉션에 성공적으로 임베딩했습니다.")

@router.delete("/{pk}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_embedded_data(
        pk: int,
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    # try-except 블록 제거
    await embedding_service.delete_embedding(pk, db_collection)
    logger.info(f"'{domain.value}' 컬렉션에서 pk='{pk}' 문서를 성공적으로 삭제했습니다.")