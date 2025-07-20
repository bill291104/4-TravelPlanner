# app/routers/embedding.py
from fastapi import APIRouter, Depends, HTTPException, status, Query
from ..schemas.embedding import EmbeddingRequest
from ..services import embedding_service
from ..dependencies import get_db_collection
from ..core.constants import Domains
from typing import List

router = APIRouter(prefix="/embedding", tags=["Embedding"])

@router.post("", status_code=status.HTTP_204_NO_CONTENT)
async def embedding_travel_data(
        request: EmbeddingRequest,
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    try:
        embedding_service.create_embedding(request, db_collection)
        print(f"Document added to '{domain.value}' collection.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Embedding failed: {e}")

@router.post("/batch", status_code=status.HTTP_204_NO_CONTENT)
async def embedding_data_batch(
        requests: List[EmbeddingRequest], # ✨ 요청 본문으로 EmbeddingRequest의 리스트를 받습니다.
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    try:
        embedding_service.create_embeddings_batch(requests, db_collection)
        print(f"{len(requests)} documents added to '{domain.value}' collection.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Batch embedding failed: {e}")

@router.delete("/batch", status_code=status.HTTP_204_NO_CONTENT)
async def delete_embedded_data_batch(
        domain: Domains,
        pks: List[int] = Query(..., description="삭제할 pk 목록"), # ✨ 쿼리 파라미터로 pk 리스트를 받습니다.
        db_collection = Depends(get_db_collection)
):
    try:
        embedding_service.delete_embeddings_batch(pks, db_collection)
        print(f"{len(pks)} document with pks={pks} deleted from '{domain.value}' collection.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Batch delete failed: {e}")

@router.delete("/{pk}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_embedded_data(
        pk: int,
        domain: Domains,
        db_collection = Depends(get_db_collection)
):
    """
    pk에 해당하는 임베딩 데이터를 삭제합니다.
    """
    try:
        embedding_service.delete_embedding(pk, db_collection)
        print(f"Document with pk='{pk}' deleted from '{domain.value}' collection.")
    except Exception as e:
        # 실제 운영에서는 더 상세한 로깅이 필요합니다.
        raise HTTPException(status_code=500, detail=f"Delete failed: {e}")