from fastapi import APIRouter, HTTPException, Depends
from typing import List

# 서비스 함수와 스키마, 상수를 가져옵니다.
from ..services import extract_service
from ..schemas.extract import DomainExtractRequest, KeywordExtractRequest, CustomKeywordRequest, SimilaritySearchRequest
from ..core.constants import Domains

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
    - **raises**: 서비스 계층에서 예외 발생 시 500 Internal Server Error.
    """
    try:
        # 비동기 서비스 함수를 await로 호출합니다.
        result_domain = await extract_service.extract_domain(request)
        return result_domain
    except Exception as e:
        # 서비스에서 발생한 상세한 예외 메시지를 클라이언트에게 전달합니다.
        raise HTTPException(
            status_code=500,
            detail=f"도메인 추출 중 서버 오류 발생: {e}"
        )


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
    - **raises**: 서비스 계층에서 예외 발생 시 500 Internal Server Error.
    """
    try:
        # 비동기 서비스 함수를 await로 호출합니다.
        result_keywords = await extract_service.extract_keywords(request)
        return result_keywords
    except Exception as e:
        # 서비스에서 발생한 상세한 예외 메시지를 클라이언트에게 전달합니다.
        raise HTTPException(
            status_code=500,
            detail=f"키워드 추출 중 서버 오류 발생: {e}"
        )

# "주문 접수처"에 해당하는 API 엔드포인트 생성
@router.post(
    "/keyword/custom",
    response_model=List[str],
    summary="사용자 정의 프롬프트로 키워드 추출",
    description="주어진 텍스트(context)에서 검색에 유용한 키워드 목록을 LLM을 통해 추출합니다."
)
async def handle_custom_keyword_extraction(request: CustomKeywordRequest):
    """
    사용자가 직접 정의한 프롬프트 템플릿을 사용하여
    주어진 텍스트(context)에서 키워드를 추출합니다.
    """
    try:
        # 서비스 계층의 핵심 로직을 호출합니다.
        extracted_keywords = await extract_service.extract_with_custom_prompt(
            request=request
        )
        # 성공 시, 키워드 리스트를 반환합니다.
        return extracted_keywords
    except Exception as e:
        # 서비스에서 오류가 발생하면, 서버가 멈추지 않고 500 에러와 함께 원인을 반환합니다.
        raise HTTPException(
            status_code=500,
            detail=f"사용자 정의 키워드 추출 중 서버 오류 발생: {e}"
        )


@router.post(
    "/pks",
    response_model=List[str],
    summary="추출된 키워드들로 유사도 검색 후 metadata 에서 pk들 추출",
    description="유사도 검색 후 PK 추출"
)
async def handle_similarity_search_pks(request: SimilaritySearchRequest):
    """
    유사도 검색 함수 호출 후 결과를 반환한다.

    - **request**: `MetadataExtractRequest` 스키마에 맞는 요청 본문.
    - **returns**: 추출된 pk들 문자열의 리스트.
    - **raises**: 서비스 계층에서 예외 발생 시 500 Internal Server Error.
    """
    try:
        # 'extract_service' 모듈의 'similarity_search' 함수를 직접 호출
        return extract_service.similarity_search(request)
    except Exception as e:
        # 서비스에서 발생한 상세한 예외 메시지를 클라이언트에게 전달합니다.
        raise HTTPException(
            status_code=500,
            detail=f"키워드 추출 중 서버 오류 발생: {e}"
        )
