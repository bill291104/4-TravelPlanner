# app/llm/client.py
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from ..core.config import settings

# 1. 기본 LLM 클라이언트는 한 번만 생성하여 공유합니다.
#    API 키와 모델 이름은 설정 파일에서 가져옵니다.
#    기본 temperature는 0.1로 설정합니다.
_llm = ChatOpenAI(
    model=settings.OPENAI_MODEL_NAME,
    api_key=settings.OPENAI_API_KEY,
    temperature=0.1
)

# def get_llm(temperature: float = 0.1):
#     """
#     지정된 temperature로 LLM을 사용할 수 있도록 '바인딩'된 Runnable 객체를 반환합니다.
#
#     Args:
#         temperature (float): 조절하고 싶은 temperature 값.
#
#     Returns:
#         Runnable: 새로운 temperature가 적용된 LLM 실행 객체.
#     """
#     # 2. .bind()를 사용해 기존 _llm 객체의 설정을 바꾸지 않고,
#     #    실행 시에만 적용될 파라미터를 동적으로 설정합니다.
#     return _llm.bind(temperature=temperature)

def get_llm(temperature: float = 0.1):
    """
    지정된 temperature로 LLM을 사용할 수 있도록 '바인딩'된 객체를 반환합니다.
    """
    # .bind()를 사용해 기존 _llm 객체의 설정을 바꾸지 않고,
    # 실행 시에만 적용될 파라미터를 동적으로 설정합니다.
    return _llm.bind(temperature=temperature)

# --- 임베딩 모델 클라이언트 (새로 추가된 부분) ---
# 임베딩 모델 클라이언트도 모듈 로딩 시 한 번만 생성합니다.
_embedding_model = OpenAIEmbeddings(
    model="text-embedding-3-small", # 임베딩에 권장되는 모델
    api_key=settings.OPENAI_API_KEY
)

def get_embedding_model():
    """
    ✨ [추가된 함수]
    미리 생성된 임베딩 모델 클라이언트 객체를 반환합니다.
    """
    return _embedding_model