# app/dependencies.py
from fastapi import HTTPException
from .core.constants import Domains
from .db.session import db_collections

def get_db_collection(domain: Domains):
    """
    요청된 도메인에 해당하는 DB 컬렉션을 반환하는 의존성 함수
    """
    collection = db_collections.get(domain.value)
    if not collection:
        raise HTTPException(status_code=503, detail="Database collection not available.")
    return collection