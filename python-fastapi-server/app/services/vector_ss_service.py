from typing import List,Dict,Any, TypedDict, Literal
from ..schemas.vector_ss import SimilaritySearchRequest,Domains
from ..llm.client import get_llm
from langchain_core.prompts import ChatPromptTemplate
from langgraph.prebuilt import create_react_agent
from ..db.session import db_collections
from langchain.schema import Document
from collections import defaultdict

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
        
        # 검색된 모든 문서의 metadata 출력
        for i, doc in enumerate(main_domain_docs):
            print(f"문서 {i+1}: ID={doc.metadata.get('additionalProp1')}, content={doc.page_content[:50]}...")

        if not main_domain_docs:
            print(f" 문서에 해당ID가 없습니다: {main_domain_docs}")
            return []

        return main_domain_docs
    except Exception as e:
        print(f"핵심 도메인 유사도 검색 중 오류 발생: {e}")
        raise Exception(f"벡터 검색 실패: {e}")



async def subdomain_filter(main_domain_docs: List[Document], request: SimilaritySearchRequest) -> List[Document]:

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

    #main_domain_docs 리스트가 비어있다면, 빈 리스트를 반환
    # tool1에서 넘겨 받은 게 없으면, subdomain_filter도 연산 안함.
    if not main_domain_docs:
        return []

    doc_map = {doc.metadata.get('additionalProp1'): doc for doc in main_domain_docs if doc.metadata.get('additionalProp1')}
    main_dic_keys = set(doc_map.keys())


    #1. 입력 데이터 준비, 검색 쿼리 생성
    #필터링의 대상이 될 원본 PK목록을 Set으로 준비하여 빠른 조회를 가능케 함
    ## main_dic_keys = main Dictionary의 value 셋(set)
    main_dic_keys={doc1.metadata.get('additionalProp1') for doc1 in main_domain_docs if doc1.metadata.get('additionalProp1')}

    # 임시로 하드코딩으로메인 도메인에 따른 관련 서브 도메인들 정의
    # 음식집(RESTAURANT)을 검색할 땐, 식당 리뷰(RESTAURANT_REVIEW)와 여행 스타일(TRAVEL_STYLE)과 관련된 DB를 참고해야 함
    subdomain_mapping={
        Domains.RESTAURANT: [Domains.RESTAURANT_REVIEW, Domains.TRAVEL_STYLE],
        Domains.PLACE: [Domains.PLACE_REVIEW, Domains.TRAVEL_TREND],
        Domains.ACCOM: [Domains.ACCOM_REVIEW, Domains.TRAVEL_STYLE]
    }

    #  메인 도메인의 PK를 서브 도메인의 FK로 받아오는 구간
    # subdomain_mapping 딕셔너리에서 현재 요청에 맞는 서브 도메인 목록을 찾아옴
    relevant_sub_domains = subdomain_mapping.get(request.target_domain, [])
    if not relevant_sub_domains:
        return main_domain_docs
    sub_collections = {sub_doc.value: db_collections[sub_doc.value] for sub_doc in relevant_sub_domains}


    #서브 도메인 검색을 위한 쿼리를 생성
    query = f" {' '.join(request.target_keywords)}"

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
            return  main_domain_docs



        # 메인 PK와 서브 PK의 관계를 저장하는 로직으로 변경
        main_to_sub_pks_map = defaultdict(list)
        for sub_doc in all_sub_docs:
            FK = sub_doc.metadata.get('additionalProp1')
            sub_pk = sub_doc.metadata.get('id')
            if FK and sub_pk:
                main_to_sub_pks_map[FK].append(sub_pk)

        #
        if not main_to_sub_pks_map:
            return main_domain_docs

        # --- 4단계: 결과 집계, 순위화 및 반환 ---
        # 언급된 FK의 빈도수를 계산 (많이 언급될수록 순위가 높음)
        #  pk_frequency = linked_FKs의 리스트를 입력으로 받아서 딕셔너리 형태로 만듦
        # filtered_pks =
            # 1차 검색 결과(main_domain_keys)에 포함된 PK들만 필터링
            # 1차 검색 범위에 없던 것은 최종 추천 목록에 들어오는 것을 방지
        # sorted_pks = 빈도수(언급 횟수)가 높은 순으로 정렬

        pk_frequency = {pk: len(sub_pks) for pk, sub_pks in main_to_sub_pks_map.items()}
        filtered_pks = {pk: count for pk, count in pk_frequency.items() if pk in main_dic_keys}
        sorted_pks = sorted(filtered_pks.keys(), key=lambda pk: filtered_pks[pk], reverse=True)

        sorted_docs = [doc_map[pk] for pk in sorted_pks if pk in doc_map]

        print(f"Tool2 최종 결과 (상위 {len(sorted_docs)}개): {[doc.metadata.get('name') for doc in sorted_docs]}")
        return sorted_docs

    except Exception as e:
        print(f"서브 도메인 필터링 중 오류 발생: {e}")
        # 오류 발생 시, 최소한 1차 검색 결과라도 반환하도록 처리
        return list(main_dic_keys)








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


