# app/services/schemas/planning.py
from typing import TypedDict, List
from ...core.constants import Domains

class Event(TypedDict):
    """
    하루의 개별 일정을 나타내는 스키마입니다.
    각 이벤트는 시간, 종류, 이름, 설명 등의 정보를 가집니다.
    """
    time: str               # 일정 시작 시간 (예: "10:00", "14:30")
    domain: Domains          # 이 이벤트의 종류 (Domains.PLACE, Domains.RESTAURANT 등)
    name: str               # 장소, 식당, 숙소 등의 구체적인 이름
    description: str        # 활동에 대한 간략한 설명 (예: "해변 산책 및 사진 촬영")
    address: str | None     # 주소 정보 (선택 사항)
    estimated_cost: str | None # 예상 비용 (선택 사항, 예: "인당 25,000원")


class DailyPlan(TypedDict):
    """
    하루 동안의 전체 계획을 나타내는 스키마입니다.
    """
    day: int                # 여행 n일차 (예: 1, 2, 3)
    date: str | None        # 해당 날짜 (예: "2025-07-26", 선택 사항)
    description: str        # 그날의 전체적인 테마나 요약 (예: "해운대 주변 맛집과 명소 탐방")
    events: List[Event]     # 해당 날짜의 상세 일정 목록


class TravelPlan(TypedDict):
    """
    전체 여행 계획을 나타내는 최상위 스키마입니다.
    여행 계획 에이전트는 반드시 이 형식에 맞춰 최종 결과를 반환해야 합니다.
    Superviser 에이전트는 이 스키마를 기준으로 결과물의 유효성을 검사합니다.
    """
    plan_title: str             # 여행 계획의 전체 제목 (예: "부산 3박 4일 여름 휴가")
    destination: str            # 주요 여행지 (예: "부산광역시")
    total_days: int             # 총 여행 기간 (예: 4)
    daily_plans: List[DailyPlan] # 일자별 계획 목록
