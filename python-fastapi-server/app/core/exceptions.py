# app/core/exceptions.py
# 모든 커스텀 예외 클래스
from fastapi import HTTPException, status

class CustomBaseException(HTTPException):
    """커스텀 예외의 기본 클래스"""
    def __init__(self, detail: str, status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR):
        super().__init__(status_code=status_code, detail=detail)
        self.name = self.__class__.__name__

# 임베딩 관련 예외
class EmbeddingServiceError(CustomBaseException):
    # 여행 관련 임베딩 시스템 전반에 알 수 없는 문제가 생겨 동작할 수 없을 때
    # 여행지 추천 임베딩 시스템이 초기화되지 않아 요청을 처리할 수 없습니다.
    def __init__(self, detail: str = "임베딩 서비스 오류가 발생했습니다.", status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR):
        super().__init__(detail=detail, status_code=status_code)

class EmbeddingCreationError(CustomBaseException):
    # 사용자의 여행 질문이나 여행지 정보를 임베딩으로 변환하여 저장하는 과정에서 오류가 발생했을 때
    # 사용자 '부산 맛집 추천' 질문 임베딩 생성 중 OpenAI API가 응답하지 않습니다.
    def __init__(self, detail: str = "임베딩 생성 중 오류가 발생했습니다."):
        super().__init__(detail=detail, status_code = status.HTTP_500_INTERNAL_SERVER_ERROR)

class EmbeddingDeletionError(CustomBaseException):
    # 특정 사용자 여행 질문 임베딩이나 여행지 정보 임베딩을 DB에서 삭제하려는데 실패했을 때
    def __init__(self, detail: str = "임베딩 삭제 중 오류가 발생했습니다."):
        super().__init__(detail=detail, status_code = status.HTTP_500_INTERNAL_SERVER_ERROR)

class TravelDocumentNotFoundError(CustomBaseException):
    """여행 관련(사용자 답변 키워드)를 찾을 수 없을 때 발생"""
    def __init__(self, document_id : int | str | None = None, detail: str = "요청한 사용자 답변 키워드 데이터를 찾을 수 없습니다."):
        if document_id is not None:
            detail = f"{detail}: Id{document_id}"
        super().__init__(detail=detail, status_code=status.HTTP_404_NOT_FOUND)

# DB 관련 예외
class DatabaseConnectionError(CustomBaseException):
    # 여행지 데이터베이스에 연결할 수 없어 추천 정보를 가져올 수 없습니다.
    def __init__(self, detail: str = "데이터베이스 연결에 실패하였습니다."):
        super().__init__(detail=detail, status_code=status.HTTP_503_SERVICE_UNAVAILABLE)

class InvalidInputError(CustomBaseException):
    # 사용자가 여행 관련 질문을 너무 짧게 하거나(빈 문자열), 필수 입력 필드를 빠뜨렸을 때.
    def __init__(self, detail: str = "유효하지 않은 입력입니다."):
        super().__init__(detail=detail, status_code=status.HTTP_400_BAD_REQUEST)

# LLM 관련 예외
class LLMServiceError(CustomBaseException):
    """LLM 서비스 전반에서 발생하는 오류 기본 클래스"""
    def __init__(self, detail: str = "LLM 서비스 오류가 발생했습니다.", status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR):
        super().__init__(detail=detail, status_code=status_code)

class LLMAPIError(LLMServiceError):
    """LLM API 호출 중 발생하는 오류 (예 : 키 문제, 인증 실패, Rate Limit 초과, 모델 오류)"""
    def __init__(self, detail: str = "LLM API 호출 중 오류가 발생했습니다.", status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR):
        super().__init__(detail=detail, status_code=status_code)

class LLMParsingError(LLMServiceError):
    """LLM 응답을 파싱하는 데 실패했을 때 발생하는 오류
    LLM이 여행 관련 질문에 대해 응답했지만, 그 응답이 시스템이 예상하는 JSON 형식이나 특정 구분자로 파싱될 수 없을 때
    사용자가 주제에 맞지 않는 답변을 했을 때에도 사용"""
    def __init__(self, detail: str = "LLM 응답 파싱에 실패했습니다.", status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR):
        super().__init__(detail=detail, status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)

class LLMResponseError(LLMServiceError):
    """LLM이 유효하지 않거나 예상치 못한 응답을 반환했을 때 발생하는 오류, 자주 발생하지 않음."""
    def __init__(self, detail: str = "LLM이 유효하지 않거나, 빈 응답을 반환했습니다.", status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR):
        super().__init__(detail=detail, status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)

class PromptTemplateError(CustomBaseException):
    """LLM 프롬프트 템플릿 관련 오류
    개발자가 LLM에게 여행 관련 질문을 전달하기 위한 프롬프트 템플릿에, context나 domains 같은 필수 변수를 빼먹고 정의했을 때"""
    def __init__(self, detail: str = "프롬프트 템플릿 형식이 유효하지 않거나 필수 변수가 누락되었습니다."):
        super().__init__(detail=detail, status_code=status.HTTP_400_BAD_REQUEST)

class UnsupportedDomainClassificationError(CustomBaseException):
    """LLM이 분류한 도메인이 현재 비즈니스 로직에서 처리할 수 없는 도메인일 때 발생
     LLM이 사용자의 여행 질문을 음식, 숙소 등으로 분류했지만,
     시스템이 아직 음식이나 숙소 도메인에 대한 처리 로직을 가지고 있지 않을 때"""
    def __init__(self, classified_domain: str, detail: str = "LLM이 처리할 수 없는 도메인을 분류했습니다."):
        super().__init__(detail=f"{detail}: '{classified_domain}'", status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)

class AmbiguousClassificationError(CustomBaseException):
    """LLM의 분류가 모호하거나 확신하기 어려울 때 발생
    사용자의 여행 질문(예: 좋은 곳 알려줘)이 너무 일반적이어서
    LLM이 명확하게 관광지, 맛집 중 하나로 분류하지 못 하고 모호하다고 판단했을 때"""
    def __init__(self, detail: str = "LLM의 도메인 분류가 모호하여 확신하기 어렵습니다."):
        super().__init__(detail=detail, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)

class NoRelevantDocumentsFoundError(CustomBaseException):
    """벡터 DB에서 관련 문서를 찾을 수 없을 때 발생
    부산 맛집에 대한 사용자의 질문으로 벡터 DB를 검색했지만, 관련된 맛집 임베딩 정보가 하나도 없을 때"""
    def __init__(self, detail: str = "쿼리에 대한 관련 문서를 찾을 수 없습니다."):
        super().__init__(detail=detail, status_code=status.HTTP_404_NOT_FOUND)