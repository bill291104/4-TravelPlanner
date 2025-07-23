from typing import List, Tuple
from langchain.agents import initialize_agent, AgentType
from langchain_chroma import Chroma
from langchain_core.tools import Tool
import ast

from sympy.polys.domains import domain

from ..schemas.vector_ss import SimilaritySearchRequest
from ..llm.client import get_llm

MAIN_KEYWORDS = {"바다","산책","식사","수영","문화","관광지","전통","카페"}
SUB_KEYWORDS = {"조용한","분위기","신난다","시원한","맛있는","평점","역사적인"}

# 👉 main_collection, sub_collection 전역 변수로 등록
main_collection: Chroma = None
sub_collection: Chroma = None
target_domin : str = None

#메인 키워드로 메인 도메인 찾기
def main_domain_similarity_search(main_keywords: List[str], target_domain : str) -> List[int]:
    global main_collection
    query = " ".join(main_keywords)
    docs = main_collection.similarity_search(
        query,
        k=5,
        filter={"domain": target_domain}
    )
    main_pks = [doc.metadata["place_id"] for doc in docs]
    return main_pks

# 위 tool1 의 진짜 함수를 래핑하는 함수 tool은 인자가 1개만 와야하는데 2개가 필요하므로 다시 한 번 감싼것.
def main_domain_wrapper(main_keywords: List[str]) -> List[int]:
    global domain
    return main_domain_similarity_search(main_keywords, domain)

tool1 = Tool(
    name="main_domain_search",
    func=main_domain_wrapper,     # tool로 사용할 함수
    description="사용자가 요청한 장소, 숙소, 식당 등 '대상'의 후보 목록을 찾는다. 제주도 호텔, 강남역 맛집 처럼 명확한 대상을 찾을 필요가 있을 때 사용한다."
)

#서브 키워드로 서브 도메인 찾기
def subdomain_filter(sub_keywords: List[str]) -> List[int]:
    global sub_collection
    if not sub_keywords:
        return []
    query = " ".join(sub_keywords)
    docs = sub_collection.similarity_search(query, k=5)
    sub_pks = [doc.metadata["place_id"] for doc in docs]
    return sub_pks

tool2 = Tool(
    name="subdomain_search",
    func=subdomain_filter,      # tool로 사용할 함수
    description="리뷰 기반으로 사용자의 주관적인 조건(분위기, 품질, 상태 등)에 맞는 장소를 찾는다. 조용한, 분위기 좋은, 가성비가 뛰어난, 깨끗한 등 실제 경험을 통해 알 수 있는 조건을 확인할 때 사용한다."
)

async def similarity_search(request: SimilaritySearchRequest) -> List[int]:
    # 🌟 전역 도메인 설정
    global domain
    domain = request.target_domain.value  # enum이면 .value 붙여줘야 string

    all_keyword = request.target_keywords

    def is_known_main_keyword(word: str) -> bool:
        return word in MAIN_KEYWORDS
    def is_known_sub_keyword(word: str) -> bool:
        return word in SUB_KEYWORDS

    # 이후 여기서 main/sub 키워드로 분류 가능
    main_keywords = [kw for kw in all_keyword if is_known_main_keyword(kw)]
    sub_keywords = [kw for kw in all_keyword if is_known_sub_keyword(kw)]
    target_domain = request.target_domain.value  # enum이면 .value 붙여줘야 string

    agent = initialize_agent(
        tools=[tool1, tool2],              # 사용할 도구(tool) 목록
        llm=get_llm(),                            # 사용할 언어 모델
        agent=AgentType.CHAT_ZERO_SHOT_REACT_DESCRIPTION,  # 에이전트 타입
        verbose=True                        # 실행 로그 보기
    )

    # 🌟 프롬프트 구성: tool1 → tool2 순서로 수행하도록 명시
    query_prompt = f"""
    아래의 키워드를 기반으로 장소 추천 후보를 찾고 필터링해주세요.

    Step 1. 먼저 main_keywords = {main_keywords} 를 사용하여 tool 'main_domain_search'를 호출하세요. 이 Tool은 장소의 후보 목록을 반환합니다.

    Step 2. 다음으로 sub_keywords = {sub_keywords} 를 사용하여 tool 'subdomain_search'를 호출하세요. 이 Tool은 사용자 조건에 부합하는 장소 목록을 반환합니다.

    Step 3. 두 결과의 교집합만 최종 결과로 출력하세요. 예시 출력 형식: [3, 8, 12]
    """

    try:
        result = agent.run(query_prompt)

        #문자열인 파이썬 객체를 실제로 해석해서 바꿔줌
        parsed = ast.literal_eval(result)
        if isinstance(parsed, list) and all(isinstance(x, int) for x in parsed):
            return parsed
        else:
            raise ValueError("리턴된 값이 List[int] 형식이 아님")
    except Exception as e:
        print("❌ agent.run() 결과 파싱 실패:", e)
        return []
