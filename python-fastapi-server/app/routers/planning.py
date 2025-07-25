from fastapi import APIRouter, HTTPException

from ..services import planning_service
from ..schemas.planning import PlanningRequest ,PlanningResponse

router = APIRouter(
    prefix="/planning",
    tags=["Planning"]
)

@router.post(
    "/make",
    response_model=PlanningResponse,
    summary="제공된 데이터를 통해 여행 계획을 생성",
    description="Supervisor Architecture 를 사용해서 3개의 성격을 가진 계획중 사용자 요청에 가장 적합한 계획을 선정하여 반환합니다."
)
async def make_plan(planning_request: PlanningRequest):
    """
    여행 계획 생성 서비스 함수를 호출하고 결과를 반환합니다.

    :param planning_request: PlanningRequest
    :return: PlanningResponse
    """
    return await planning_service.make_travel_plan(planning_request)