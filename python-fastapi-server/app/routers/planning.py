import logging
from fastapi import APIRouter

from ..services import planning_service
from ..schemas.planning import PlanningRequest ,PlanningResponse

logger = logging.getLogger(__name__)

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
    logger.info(f"\n'make_plan' 요청\n요청 내용: \n\n{planning_request}\n")
    try:
        result = await planning_service.make_travel_plan(planning_request)
        logger.info(f"\n여행 계획 세우기 완료\n==========결과==========\n{result}\n")
        return result
    except Exception as e:
        logger.error(e)