from typing import Optional, List
from pydantic import BaseModel, Field

from ..core.constants import Domains

class BaseTravelDetail(BaseModel):
    """
    여행 계획을 세우기 위한 단일 데이터의 기본 타입입니다.
    Tool을 사용할 때 필요한 공통 필드를 포함합니다.
    'name': 이름, 'lon': 경도, 'lat': 위도, 'exp_cost': 예상 비용, 'description': 설명
    """
    name: str
    lat: float
    lon: float
    exp_cost: float
    description: str

class PlaceDetail(BaseTravelDetail):
    """
    여행지에 대한 세부 정보 스키마입니다.
    RDB의 pk와 같은 고유 식별자를 포함합니다.
    """
    pk: int
    category: str | None
    operating_hours: str | None
    required_time: str | None

class RestaurantDetail(BaseTravelDetail):
    """
    식당에 대한 세부 정보 스키마입니다.
    RDB의 pk와 같은 고유 식별자를 포함합니다.
    """
    pk: int
    cuisine_type: str | None
    signature_menu: str | None
    operating_hours: str | None

class AccommodationDetail(BaseTravelDetail):
    """
    숙소에 대한 세부 정보 스키마입니다.
    RDB의 pk와 같은 고유 식별자를 포함합니다.
    """
    pk: int
    accom_type: str
    grade: str | None
    amenities: List[str] | None
    check_in_out_time: str | None
    booking_url: str | None

class Event(BaseModel):
    """
    하루의 개별 일정을 나타내는 스키마입니다.
    각 이벤트는 시간, 종류, 이름, 설명 등의 정보를 가집니다.
    """
    time: str
    domain: Domains
    name: str
    description: str
    address: str | None
    estimated_cost: str | None

class DailyPlan(BaseModel):
    """
    하루 동안의 전체 계획을 나타내는 스키마입니다.
    """
    day: int
    date: str | None
    description: str
    events: List[Event]

class TravelPlan(BaseModel):
    """
    전체 여행 계획을 나타내는 최상위 스키마입니다.
    여행 계획 에이전트는 반드시 이 형식에 맞춰 최종 결과를 반환해야 합니다.
    Superviser 에이전트는 이 스키마를 기준으로 결과물의 유효성을 검사합니다.
    """
    plan_title: str
    destination: str
    total_days: int
    daily_plans: List[DailyPlan]

class ChatMessage(BaseModel):
    """채팅 메시지 단일 객체"""
    role: str  # "user" 또는 "assistant"
    content: str

class TravelData(BaseModel):
    """
    여행 계획 에이전트의 전체 상태(State)를 정의하는 스키마입니다.
    이 데이터 구조는 LangGraph의 노드를 거치며 업데이트됩니다.
    """
    # 전체 대화 내용 (LLM이 맥락을 파악하는 데 중요)
    conversation_history: List[ChatMessage]

    # 에이전트가 사용할 검색된 정보 목록
    places: List[PlaceDetail]
    restaurants: List[RestaurantDetail]
    accommodations: List[AccommodationDetail]
    # 사용자의 추가 요청
    additional_info: List[str] | None

    # 각 워커의 결과물을 저장할 필드
    worker_results: List[TravelPlan]
    # 최종적으로 생성될 여행 계획
    plan: Optional[TravelPlan]

class PlanningRequest(BaseModel):
    """
    여행 계획 생성을 요청하는 스키마입니다.
    프론트엔드에서 수집된 모든 정보가 포함됩니다.
    """
    # 1. RDB에서 가져온 기본 프롬프트
    supervisor_prompt: str

    # 2. 사용자가 추가로 입력한 구체적인 요구사항
    user_requests: Optional[List[str]] = Field(default_factory=lambda: ["특별한 추가 요구사항 없음"])

    # 3. 전체 대화 내용 (LLM이 맥락을 파악하는 데 중요)
    conversation_history: List[ChatMessage]

    # 4. 사용자와의 상호작용을 통해 선택된 후보 데이터 목록
    candidate_places: List[PlaceDetail]
    candidate_restaurants: List[RestaurantDetail]
    candidate_accommodations: List[AccommodationDetail]

class PlanningResponse(BaseModel):
    """
    생성된 여행 계획과 그 근거를 반환하는 스키마입니다.
    """
    # 1. 최종적으로 완성된 여행 계획
    final_plan: TravelPlan

    # 2. Supervisor가 최종 계획을 선택한 이유
    # supervisor_reasoning: str

    # 3. 최종 선택된 Worker의 이름과 그 Worker가 제시한 계획 원본
    # selected_worker_name: str
    # selected_worker_output: str # Supervisor가 평가한 계획안 원본 텍스트