#-----------------------------------------
#
# async def similarity_search(request: SimilaritySearchRequest) -> List[str]:
#     """
#     사용자 요청(request)을 인자로 받아, 메인 검색과 서브 필터링을
#     순차적으로 모두 실행하고 최종 결과를 반환하는 **통합 실행 함수**.
#
#     FastAPI 라우터가 이 함수를 직접 호출하게 됩니다.
#     """
#     print(" 검색 파이프라인을 시작합니다...")
#
#     # 1. main_domain_search 함수를 호출하여 1차 검색을 수행합니다.
#     print("--- 1단계: 메인 도메인 검색 ---")
#     main_docs = await main_domain_search(request)
#
#     # 1차 검색 결과가 없으면 더 진행하지 않고 빈 리스트를 반환합니다.
#     if not main_docs:
#         print(" 최종 추천 PK 리스트: [] (1차 검색 결과 없음)")
#         return []
#
#     # 2. 1차 검색 결과를 다음 함수의 입력으로 사용하여 2차 필터링을 수행합니다.
#     print("--- 2단계: 서브 도메인 필터링 및 재정렬 ---")
#     final_pk_list = await subdomain_filter(main_docs, request)
#
#     # 3. 최종 결과를 반환합니다.
#     print(f"\n최종 추천 PK 리스트: {final_pk_list}")
#     return final_pk_list

async def similarity_search(request: SimilaritySearchRequest) -> List[int]:
    """사용자 요청을 받아 메인 검색과 서브 필터링을 실행하고, 최종 PK 리스트를 반환."""
    print("🚀 검색 파이프라인을 시작합니다...")
    print(f"요청 도메인: {request.target_domain}")
    print(f"키워드: {request.target_keywords}")

    main_docs = await main_domain_search(request)
    print(f"main_domain_search 결과: {len(main_docs)}개 문서")
    if not main_docs:
        print(" main_domain_search에서 결과가 없습니다")
        return []

    final_docs = await subdomain_filter(main_docs, request)
    print(f"subdomain_filter 결과: {len(final_docs)}개 문서")

    # Document의 metadata 구조 확인
    if final_docs:
        print(f"첫 번째 문서 metadata: {final_docs[0].metadata}")

    # --- [핵심 수정] Document 리스트에서 숫자 형태의 PK(ID)만 추출 ---
    final_pks = []
    for doc in final_docs:
        # metadata에서 'id' 대신 'additionalProp1'을 사용
        doc_id = doc.metadata.get('additionalProp1')
        print(f"문서 ID: {doc_id} (타입: {type(doc_id)})")
        if doc_id is not None:
            try:
                final_pks.append(int(doc_id))
            except (ValueError, TypeError) as e:
                print(f"ID 변환 실패: {doc_id} -> {e}")

    print(f"\n✨ 최종 추천 PK 리스트: {final_pks}")

    # [수정] API의 약속에 맞게 Document가 아닌 숫자 PK 리스트를 반환
    return final_pks