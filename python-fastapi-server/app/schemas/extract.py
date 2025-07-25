# app/schemas/extract.py
from typing import List, Optional
from pydantic import BaseModel, Field

from ..core.constants import Domains

class DomainExtractRequest(BaseModel):
    context: str
    domains: List[Domains]
    prompt_template:str

class KeywordExtractRequest(BaseModel):
    context:str
    target_domain: Domains
    prompt_template:str

# "설계도" 그리기
class CustomKeywordRequest(BaseModel):
    context: str
    domains: List[Domains]
    prompt_template: str

class PipelineInvokeRequest(BaseModel):
    """
    여행 데이터 추천 파이프라인 에이전트를 실행하기 위한 요청 스키마입니다.
    """
    keywords: List[str] = Field(
        ...,
        title="사용자 키워드 목록",
        description="사용자의 초기 요청에서 추출된 전체 키워드 리스트입니다.",
        example=["가족여행", "제주도", "맛집", "가성비", "한식"]
    )

    class Config:
        json_schema_extra = {
            "example": {
                "keywords": ["가족여행", "제주도", "맛집", "가성비", "한식"]
            }
        }

class PipelineInvokeResponse(BaseModel):
    """
    파이프라인 에이전트의 최종 결과 응답 스키마입니다.
    """
    pks: List[int] = Field(
        ...,
        title="최종 추천 PK 목록",
        description="모든 필터링을 거친 최종 추천 장소의 PK(정수) 리스트입니다.",
        example=[101, 504]
    )

    class Config:
        json_schema_extra = {
            "example": {
                "pks": [101, 504]
            }
        }