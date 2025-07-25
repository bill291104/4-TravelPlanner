from typing import List,Dict,Any
from ..schemas.vector_ss import SimilaritySearchRequest,Domains
from ..llm.client import get_llm
from langchain_core.prompts import ChatPromptTemplate
from langgraph.prebuilt import create_react_agent
from ..db.session import db_collections
from typing import TypedDict, List, Literal
from langchain.schema import Document
from collections import Counter

from langchain_core.runnables import RunnablePassthrough, RunnableLambda


async def main_domain_search(request: SimilaritySearchRequest) -> List[Document]:
    """
        "basic":
        다음 여행 요청에서 핵심이 되는 메인 도메인{Main_Domains}을 추출합니다.그리고 해당 PK를 검색합니다.

        사용자 요청: {context}
        가능한 도메인: {domains}

        --- Main_Domains ---
        PLACE = "place"
        RESTAURANT = "restaurant"
        ACCOM = "accom"

        "detailed":
        여행 계획 수립을 위해 다음 요청을 분석하여 핵심 도메인을 찾아주세요.

        요청 내용: {context}
        선택 가능한 도메인: {domains}

        규칙:
        1. 구체적인 장소나 시설 카테고리만 선택
        2. 주관적 평가(좋은, 맛있는)는 제외(=Sub_Domains는 제외)
        3. 최대 3개까지만 선택

        결과:

        "focused":
        사용자가 찾고자 하는 여행 관련 시설의 종류를 파악하세요.

        요청: {context}
        도메인 목록: {domains}

        가장 중요한 시설 유형만 추출하세요:

        """


    main_collection = db_collections[request.target_domain.value]

    # main_domain_pks: List[str] = []

    try:

        query =' '.join(request.target_keywords)

        #메인 컬렉션의 document
        main_domain_docs = main_collection.similarity_search(query,k=20)
        print(f"검색된 문서 수: {len(main_domain_docs)}")

        if not main_domain_docs:
            print(f" 문서에 해당ID가 없습니다: {main_domain_docs}")
            return []

        return main_domain_docs
    except Exception as e:
        print(f"핵심 도메인 유사도 검색 중 오류 발생: {e}")
        raise Exception(f"벡터 검색 실패: {e}")



        #main_result: Dict -> 유사도 최상위 1개의 도메인
async def subdomain_filter(main_domain_docs: List[Document], request: SimilaritySearchRequest) ->List[str] :

    """
     서브 도메인을 통해 세부 조건을 필터링합니다.

     --- Sub Domains ---
     PLACE_REVIEW = "place_review"           # 관광지 리뷰/평가
     RESTAURANT_REVIEW = "restaurant_review"  # 음식점 리뷰/평가
     ACCOM_REVIEW = "accom_review"           # 숙박 리뷰/평가
     TRAVEL_STYLE = "travel_style"           # 여행 스타일
     TRAVEL_TREND = "travel_trend"           # 여행 트렌드


    다음 여행 요청에서 주관적 속성이나 품질 조건을 추출하세요.

    원본 요청: {context}
    메인 도메인: {main_domain}
    키워드: {keywords}

     # 메인 도메인에 따른 관련 서브 도메인들 정의
    subdomain_mapping = {
        Domains.RESTAURANT: ["restaurant_review", "travel_style"],
        Domains.PLACE: ["place_review", "travel_trend"],
        Domains.ACCOM: ["accom_review", "travel_style"]
    }

    찾아야 할 서브 속성:
    - 품질 관련: 맛있는, 좋은, 깨끗한, 고급스러운
    - 분위기 관련: 조용한, 아늑한, 로맨틱한, 활기찬
    - 가격 관련: 저렴한, 가성비, 합리적인, 고급
    - 서비스 관련: 친절한, 빠른, 전문적인
    - 위치 관련: 접근성 좋은, 뷰 좋은, 중심가

    해당하는 서브 도메인 선택:
    - place_review: 관광지 품질/평가 관련
    - restaurant_review: 음식점 품질/평가 관련
    - accom_review: 숙박시설 품질/평가 관련
    - travel_style: 개인 여행 스타일 관련
    - travel_trend: 최신 여행 트렌드 관련

    가장 적합한 서브 도메인 1-2개를 쉼표로 구분하여 답변:
    """

    if not main_domain_docs:
        return []

    #1. 입력 데이터 준비, 검색 쿼리 생성
    # 필터링의 대상이 될 원본 PK목록을 Set으로 준비하여 빠른 조회를 가능케 함
    main_pks_set={doc.metadata.get('id') for doc in main_domain_docs if doc.metadata.get('id')}

    # 임시로 하드코딩으로메인 도메인에 따른 관련 서브 도메인들 정의
    #get_subs 적용은 기능 구현 확인 후 진행 예정
    subdomain_mapping={
        Domains.RESTAURANT: [Domains.RESTAURANT_REVIEW, Domains.TRAVEL_STYLE],
        Domains.PLACE: [Domains.PLACE_REVIEW, Domains.TRAVEL_TREND],
        Domains.ACCOM: [Domains.ACCOM_REVIEW, Domains.TRAVEL_STYLE]
    }

    # (코드 파악 미흡) 현재 요청의 메인 도메인에 해당하는 서브 도메인 컬렉션들을 가져옴
    relevant_sub_domains = subdomain_mapping.get(request.target_domain, [])
    if not relevant_sub_domains:
        print("매핑된 서브 도메인이 없어 1차 검색 결과를 반환합니다.")
        return list(main_pks_set)

    sub_collections = {name.value: db_collections[name.value] for name in relevant_sub_domains}

    #서브 도메인 검색을 위한 쿼리를 생성
    query = f"{request.context} {' '.join(request.target_keywords)}"

    #2. 모든 관련 서브 도메인에서 문서 검색
    all_sub_docs=[]
    try:
        for name,collection in sub_collections.items():
            print(f"->'{name}'컬렉션 검색 중")
            #각 서브 컬렉션에서 유사도 높은 문서 10개 검색
            sub_docs = collection.similarity_search(query,k=10)
            all_sub_docs.extend(sub_docs)

        if not all_sub_docs:
            print("연관된 서브 도메인 정보를 찾지 못했습니다. 원본 PK 목록을 그대로 반환합니다.")
            return list(main_pks_set)


