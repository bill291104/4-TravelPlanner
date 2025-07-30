# app/db/session.py
import os
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings
from ..core.config import settings
from ..core.constants import Domains
from ..core.exceptions import DatabaseConnectionError
from openai import APIConnectionError


# 애플리케이션 전역에서 사용할 db_collections 객체 (초기에는 비어있음)
db_collections = {}

def initialize_db():
    """
    애플리케이션 시작 시 DB와 컬렉션을 초기화합니다.
    """
    try:
        print("Initializing ChromaDB collections...")
        collection_name = "initializing..."
        embedding_function = OpenAIEmbeddings(
            model="text-embedding-3-large",
            api_key=settings.OPENAI_API_KEY # 환경변수 자동 인식 기능을 사용하면 이 줄은 필요 없습니다.
        )

        # vector_db 폴더를 python-fastapi-server 폴더 내에 생성
        persist_directory = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), '../vector_db')

        if not os.path.exists(persist_directory):
            os.makedirs(persist_directory)

        for domain in Domains:
            collection_name = domain.value
        # --- ChromaDB 연결 및 컬렌셕 초기화 시도 ---
            db_collections[collection_name] = Chroma(
                collection_name=collection_name,
                embedding_function=embedding_function,
                persist_directory=persist_directory
            )
            print(f"  - Collection '{collection_name}' initialized.")

    except Exception as e:
        raise DatabaseConnectionError(detail = f"ChromaDB 컬렉션 '{collection_name}' 초기화 중 알 수 없는 오류 발생: {e}")

    print("✅ ChromaDB collections initialized successfully.")