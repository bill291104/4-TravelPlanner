# app/core/constants.py
from enum import Enum
from typing import List

class Domains(str, Enum):
    """
    애플리케이션에서 사용되는 모든 도메인을 정의하는 단일 Enum입니다.
    도메인은 '메인 도메인'과 '서브 도메인'으로 구분됩니다.
    - 메인 도메인: 여행 계획의 핵심 대상 (장소, 식당, 숙소)
    - 서브 도메인: 메인 도메인에 대한 부가 정보 (리뷰, 여행 스타일 등)
    """
    # --- Main Domains ---
    PLACE = "place"
    RESTAURANT = "restaurant"
    ACCOM = "accom"

    # --- Sub Domains ---
    PLACE_REVIEW = "place_review"
    RESTAURANT_REVIEW = "restaurant_review"
    ACCOM_REVIEW = "accom_review"
    TRAVEL_STYLE = "travel_style"
    TRAVEL_TREND = "travel_trend"

    @property
    def get_subs(self) -> List[str]:
        match self:
            case Domains.PLACE:
                # return [Domains.PLACE_REVIEW.value(), Domains.TRAVEL_STYLE.value(), Domains.TRAVEL_TREND.value()]
                return [Domains.PLACE_REVIEW.value, Domains.TRAVEL_STYLE.value, Domains.TRAVEL_TREND.value]
            case Domains.RESTAURANT:
                # return [Domains.RESTAURANT_REVIEW.value(), Domains.TRAVEL_STYLE.value(), Domains.TRAVEL_TREND.value()]
                return [Domains.RESTAURANT_REVIEW.value, Domains.TRAVEL_STYLE.value, Domains.TRAVEL_TREND.value]
            case Domains.ACCOM:
                # return [Domains.ACCOM_REVIEW.value(), Domains.TRAVEL_STYLE.value(), Domains.TRAVEL_TREND.value()]
                return [Domains.ACCOM_REVIEW.value, Domains.TRAVEL_STYLE.value, Domains.TRAVEL_TREND.value]
            case _:
                return []

    @property
    def is_main(self) -> bool:
        """
        이 도메인이 메인 도메인인지 여부를 반환합니다.
        메인 도메인은 여행 계획의 핵심이 되는 장소, 식당, 숙소 등을 의미합니다.
        """
        return self in {Domains.PLACE, Domains.RESTAURANT, Domains.ACCOM}

    @property
    def is_sub(self) -> bool:
        """
        이 도메인이 서브 도메인인지 여부를 반환합니다.
        서브 도메인은 메인 도메인에 대한 부가적인 정보(리뷰, 트렌드 등)를 의미합니다.
        """
        return not self.is_main

    def get_description(self) -> str:
        """
        LLM 프롬프트에 사용될 각 도메인에 대한 상세 설명을 반환합니다.
        AI 에이전트가 각 도메인의 역할과 의미를 명확히 이해하도록 돕습니다.
        """
        match self:
            # Main Domain Descriptions
            case Domains.PLACE:
                return "PLACE: 사용자가 방문할 수 있는 물리적인 장소. 관광 명소, 공원, 랜드마크, 특정 지역 등을 포함합니다. 여행 계획의 핵심 목적지가 됩니다."
            case Domains.RESTAURANT:
                return "RESTAURANT: 음식이나 음료를 제공하는 장소. 식당, 카페, 바, 베이커리 등을 포함합니다. 사용자가 식사나 휴식을 위해 방문할 수 있습니다."
            case Domains.ACCOM:
                return "ACCOM: 여행자가 머물 수 있는 숙박 시설. 호텔, 펜션, 게스트하우스, 리조트 등을 포함합니다. 여행 중 잠을 자거나 휴식을 취하는 장소입니다."

            # Sub Domain Descriptions
            case Domains.PLACE_REVIEW:
                return "PLACE_REVIEW: 'PLACE' 도메인에 속한 특정 장소에 대한 사용자 리뷰나 평가 정보입니다. 이 정보는 특정 'PLACE'의 장단점을 파악하고 필터링하는 데 사용됩니다."
            case Domains.RESTAURANT_REVIEW:
                return "RESTAURANT_REVIEW: 'RESTAURANT' 도메인에 속한 특정 식당이나 카페에 대한 사용자 리뷰나 평가 정보입니다. 맛, 분위기, 서비스 등에 대한 의견을 포함하며 필터링 조건으로 활용됩니다."
            case Domains.ACCOM_REVIEW:
                return "ACCOM_REVIEW: 'ACCOM' 도메인에 속한 특정 숙소에 대한 사용자 리뷰나 평가 정보입니다. 청결도, 편의시설, 위치 등에 대한 경험을 담고 있으며 필터링에 사용됩니다."
            case Domains.TRAVEL_STYLE:
                return "TRAVEL_STYLE: 특정 사용자의 일반적인 여행 선호도나 스타일을 나타냅니다. (예: '가성비 여행', '럭셔리 여행', '가족 여행', '혼자 여행') 이 정보는 사용자의 취향에 맞는 메인 도메인(장소, 식당, 숙소)을 추천하는 데 사용됩니다."
            case Domains.TRAVEL_TREND:
                return "TRAVEL_TREND: 현재 유행하는 여행 경향이나 인기있는 주제에 대한 정보입니다. (예: '2024년 여름 인기 여행지', '요즘 뜨는 여행 액티비티') 이 정보는 새로운 아이디어를 얻거나 인기있는 선택지를 추천하는 데 사용됩니다."
            case _:
                # 모든 Enum 멤버가 위에 명시되어 있으므로, 실제로는 이 코드가 실행될 일이 없습니다.
                return "알 수 없는 도메인입니다."