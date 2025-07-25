import json
from typing import List, TypedDict

from langchain_core.prompts import ChatPromptTemplate

from ..core.constants import Domains
from ..llm.client import get_llm

# 에이전트의 데이터 흐름을 정의하는 상태 객체
class DataState(TypedDict):
    initial_keywords: List[str]
    main_domain: Domains
    main_keywords: List[str]
    sub_keywords: List[str]
    # ... 기타 상태 값들

llm = get_llm(temperature=0)

async def classify_keyword(state: DataState) -> dict:
    """
    LLM이 전체 키워드를 main_domain, main_keywords, sub_keywords로 분류합니다.
    """
    print("--- 키워드 분류 ---")
    keywords = state.get("initial_keywords", [])

    # 여기에서 프롬프트를 명확히 해야 1차에서 main, sub 키워드를 분류할 수 있음
    prompt = ChatPromptTemplate.from_template(
        """당신은 사용자의 키워드를 '검색용'과 '필터링용'으로 완벽하게 분리하는 최고의 분류 전문가입니다.
        주어진 키워드 리스트를 'main_domain', 'main_keywords', 'sub_keywords' 세 가지로 분류해주세요.

        [분류 규칙]
        1.  **main_domain**: 여행의 가장 큰 카테고리입니다. 반드시 다음 중 하나여야 합니다: [{main_domain_options}]
        2.  **main_keywords**: 1차 검색에 사용할 **핵심 명사**입니다. 주로 **[지역 이름], [장소 종류(숙소, 맛집, 관광지)], [구체적인 대상(국밥, 스테이크)]** 등이 해당됩니다.
        3.  **sub_keywords**: 1차 검색 결과를 좁힐 **부가적인 조건**입니다. 주로 **[분위기(조용한)], [특징(가성비, 커플)], [속성(바다 보이는)]** 같은 단어들이 해당됩니다.

        [분류 예시]
        - 예시 1 (맛집 필터링):
          - 입력: ["부산역", "가성비", "국밥집"]
          - 출력: {{"main_domain": "restaurant", "main_keywords": ["부산역", "국밥집"], "sub_keywords": ["가성비"]}}
        - 예시 2 (숙소 필터링):
          - 입력: ["제주도", "가족여행", "오션뷰", "리조트"]
          - 출력: {{"main_domain": "accom", "main_keywords": ["제주도", "리조트"], "sub_keywords": ["가족여행", "오션뷰"]}}
        - 예시 3 (관광지 필터링):
          - 입력: ["서울", "사진찍기 좋은", "역사", "궁궐"]
          - 출력: {{"main_domain": "place", "main_keywords": ["서울", "궁궐"], "sub_keywords": ["사진찍기 좋은", "역사"]}}
        - 예시 4 (필터링 조건 없음):
          - 입력: ["강원도", "등산"]
          - 출력: {{"main_domain": "place", "main_keywords": ["강원도", "등산"], "sub_keywords": []}}
        - 예시 5 (맛집 데이트):
          - 입력: ["강남역", "데이트", "스테이크", "맛집"]
          - 출력: {{"main_domain": "restaurant", "main_keywords": ["강남역", "스테이크", "맛집"], "sub_keywords": ["데이트"]}}

        결과는 반드시 다음 JSON 형식으로만 반환해주세요: {{"main_domain": "...", "main_keywords": ["...", ...], "sub_keywords": ["...", ...]}}
        
        [사용자 키워드]: {keywords}"""
    )

    main_domain_options = ", ".join(d.value for d in Domains)

    # LCEL 체인 구성
    chain = prompt | llm

    # 체인 실행 및 결과 파싱
    response = await chain.ainvoke({"keywords": keywords, "main_domain_options": main_domain_options})
    result_json = json.loads(response.content)

    # print(f"키워드 분류 결과: {result_json}")

    # # 다음 상태로 전달할 결과 반환
    # return {
    #     "main_domain": Domains(result_json.get("main_domain")),
    #     "main_keywords": result_json.get("main_keywords", []),
    #     "sub_keywords": result_json.get("sub_keywords", [])
    # }

    return result_json
