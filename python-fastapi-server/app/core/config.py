# app/core/config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    OPENAI_API_KEY: str
    OPENAI_MODEL_NAME: str = "gpt-4o-mini"

    class Config:
        env_file = ".env" # 프로젝트 루트의 .env 파일을 읽도록 설정

settings = Settings()