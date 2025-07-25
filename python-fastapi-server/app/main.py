# intellij 에서 서버 구동 방법
# 1. 가상 환경 구축
# intellij 에서 터미널을 열면 {리포지토리를 클론한 프로젝트가 있는 경로}\KDT_BE12_Toy_Project4 라고 나옴. 여기가 프로젝트 루트 경로
# 이 상태에서 `python -m venv .venv` 명령어 입력 <- 프로젝트 루트 경로에 .venv 폴더가 생김
# `.venv\Scripts\activate` 명령어 입력 <- 터미널의 프로젝트 루트경로 왼쪽에 (.venv) 가 생김
# `pip install -r .\python-fastapi-server\requirements.txt` 명령어 입력 <- 터미널에서 라이브러리가 오류(빨간 글씨) 없이 다운로드 되어야 함
# 2. api 키 세팅
# 프로젝트 루트 경로 하위(.venv 폴더와 같은 위치)에 `.env` 이름으로 파일을 만들고 내용에 OPENAI_API_KEY={본인 api-key} 를 입력하고 저장
# 3. 서버 구동
# `uvicorn app.main:app --reload --app-dir python-fastapi-server` 명령어 입력 <- 8000번 포트로 서버가 구동 되어야 함
# localhost:8000/ 으로 접속 하면 /docs 로 redirect 되어서 이 서버의 api 문서가 보여야 정상

from fastapi import FastAPI, responses
from contextlib import asynccontextmanager
from datetime import datetime
import logging
from logging.config import dictConfig
from .core.config import LOGGING_CONFIG # ✨ 로깅 설정 가져오기

from .db.session import initialize_db
from .routers import embedding, extract, vector_ss
from .services import agent_service

# health check를 위해 lifespan 외부에서도 접근할 전역 변수
server_startup_time = "N/A"

@asynccontextmanager
async def lifespan(app: FastAPI):
    # ✨ 로깅 설정을 애플리케이션에 적용합니다.
    dictConfig(LOGGING_CONFIG)
    logger = logging.getLogger("app") # 'app' 로거를 가져옵니다.

    global server_startup_time

    # --- 애플리케이션 시작 시 실행될 로직 ---
    logger.info("🚀 Application startup...")

    # 1. 시작 시간 기록 (한 번만 실행)
    startup_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    server_startup_time = startup_time # 전역 변수 업데이트

    # 2. API 문서 제목 업데이트
    app.title = f"Embedding API (서버 시작: {startup_time})"

    # 3. 데이터베이스 초기화
    initialize_db()

    yield # 이 시점에서 애플리케이션이 요청을 받기 시작

    # --- 애플리케이션 종료 시 실행될 로직 ---
    logger.info("👋 Application shutdown...")

# --- App Initialization ---
app = FastAPI(
    title="Embedding API (서버 시작 대기 중...)",
    description="문서 임베딩 및 관리를 위한 API입니다.",
    lifespan=lifespan # ✨ 모든 시작/종료 로직을 담은 lifespan 등록
)

# --- Include Routers ---
app.include_router(embedding.router)
app.include_router(extract.router)
app.include_router(vector_ss.router)
app.include_router(agent_service.router)

# --- Root Redirect ---
@app.get("/", include_in_schema=False)
async def root():
    return responses.RedirectResponse(url="/docs")

# --- Health Check Endpoint ---
@app.get("/health", tags=["Server Status"])
async def health_check():
    """서버의 현재 상태와 시작 시간을 반환합니다."""
    return {
        "status": "ok",
        "server_startup_time": server_startup_time
    }
