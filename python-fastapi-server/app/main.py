from fastapi import FastAPI, HTTPException, responses
from pydantic import BaseModel
from typing import List, Union, Literal

from langchain_core.documents import Document

from .setting import db_collections, Domains

app = FastAPI()

# intellij 에서 서버 구동 방법
# 1. 가상 환경 구축
# intellij 에서 터미널을 열면 {리포지토리를 클론한 프로젝트가 있는 경로}\KDT_BE12_Toy_Project4 라고 나옴. 여기가 프로젝트 루트 경로
# 이 상태에서 `python -m venv .venv` 명령어 입력 <- 프로젝트 루트 경로에 .venv 폴더가 생김
# `.venv\Scripts\activate` 명령어 입력 <- 터미널의 프로젝트 루트경로 왼쪽에 (.venv) 가 생김
# `pip install -r .\python-fastapi-server\requirements.txt` 명령어 입력 <- 터미널에서 라이브러리가 오류(빨간 글씨) 없이 다운로드 되어야 함
# 2. api 키 세팅
# 프로젝트 루트 경로 하위(.venv 폴더와 같은 위치)에 `.env` 이름으로 파일을 만들고 내용에 OPENAI_API_KEY={본인 api-key} 를 입력하고 저장
# 3. 서버 구동
# `uvicorn python-fastapi-server.app.main:app --reload` 명령어 입력 <- 8000번 포트로 서버가 구동 되어야 함
# localhost:8000/ 으로 접속 하면 /docs 로 redirect 되어서 이 서버의 api 문서가 보여야 정상


@app.get("/")
async def root():
    return responses.RedirectResponse(url="/docs")

class EmbeddingRequest(BaseModel):
    content: str
    metadata: dict
    domain: Domains
    related_contents: Union[List[str], None] = None

@app.post("/embedding", status_code=204)
async def embedding_travel_data(request: EmbeddingRequest):
    print(request)
    try:
        document = Document(
            page_content=request.content,
            metadata=request.metadata
        )
        db_collections[request.domain.value].add_documents([document])
    except Exception as e:
        print(f"Embedding failed: {e}")
        raise HTTPException(status_code=500, detail="Embedding failed")
