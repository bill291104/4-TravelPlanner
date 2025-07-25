import enum
from builtins import int, bool, isinstance, list, all, ValueError, Exception, print
import json
from langchain_core.tools.structured import StructuredTool
from pydantic import BaseModel
from typing import List
from langchain.agents import initialize_agent, AgentType
from langchain_chroma import Chroma
from ..core.constants import Domains
import ast
from ..schemas.vector_ss import SimilaritySearchRequest
from ..llm.client import get_llm
from ..db.session import db_collections
from langchain_core.prompts import ChatPromptTemplate

MAIN_KEYWORDS = {"바다","산책","식사","수영","문화","관광지","전통","카페","물놀이","해수욕장"}
SUB_KEYWORDS = {"조용한","분위기","신난다","시원한","맛있는","평점","역사적인"}

# 👉 main_collection, sub_collection 전역 변수로 등록
main_collections: Chroma = None

#메인 키워드로 메인 도메인 찾기
def main_keyword_similarity_search(main_keywords: List[str], target_domain: str) -> str:
    main_collection = db_collections[target_domain]
    query = " ".join(main_keywords)
    docs = main_collection.similarity_search(
        query,
        k=5,
        filter=None
    )
    main_doc_pks = [int(doc.id) for doc in docs]
    print (f"Document의 id들 : {main_doc_pks}")
    return json.dumps(main_doc_pks)

#인자 2개를 받기 위한 래퍼클래스의 인자가 될 클래스
class MainSearchInput(BaseModel):
    main_keywords: List[str]
    target_domain: str
    # main_doc_pks: List[str]

    # sub_collection_names: List[str]
    # domain_enum = Domains(enum)
    # sub_collection_names = domain_enum.get_subs


# # 위 tool1 의 진짜 함수를 래핑하는 함수 tool은 인자가 1개만 와야하는데 2개가 필요하므로 다시 한 번 감싼것.
def main_domain_wrapper(request: MainSearchInput) -> str:
    print("🔍 툴1 main_keyword_similarity_search 실행됨")
    main_result = main_keyword_similarity_search(request.main_keywords, request.target_domain)
    print(f"툴 1 결과 : {main_result}")

    return main_result

tool1 = StructuredTool.from_function(
    name="main_domain_wrapper",
    func=main_domain_wrapper,     # tool로 사용할 함수
    description="사용자가 요청한 장소, 숙소, 식당 등 장소가 되는 곳을 찾는다. 호텔, 강남역, 고속터미널 처럼 명확한 대상을 찾을 필요가 있을 때 사용한다.",
    return_direct=True  #✅ Agent가 Tool 실행 후 바로 종료
)

# ////////// 서브로 거르기

def sub_domain_similarity_search(sub_keywords: List[str], target_domain:str,main_doc_pks: str) -> str:
    if not sub_keywords:
        return json.dumps([])

    #targaet_domaain으로 subCollection 찾기
    try:
        domain_enum = Domains(target_domain)  # 예: "place" → Domains.PLACE
    except ValueError:
        raise ValueError(f"잘못된 도메인: {target_domain}")
        return json.dumps([])

    sub_collection_names = domain_enum.get_subs  # 예: ["place_review", "travel_style", "travel_trend"]
    # ✅ Tool1의 결과 main_doc_pks(JSON 문자열)를 리스트로 변환
    try:
        main_pks = json.loads(main_doc_pks)
        print(f"🔍 파싱된 main_pks : {main_pks}")
    except json.JSONDecodeError as e:
        print(f"❌ JSON 파싱 실패: {e}")
        return json.dumps([])

    #서브컬렉션 이름을 찾았으니 지정해서 유사도 검사하기
    query = " ".join(sub_keywords)
    print(f"🔍 검색 쿼리: {query}")

    final_pks = set()

    for name in sub_collection_names:
        print(f"🔍 {name} 컬렉션에서 검색 중...")
        if name not in db_collections:
            print(f"❌ {name} 컬렉션을 찾을 수 없음")
            continue

        sub_collection = db_collections[name]  # ✅ 반복문 안에서 서브컬렉션 객체 꺼냄
        docs = sub_collection.similarity_search(
            query=query,
            k=10,
            filter={"fk": {"$in": main_pks}}  # tool1 결과 기반 필터링
        )
        print(f"🔍 {name}에서 찾은 문서 수: {len(docs)}")

    # 🎯 main_pks(fk) 포함 여부로 필터링


        for doc in docs:
            print(f"닥스 가져왔나???? {docs.metadata}")
            if "fk" in doc.metadata:
                fk_value = int(doc.metadata["fk"])
            if fk_value in main_pks:
                print(f"메타데이터 있어여??? {doc.metadata.fk}")
                final_pks.add(fk_value)
                print( f"필터링결과 >> {fk_value} 짜잔 ")

    return json.dumps(list(final_pks))


#인자 2개를 받기 위한 래퍼클래스의 인자가 될 클래스
class SubSearchInput(BaseModel):
    sub_keywords: List[str]
    main_result: str    # tool1 결과 문자열
    target_domain: str
    # sub_collection_names: List[str]
    # main_doc_pks: List[str]

def sub_domain_wrapper(request: SubSearchInput) -> str:
    print("🔍 툴2 sub_domain_wrapper 실행됨")
    try:
        parsed_main_result = json.loads(request.main_result)
        print("🔍 넘어왔어????111")
        if not isinstance(parsed_main_result, list):
            raise ValueError("main_result가 리스트 형태가 아닙니다.")
    except Exception as e:
        raise ValueError(f"main_result 파싱 실패: {e}")
        return json.dumps([])

    sub_result = sub_domain_similarity_search(
        request.sub_keywords,
        request.target_domain,
        request.main_result,
    )
    print(f"툴 2 결과 : {sub_result}")
    return sub_result

