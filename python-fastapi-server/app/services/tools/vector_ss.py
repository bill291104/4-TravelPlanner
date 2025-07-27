from typing import List, Literal

from ...db.session import db_collections

# Tool_1 main domain similarity search
async def main_domain_similarity_search(main_domain: Literal["place", "restaurant", "accom"], main_keywords: List[str]) -> List[int]:
    """
    main_domain 에 해당하는 Vector DB 의 Collection 에서 main_keywords 를 가지고 유사도 검색을 수행합니다.
    유사도 검색을 수행한 결과에서 id 들을 추출하여 반환합니다. 이 id list 는 List[int] 입니다.
    args:
        main_domain: Literal["place", "restaurant", "accom"]    main_domain 에 해당하는 Collection 의 이름
        main_keywords: List[str]                                유사도 검색에 사용하는 키워드들
    return:
        List[int]       유사도 검색 결과들에서 id 를 추출하여 int 로 형변환 한 결과 리스트
    """
    main_collection = db_collections[main_domain]
    pks = set()
    for keyword in main_keywords:
        documents = await main_collection.asimilarity_search(query=keyword, k=10)
        for d in documents:
            pks.add(int(d.id))
    return list(pks)

# Tool_2 subdomain filter
async def subdomain_filter(
        target_pks: List[int],
        subdomain: Literal["place_review", "restaurant_review", "accom_review", "travel_style", "travel_trend"],
        sub_keywords: List[str]
) -> List[int]:
    """
    main domain 에서 유사도 검색한 결과 id list 혹은 subdomain 으로 필터링 된 결과 id list 에서
    subdomain 에 해당하는 Vector DB 컬렉션에 sub_keywords 를 가지고 유사도 검색을 통해 겹치지 않는 요소를 target_pks 에서 필터링 합니다.
    target_pks 에서 sub_keywords 와 맥락이 맞지 않는 것들이 제외 되고 그 결과를 반환 합니다.
    args:
        target_pks: List[int]   필터링의 대상. main_domain_similarity_search 의 결과 혹은 subdomain_filter 의 결과가 인자로 전달 되어야 함
        subdomain: Literal["place_review", "restaurant_review", "accom_review", "travel_style", "travel_trend"]     필터링의 기준이 되는 subdomain 에 해당하는 Collection 의 이름
        sub_keywords: List[str]     필터링의 기준이 되는 키워드. subdomain Collection 에서 유사도 검색의 query 에 해당
    return:
        List[int]   전달 받은 target_pks 에서 일부 요소가 sub_keywords 를 기준으로 필터링 된 상태의 결과물
    """
    sub_collection = db_collections[subdomain]
    filter_set = set()
    for keyword in sub_keywords:
        documents = await sub_collection.asimilarity_search(query=keyword, k=10)
        for d in documents:
            filter_set.add(int(d.metadata['fk']))
    target_set = set(target_pks)
    return list(target_set & filter_set)