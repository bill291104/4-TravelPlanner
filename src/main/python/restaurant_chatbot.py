import os
import openai
from langchain_community.utilities import SQLDatabase
from langchain_community.document_loaders import SQLDatabaseLoader
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_chroma import Chroma
from langchain.chains import RetrievalQA
from langchain.prompts import PromptTemplate

# --- 0. 설정 ---
# True로 설정하면 DB에서 데이터를 불러와 ChromaDB에 다시 저장합니다.
# 이미 데이터 저장을 완료했다면 False로 설정하고 실행하세요.
SETUP_DATABASE = True
CHROMA_DB_PATH = "./chroma_db_langchain" # 이 코드를 실행하면 restaurant_chatbot.py 파일과 같은 위치에 chroma_db_langchain 폴더가 생성됩니다.

# --- 1. 초기 설정: API 키, LLM 및 임베딩 모델 ---
print("🚀 LangChain 기반 RAG 챗봇 시스템을 시작합니다...")

try:
    # LangChain은 OpenAI API 키를 자동으로 환경 변수에서 읽습니다.
    if "OPENAI_API_KEY" not in os.environ:
        raise TypeError("OPENAI_API_KEY 환경 변수가 설정되지 않았습니다.")
    print("✅ OpenAI API 키 로드 성공")

    # LangChain이 사용할 모델들을 정의합니다.
    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
    embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
    print("✅ LLM 및 임베딩 모델 준비 완료")

except Exception as e:
    print(f"❌ 초기 설정 중 오류 발생: {e}")
    exit()

# --- 2. 데이터베이스 준비 및 인덱싱 (SETUP_DATABASE가 True일 때만 실행) ---
# 이 부분은 최초 1회만 실행하는 '느린' 과정입니다.
if SETUP_DATABASE:
    print("\n🔄 [1회성 작업] 데이터베이스를 로드하여 ChromaDB에 저장합니다...")
    try:
        db_uri = f"mysql+mysqlconnector://root:constant123@158.180.77.204/aitoy4"

        db = SQLDatabase.from_uri(db_uri, include_tables=['restaurant'])
        loader = SQLDatabaseLoader(db)

    # 데이터를 Document 객체로 불러옵니다.
        documents = loader.load()
        print(f"✅ 데이터베이스에서 {len(documents)}개의 문서를 성공적으로 로드했습니다.")

        # 불러온 문서를 벡터로 변환하여 ChromaDB에 저장합니다.
        Chroma.from_documents(
            documents=documents,
            embedding=embeddings,
            persist_directory=CHROMA_DB_PATH
        )
        print(f"✅ 문서를 성공적으로 임베딩하고 ChromaDB에 저장했습니다.")

    except Exception as e:
        print(f"❌ 데이터 준비 중 오류 발생: {e}")
        exit()

# --- 3. RAG 체인(Chain) 생성 (실시간 실행) ---
# 이 부분은 사용자가 질문할 때마다 실행되는 '빠른' 과정입니다.
print("\n🔄 RAG 검색 및 답변 체인을 생성합니다...")
try:
    # 디스크에 저장된 ChromaDB를 불러옵니다.
    vectorstore = Chroma(persist_directory=CHROMA_DB_PATH, embedding_function=embeddings)

    # 프롬프트 템플릿 정의: LLM에게 어떻게 답변할지 지시
    prompt_template = """
    당신은 부산 맛집을 추천해주는 친절한 여행 가이드 챗봇입니다.
    아래에 제공된 [검색된 맛집 정보]를 바탕으로 사용자의 질문에 답변해주세요.
    정보에 없는 내용은 답변하지 마세요.

    [검색된 맛집 정보]
    {context}

    [사용자 질문]
    {question}

    [답변]
    """
    PROMPT = PromptTemplate(
        template=prompt_template, input_variables=["context", "question"]
    )

    # LangChain의 RetrievalQA 체인을 생성합니다.
    # 이 한 줄이 검색(Retrieval)과 생성(Generation)을 모두 처리합니다.
    qa_chain = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff", # 가장 간단한 방식, 검색된 내용을 모두 context에 넣음
        retriever=vectorstore.as_retriever(),
        chain_type_kwargs={"prompt": PROMPT},
        return_source_documents=True # 답변의 근거가 된 문서를 함께 반환
    )
    print("✅ RAG 체인 생성 완료!")

except Exception as e:
    print(f"❌ RAG 체인 생성 중 오류 발생: {e}")
    exit()

# --- 4. 챗봇 실행 ---
question = ("해운대 근처 맛집 알려줘")
print(f"\n[사용자 질문] {question}")

# 생성된 체인을 실행하여 답변을 얻습니다.
result = qa_chain.invoke({"query": question})

print("\n--- 챗봇 최종 답변 ---")
print(result["result"])

print("\n--- 답변 근거 문서 ---")
for doc in result["source_documents"]:
    print(f"- {doc.page_content[:100]}...")