tool2 = StructuredTool.from_function(
    name="sub_domain_wrapper",
    func=sub_domain_wrapper,      # too2로 사용할 함수
    description="리뷰 기반으로 사용자의 주관적인 조건(분위기, 품질, 상태 등)에 맞는 장소를 찾는다. 조용한, 분위기 좋은, 가성비가 뛰어난, 깨끗한 등 실제 경험을 통해 알 수 있는 조건을 확인할 때 사용한다.",
    return_direct=True
)
# //////////

async def similarity_search(request: SimilaritySearchRequest) -> List[int]:

    print("🧪 similarity_search 진입!")
    print("🔍 target_domain 도메인 값:", request.target_domain)
    print("🔍 target_keywords 키워드:", request.target_keywords)

# 🌟 전역 도메인 설정
    global domain
    domain = request.target_domain.value  # enum이면 .value 붙여줘야 string
    target_domain = request.target_domain.value  # enum이면 .value 붙여줘야 string
    all_keyword = request.target_keywords

    def is_known_main_keyword(word: str) -> bool:
        return word in MAIN_KEYWORDS
    def is_known_sub_keyword(word: str) -> bool:
        return word in SUB_KEYWORDS

    # 이후 여기서 main/sub 키워드로 분류 가능
    main_keywords = [kw for kw in all_keyword if is_known_main_keyword(kw)]
    sub_keywords = [kw for kw in all_keyword if is_known_sub_keyword(kw)]


    # 🧠 1. agent_main (tool1만 포함)
    agent_main = initialize_agent(
        tools=[tool1],
        llm=get_llm(),
        agent=AgentType.CHAT_ZERO_SHOT_REACT_DESCRIPTION,
        verbose=True,
        handle_parsing_errors=True,
        max_iterations=4  # 🔧 반복 제한 완화
    )
    print("🧪 main_prompt 생성 시작")

    # 🌟 프롬프트 구성: tool1 → tool2 순서로 수행하도록 명시
    main_prompt = f"""
    당신은 장소 후보를 찾기 위해 도구를 사용하는 AI입니다.

    다음 메인 키워드를 기반으로 도구를 사용하여 장소 후보 ID 목록을 찾으세요:
    ```json
    {{
      "action": "main_domain_wrapper",
      "action_input": {{
        "request": {{
          "main_keywords": {main_keywords},
          "target_domain": "{target_domain}"
        }}
      }}
    }}
    
    - `main_domain_wrapper`를 통해 찾은 장소 후보들: 사용자의 주요 관심사(예: "{main_keywords}")에 기반합니다.
    - 💡 도구 실행 후에는 반드시 숫자만 포함된 리스트 형태로 응답하세요.
    - ❗설명이나 문장 없이 리스트만 출력하세요.
    - 반드시 JSON 표준 형식을 따르세요 (큰따옴표 사용, 쉼표, 대괄호).
    - 이 결과를 tool2 에게 전달하세요.
    """

    print("🔍 메인 프롬프트 내용:\n", main_prompt)  # 👈 이거 추가
    try:
        print("🔍 main try 진입:\n")  # 👈 이거 추가
        result = agent_main.run(main_prompt)
        print(f"🤖 Agent1 결과: {result}")

        #문자열인 파이썬 객체를 실제로 해석해서 바꿔줌
        parsed = ast.literal_eval(result)
        if isinstance(parsed, list) and all(isinstance(x, int) for x in parsed):
            main_result = parsed
        else:
            raise ValueError("리턴된 값이 List[int] 형식이 아님")
    except Exception as e:
        print("❌ agent_main() 결과 파싱 실패:", e)
        return []

    # ////////////

    # 🧠 2. agent_sub (tool2만 포함)
    agent_sub = initialize_agent(
        tools=[tool2],
        llm=get_llm(),
        agent=AgentType.CHAT_ZERO_SHOT_REACT_DESCRIPTION,
        verbose=True,
        handle_parsing_errors=True,
        max_iterations=4  # 🔧 반복 제한 완화
    )
    print("🧪 sub_prompt 생성 시작")
    sub_prompt = f"""
    당신은 사용자의 주관적인 조건을 기반으로 장소를 필터링하는 AI입니다.

    다음 서브 키워드를 기반으로 도구를 사용하여 필터링된 장소 ID 목록을 찾으세요:
    ```json
    {{
      "action": "sub_domain_wrapper",
      "action_input": {{
        "request": {{
          "sub_keywords": {sub_keywords},
          "main_result": "{result}",
          "target_domain": "{target_domain}"
        }}
      }}
    }}
    
    - tool1이 전달한 값을 가지고 로직을 실행하세요.
    - 💡 도구 실행 후에는 반드시 아래 예시처럼 숫자만 포함된 리스트 형태로 응답하세요.
    - ❗설명이나 문장 없이 리스트만 출력하세요.
    - 반드시 JSON 표준 형식을 따르세요 (큰따옴표 사용, 쉼표, 대괄호).
    """

    print("🔍 두번째 프롬프트 내용:\n", sub_prompt)  # 👈 이거 추가
    try:
        print("🔍 sub try 진입:\n")  # 👈 이거 추가
        result = agent_sub.run(sub_prompt)
        print(f"🤖 Agent2 결과: {result}")

        #문자열인 파이썬 객체를 실제로 해석해서 바꿔줌
        parsed = ast.literal_eval(result)
        if isinstance(parsed, list) and all(isinstance(x, int) for x in parsed):
            sub_result = parsed
        else:
            raise ValueError("리턴된 값이 List[int] 형식이 아님")
    except Exception as e:
        print("❌ agent.run() 결과 파싱 실패:", e)
        return []
