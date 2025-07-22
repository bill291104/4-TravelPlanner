# app/core/constants.py
from enum import Enum

class Domains(str, Enum):
    PLACE = "place"
    RESTAURANT = "restaurant"
    ACCOM = "accom"

    def get_description(self) -> str:
        """
        LLM 프롬프트에 사용될 각 도메인에 대한 상세 설명을 반환합니다.
        """
        match self:
            case Domains.PLACE:
                return "PLACE: 관광 명소, 방문할 만한 장소 또는 특정 지역을 의미합니다."
            case Domains.RESTAURANT:
                return "RESTAURANT: 식당, 카페, 바 등 음식을 판매하는 장소를 의미합니다."
            case Domains.ACCOM:
                return "ACCOM: 호텔, 펜션, 게스트하우스 등 숙박 시설을 의미합니다."
            case _:
                return "알 수 없는 도메인입니다."