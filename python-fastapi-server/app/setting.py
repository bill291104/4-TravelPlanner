import os
from enum import Enum
from dotenv import load_dotenv

from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings

load_dotenv()
openai_api_key = os.getenv("OPENAI_API_KEY")

embedding_function = OpenAIEmbeddings(
    model="text-embedding-3-large"
)

persist_directory = './vector_db'
if not os.path.exists(persist_directory):
    os.mkdir(persist_directory)

# 우선 순위 기준 도메인 열거형
class Domains(Enum):
    PLACE = "place"
    RESTAURANT = "restaurant"
    ACCOM = "accom"

# 벡터 DB 컬렉션 인스턴스 생성
db_collections = {}
for domain in Domains:
    db_collections[domain.value] = Chroma(
        collection_name=domain.value,
        embedding_function=embedding_function,
        persist_directory=persist_directory
    )