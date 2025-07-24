from builtins import len, Exception, int

import logging
from typing import List

from fastapi import APIRouter, Depends, HTTPException, status, Query
from ..schemas.embedding import EmbeddingRequest
from ..services import embedding_service
from ..dependencies import get_db_collection
from ..core.constants import Domains

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
    try:
        await embedding_service.create_embeddings_batch(requests, db_collection)
        # logger.info() 사용
        logger.info(f"{len(requests)}개의 문서를 '{domain.value}' 컬렉션에 성공적으로 임베딩했습니다.")
    except Exception as e:
        # 예외 발생 시 에러 로그 기록
        logger.error(f"배치 임베딩 실패: domain='{domain.value}', error='{e}'")
        raise HTTPException(status_code=500, detail=f"Batch embedding failed: {e}")

@router.delete("/batch", status_code=status.HTTP_204_NO_CONTENT)
async def delete_embedded_data_batch(
        domain: Domains,
        pks: List[int] = Query(..., description="삭제할 pk 목록"),
        db_collection = Depends(get_db_collection)
):
    try:
        await embedding_service.delete_embeddings_batch(pks, db_collection)
        logger.info(f"'{domain.value}' 컬렉션에서 {len(pks)}개의 문서를 성공적으로 삭제했습니다. (pks: {pks})")
    except Exception as e:
        logger.error(f"배치 삭제 실패: domain='{domain.value}', pks='{pks}', error='{e}'")
        raise HTTPException(status_code=500, detail=f"Batch delete failed: {e}")

# --- 단일 엔드포인트 ---

@router.post("", status_code=status.HTTP_204_NO_CONTENT)
async def embedding_travel_data(
        request: EmbeddingRequest,
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    try:
        await embedding_service.create_embedding(request, db_collection)
        logger.info(f"pk='{request.pk}' 문서를 '{domain.value}' 컬렉션에 성공적으로 임베딩했습니다.")
    except Exception as e:
        logger.error(f"단일 임베딩 실패: domain='{domain.value}', pk='{request.pk}', error='{e}'")
        raise HTTPException(status_code=500, detail=f"Embedding failed: {e}")

@router.delete("/{pk}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_embedded_data(
        pk: int,
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    try:
        await embedding_service.delete_embedding(pk, db_collection)
        logger.info(f"'{domain.value}' 컬렉션에서 pk='{pk}' 문서를 성공적으로 삭제했습니다.")
    except Exception as e:
        logger.error(f"단일 삭제 실패: domain='{domain.value}', pk='{pk}', error='{e}'")
        raise HTTPException(status_code=500, detail=f"Delete failed: {e}")

