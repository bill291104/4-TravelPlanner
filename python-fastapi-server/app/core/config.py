import os
import logging.config
from pydantic_settings import BaseSettings

# --- Pydantic Settings ---
class Settings(BaseSettings):
    OPENAI_API_KEY: str
    OPENAI_MODEL_NAME: str = "gpt-4o-mini"
    TMAP_APP_KEY: str

    class Config:
        env_file = ".env" # 프로젝트 루트의 .env 파일을 읽도록 설정

settings = Settings()


# --- Log Directory Setup ---
# 이 코드는 app/core/config.py에 있을 때, 프로젝트 루트(KDT_BE12_Toy_Project4)에 'logs' 폴더를 생성합니다.
LOG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), 'logs')
if not os.path.exists(LOG_DIR):
    os.makedirs(LOG_DIR)


# --- Logging Configuration ---
LOGGING_CONFIG = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        # ✨ 콘솔과 파일에서 모두 사용할 상세 포맷
        "detailed": {
            "format": "%(asctime)s [%(levelname)s] [%(name)s:%(lineno)d] - %(message)s",
        },
    },
    "handlers": {
        # 콘솔(터미널) 핸들러
        "console": {
            "class": "logging.StreamHandler",
            "level": "DEBUG", # 개발 중에는 DEBUG 레벨까지 모두 확인
            "formatter": "detailed",
        },
        # 파일 핸들러
        "file": {
            "class": "logging.handlers.TimedRotatingFileHandler",
            "level": "INFO", # 파일에는 INFO 레벨 이상만 기록
            "formatter": "detailed", # ✨ 파일에도 상세 포맷을 적용하여 스택 트레이스 기록
            "filename": os.path.join(LOG_DIR, "app.log"), # ✨ 일반적인 로그 파일 이름으로 변경
            "when": "midnight",
            "interval": 1,
            "backupCount": 30,
            "encoding": "utf-8",
        },
    },
    "loggers": {
        # 우리 애플리케이션 코드용 로거 ('app'으로 시작하는 모든 모듈)
        "app": {
            "handlers": ["console", "file"], # 콘솔과 파일 모두에 로그 전송
            "level": "INFO", # INFO 레벨 이상의 로그만 처리
            "propagate": False,
        },
        # Uvicorn 액세스 로그 (요청/응답 기록)
        "uvicorn.access": {
            "handlers": ["console", "file"], # 콘솔과 파일 모두에 기록
            "level": "INFO",
            "propagate": False,
        },
        # Uvicorn 에러 로그
        "uvicorn.error": {
            "handlers": ["console", "file"], # ✨ Uvicorn 에러도 파일에 기록
            "level": "INFO",
            "propagate": False,
        },
    },
    "root": {
        "handlers": ["console", "file"], # ✨ 다른 라이브러리에서 발생하는 로그도 파일에 기록
        "level": "WARNING",
    },
}