#### -----코드 파악 미흡 구간

            # 3단계 서브 도메인 문서와 메인 도메인 pk연결
            # 각 서브 도메인 문서의 메다 데이터에 'main_pk'가 저장되어 있다고 가정
            # 예: restaurant_review 문서 -> metadata: {'id': 'review_123', 'main_pk': 'restaurant_abc', ...}
        linked_main_pks=[
                doc.metadata.get('main_pk')
                for doc in all_sub_docs
                if  doc.metadata.get('main_pk')
        ]
        if not linked_main_pks:
            print("서브 도메인 문서에 연결된 main_pk가 없습니다. DB 스키마를 확인하세요.")
            return list(main_pks_set)

        # --- 4단계: 결과 집계, 순위화 및 반환 ---
        # 언급된 main_pk의 빈도수를 계산 (많이 언급될수록 순위가 높음)
        pk_frequency = Counter(linked_main_pks)
        # 1차 검색 결과(main_pks_set)에 포함된 PK들만 필터링
        filtered_pks = {pk: count for pk, count in pk_frequency.items() if pk in main_pks_set}
        # 빈도수(언급 횟수)가 높은 순으로 정렬
        sorted_pks = sorted(filtered_pks.keys(), key=lambda pk: filtered_pks[pk], reverse=True)

        print(f"Tool2 최종 결과 (상위 {len(sorted_pks)}개): {sorted_pks}")
        return sorted_pks

    except Exception as e:
        print(f"서브 도메인 필터링 중 오류 발생: {e}")
        # 오류 발생 시, 최소한 1차 검색 결과라도 반환하도록 처리
        return list(main_pks_set)


async def create_similarity_search_chain():
    """
    main_domain_search와 subdomain_filter를 순차적으로 실행하는 LCEL 체인을 생성합니다.
    """
    chain = (
            RunnablePassthrough.assign(
                main_docs=RunnableLambda(main_domain_search)
            )
            | RunnablePassthrough.assign(
        final_pks=lambda x: subdomain_filter(x['main_docs'], x['request'])
    )
            | (lambda x: x['final_pks'])
    )
    return chain


# (코드 파악 미흡 구간)------------------------------------------

async def similarity_search(request: SimilaritySearchRequest) -> List[str]:
    """
    사용자 요청(request)을 인자로 받아, 메인 검색과 서브 필터링을
    순차적으로 모두 실행하고 최종 결과를 반환하는 **통합 실행 함수**.

    FastAPI 라우터가 이 함수를 직접 호출하게 됩니다.
    """
    print(" 검색 파이프라인을 시작합니다...")

    # 1. main_domain_search 함수를 호출하여 1차 검색을 수행합니다.
    print("--- 1단계: 메인 도메인 검색 ---")
    main_docs = await main_domain_search(request)

    # 1차 검색 결과가 없으면 더 진행하지 않고 빈 리스트를 반환합니다.
    if not main_docs:
        print(" 최종 추천 PK 리스트: [] (1차 검색 결과 없음)")
        return []

    # 2. 1차 검색 결과를 다음 함수의 입력으로 사용하여 2차 필터링을 수행합니다.
    print("--- 2단계: 서브 도메인 필터링 및 재정렬 ---")
    final_pk_list = await subdomain_filter(main_docs, request)

    # 3. 최종 결과를 반환합니다.
    print(f"\n최종 추천 PK 리스트: {final_pk_list}")
    return final_pk_list
