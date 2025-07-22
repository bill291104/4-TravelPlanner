# app/llm/client.py
from langchain_openai import ChatOpenAI
from ..core.config import settings
from ..core.exceptions import LLMServiceError, LLMAPIError

# 1. 기본 LLM 클라이언트는 한 번만 생성하여 공유합니다.
#    API 키와 모델 이름은 설정 파일에서 가져옵니다.
#    기본 temperature는 0.1로 설정합니다.
_llm = None # 초기에는 None으로 설정
try:
    # API 키가 유효한지 확인
    if not settings.OPENAI_API_KEY:
        raise LLMAPIError(detail="OpenAI API 키가 설정되지 않았습니다. .env 파일을 확인해주세요.")

    _llm = ChatOpenAI(
        model=settings.OPENAI_MODEL_NAME,
        api_key=settings.OPENAI_API_KEY,
        temperature=0.1
    )

    # LLM 객체가 성공적으로 생성되었는지 한 번 더 확인 (간혹 초기화 실패 시 None이 될 수 있음)
    if _llm is None:
        raise LLMServiceError(detail="LLM 클라이언트 초기화에 실패했습니다. 알 수 없는 오류입니다.")

except Exception as e:
    # ChatOpenAI 초기화 중 발생할 수 있는 모든 예외를 잡아서 LLMServiceError로 변환
    if "api_key" in str(e).lower(): #오류 메세지에 api_key가 포함되어있다면
        raise LLMAPIError(detail=f"OpenAI API 키 문제로 LLM 초기화에 실패했습니다: {e}") from e
    else:
        raise LLMServiceError(detail=f"LLM 클라이언트 초기화 중 예상치 못한 오류 발생: {e}") from e

def get_llm(temperature: float = 0.1):
    """
    지정된 temperature로 LLM을 사용할 수 있도록 '바인딩'된 Runnable 객체를 반환합니다.

    Args:
        temperature (float): 조절하고 싶은 temperature 값.

    Returns:
        Runnable: 새로운 temperature가 적용된 LLM 실행 객체.
    """
    # 2. .bind()를 사용해 기존 _llm 객체의 설정을 바꾸지 않고,
    #    실행 시에만 적용될 파라미터를 동적으로 설정합니다.
    return _llm.bind(temperature=temperature)