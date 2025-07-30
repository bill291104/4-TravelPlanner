from langgraph.prebuilt import create_react_agent
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser
from langgraph.graph.state import StateGraph, END

from ..schemas.planning import PlanningRequest, PlanningResponse
from .tools.planning import price_summation, tmap_route_optimizer_with_details
from ..schemas.planning import TravelData, BaseTravelDetail, TravelPlan, DailyPlan, PlaceDetail, RestaurantDetail, AccommodationDetail
from ..llm.client import get_llm

async def make_travel_plan(request: PlanningRequest) -> PlanningResponse:
    # === 1. 준비 단계 (Preparation) ===

    # 1.1. 요청(Request) 데이터 추출
    #      - PlanningRequest 객체에서 base_prompt, user_requests, 대화 기록, 후보 장소 목록 등을 변수에 할당합니다.

    # 1.2. LangGraph 초기 상태(State) 생성
    #      - 추출한 데이터를 바탕으로 StateGraph가 사용할 TravelData 상태 객체를 초기화합니다.
    #      - places, restaurants, accommodations 목록을 채우고, plan, worker_results 등은 초기값(None, [])으로 설정합니다.

    travel_state = TravelData(
        conversation_history=request.conversation_history,

        places=request.candidate_places,
        restaurants=request.candidate_restaurants,
        accommodations=request.candidate_accommodations,

        additional_info=request.user_requests,
        worker_results=[],
        plan=None
    )

    # === 2. 에이전트 및 노드 정의 (Agent & Node Definition) ===

    # 2.1. Tool 정의
    #      - price_summation, tmap_route_optimizer_with_details 등 Worker들이 사용할 Tool 목록을 준비합니다.

    worker_tools=[price_summation, tmap_route_optimizer_with_details]

    # 2.2. Worker Agent 생성
    #      - 각기 다른 시스템 프롬프트(효율, 감성, 휴식)를 사용하여 3개의 Worker Agent를 생성합니다.

    # 1. 실용적인 여행 플래너 (Practical Planner)
    practical_planner = create_react_agent(
        model=get_llm(temperature=0.1), # 논리적이고 일관된 결과를 위해 온도를 낮게 설정
        tools=worker_tools,
        prompt="""당신은 최종 결과물인 `TravelPlan` JSON 객체를 생성하는 임무를 맡은 '실용적인 여행 플래너'입니다.
        
        **규칙:**
        - 당신의 유일한 목표는 주어진 정보를 사용하여 `TravelPlan` 스키마의 모든 필드를 완벽하게 채우는 것입니다.
        - 절대로 사용자에게 추가 질문을 하지 마세요. 제공된 정보가 전부입니다.
        - 모든 작업은 Tool을 사용하여 논리적 근거를 확보한 후 진행해야 합니다.

        **작업 절차:**
        1.  **총 경비 계산:** `TravelPlan`의 `total_estimated_cost` 필드를 채우기 위해, 먼저 `price_summation` Tool을 호출하여 모든 후보 장소의 비용 합계를 계산하세요.
        2.  **경로 최적화:** `TravelPlan`의 `daily_plans` 필드를 구성하기 위해, 반드시 `tmap_route_optimizer_with_details` Tool을 호출하세요. 이 Tool의 결과는 일정 순서와 이동 시간을 결정하는 유일한 근거가 됩니다.
        3.  **일정 구성:** Tool로 얻은 최적 경로 순서와 이동 시간 정보를 바탕으로 `daily_plans` 배열을 생성하세요. 각 `Event` 객체의 `travel_time_from_previous` 필드에 계산된 이동 시간을 정확히 기입해야 합니다.
        4.  **최종 제출:** 위 과정을 통해 모든 필드가 채워진 완벽한 `TravelPlan` JSON 객체를 생성하여 제출하세요.
        """,
        name="practical_planner",
        response_format=TravelPlan,
        debug=True
    )

    # 2. 감성 여행 플래너 (Creative Planner)
    creative_planner = create_react_agent(
        model=get_llm(temperature=0.7), # 창의적인 작성을 위해 온도를 높게 설정
        tools=worker_tools,
        prompt="""당신은 최종 결과물인 `TravelPlan` JSON 객체를 생성하는 임무를 맡은 '감성 여행 플래너'입니다.
        
        **규칙:**
        - 당신의 유일한 목표는 주어진 정보를 사용하여 `TravelPlan` 스키마의 모든 필드를 완벽하게 채우는 것입니다.
        - 절대로 사용자에게 추가 질문을 하지 마세요. 제공된 정보가 전부입니다.
        - 감성적인 계획을 세우되, 모든 일정은 Tool을 사용한 데이터에 기반해야 합니다.

        **작업 절차:**
        1.  **총 경비 계산:** `TravelPlan`의 `total_estimated_cost` 필드를 채우기 위해, 먼저 `price_summation` Tool을 호출하여 비용 합계를 계산하세요.
        2.  **이동 시간 파악:** 감성적인 동선을 구상하기 위해, 먼저 `tmap_route_optimizer_with_details` Tool을 호출하여 장소 간의 현실적인 이동 시간을 파악하세요. 이는 당신의 창의적인 계획이 실현 가능하도록 만드는 중요한 과정입니다.
        3.  **테마 기반 일정 구성:** Tool로 파악한 이동 시간을 참고하여, 당신의 창의적인 테마에 맞게 `daily_plans`를 구성하세요. 각 `Event`의 `travel_time_from_previous` 필드에 이동 시간을 반드시 포함시켜야 합니다.
        4.  **최종 제출:** 위 과정을 통해 모든 필드가 채워진 완벽한 `TravelPlan` JSON 객체를 생성하여 제출하세요. `plan_title`과 각 `description`은 최대한 감성적으로 작성하세요.
        """,
        name="creative_planner",
        response_format=TravelPlan,
        debug=True
    )

    # 3. 여유로운 여행 플래너 (Flexible Planner)
    flexible_planner = create_react_agent(
        model=get_llm(temperature=0.4), # 너무 창의적이지도, 너무 경직되지도 않게 중간 온도로 설정
        tools=worker_tools,
        prompt="""당신은 최종 결과물인 `TravelPlan` JSON 객체를 생성하는 임무를 맡은 '여유로운 여행 플래너'입니다.
        
        **규칙:**
        - 당신의 유일한 목표는 주어진 정보를 사용하여 `TravelPlan` 스키마의 모든 필드를 완벽하게 채우는 것입니다.
        - 절대로 사용자에게 추가 질문을 하지 마세요. 제공된 정보가 전부입니다.
        - '여유'를 주제로 하되, 모든 일정은 Tool을 사용한 데이터에 기반해야 합니다.

        **작업 절차:**
        1.  **장소 선정:** '여유'를 위해 하루에 방문할 장소를 2~3개로 최소화하여 직접 선정하세요.
        2.  **총 경비 계산:** `TravelPlan`의 `total_estimated_cost` 필드를 채우기 위해, 당신이 선정한 장소들을 대상으로 `price_summation` Tool을 호출하여 비용 합계를 계산하세요.
        3.  **최소 이동 경로 계산:** `TravelPlan`의 `daily_plans`를 구성하기 위해, 당신이 선정한 장소들을 대상으로 `tmap_route_optimizer_with_details` Tool을 호출하여 이동 시간을 최소화하는 경로를 찾으세요. 이것이 '여유'를 극대화하는 방법입니다.
        4.  **넉넉한 일정 구성:** 계산된 이동 시간을 바탕으로, 각 `Event` 사이에 충분한 휴식 시간을 포함하여 `daily_plans`를 구성하세요. `travel_time_from_previous` 필드에 계산된 이동 시간을 정확히 기입해야 합니다.
        5.  **최종 제출:** 위 과정을 통해 모든 필드가 채워진 완벽한 `TravelPlan` JSON 객체를 생성하여 제출하세요.
        """,
        name="flexible_planner",
        response_format=TravelPlan,
        debug=True
    )
    # 2.3. Supervisor Agent 생성
    #      - Worker들의 결과물을 평가하고 최종 선택할 Supervisor Agent를 생성합니다. (Tool이 필요 없을 수 있음)

    async def supervisor_node(state: TravelData):
        """
        Worker들의 결과물을 평가하고, 최종 TravelPlan 객체를 생성하여 반환하는 노드입니다.
        """
        print("--- SUPERVISOR: 최종 계획 생성 시작 ---")

        # 1. Pydantic 파서 생성
        parser = PydanticOutputParser(pydantic_object=TravelPlan)

        # 2. Worker 결과물과 대화 기록을 프롬프트에 넣기 좋게 문자열로 준비
        plan_strings = [
            f"--- {i+1}번 계획안 ---\n{plan.model_dump_json(indent=2)}"
            for i, plan in enumerate(state.worker_results)
        ]
        worker_outputs = "\n\n".join(plan_strings)
        conversation_history_str = "\n".join([f"{msg.role}: {msg.content}" for msg in state.conversation_history])

        # 3. 평가와 포맷팅 지시를 하나로 합친 프롬프트 템플릿 생성
        prompt_template = ChatPromptTemplate.from_template(
            """당신은 사용자의 숨은 의도까지 파악하는 최고의 여행 계획 분석가입니다.
    
            먼저 아래 '사용자와의 대화 내용'을 깊이 있게 분석하여 사용자의 진짜 취향과 우선순위를 파악하세요.
            그 다음, 파악된 사용자 취향을 기준으로 아래 '계획안 목록'의 장단점을 비교하여 최고의 계획 하나를 선택하세요.
    
            마지막으로, 당신의 최종 선택과 그 이유를 바탕으로 여행 계획의 모든 필드를 채워 {format_instructions}에 맞는 JSON 객체로 출력해주세요.
            'plan_title'은 대화 내용과 최종 계획안을 바탕으로 창의적으로 작성해주세요.
    
            ---
            [사용자와의 대화 내용]
            {conversation_history}
    
            ---
            [계획안 목록]
            {worker_outputs}
            """
        )

        # 4. LLM과 파서를 연결한 체인 생성
        chain = prompt_template | get_llm(temperature=0.4) | parser

        # 5. 체인을 "한 번만" 호출하여 최종 TravelPlan 객체를 얻음
        final_plan_object = await chain.ainvoke({
            "format_instructions": parser.get_format_instructions(),
            "conversation_history": conversation_history_str,
            "worker_outputs": worker_outputs
        })

        # 6. 파싱된 TravelPlan 객체를 State에 업데이트하기 위해 반환
        return {"plan": final_plan_object}

    # 2.4. 그래프 노드(Node) 함수 정의
    #      - 각 Worker Agent를 실행하고 결과를 반환할 함수 (예: run_efficient_worker)
    async def run_worker(state: TravelData, agent, name: str):
        """공통 Worker 실행 로직"""
        print(f"--- {name.upper()} PLANNER: 실행 시작 ---")

        # 에이전트에 전달할 입력 메시지 구성
        input_message = f"""
        당신은 당신의 역할과 지침에 따라 여행 계획을 생성해야 합니다. 제공된 후보 목록과 사용자 요청을 사용하여 즉시 계획 생성을 시작하세요. 먼저 Tool을 사용하여 경로를 최적화하고 계획을 구성한 다음, 최종 결과를 제시하세요. 사용자에게 다시 질문하지 말고 작업을 완료하세요.

        **후보 장소:**
        {state['places']}

        **후보 식당:**
        {state['restaurants']}

        **후보 숙소:**
        {state['accommodations']}

        **추가 요청사항:**
        {state['additional_info']}
        """

        # 에이전트 실행
        response = await agent.ainvoke({"messages": [("user", input_message)]})

        print(f"--- {name.upper()} PLANNER: 실행 완료 ---")

        # 결과를 기존 worker_results에 추가하여 반환
        return {"worker_results": state['worker_results'] + [response]}

    async def run_practical_planner(state: TravelData):
        return await run_worker(state, practical_planner, "practical_planner")

    async def run_creative_planner(state: TravelData):
        return await run_worker(state, creative_planner, "creative_planner")

    async def run_flexible_planner(state: TravelData):
        return await run_worker(state, flexible_planner, "flexible")

    # === 3. 그래프 구성 (Graph Construction) ===

    # 3.1. StateGraph 초기화
    workflow = StateGraph(TravelData)

    # 3.2. 노드 추가
    workflow.add_node("practical_planner", practical_planner)
    workflow.add_node("creative_planner", creative_planner)
    workflow.add_node("flexible_planner", flexible_planner)
    workflow.add_node("supervisor", supervisor_node)

    # 3.3. 엣지(Edge) 구성
    #      - **(병렬 처리)** 3개의 Worker 노드를 모두 진입점(Entry Point)으로 설정합니다.
    workflow.set_entry_point("practical_planner")
    workflow.set_entry_point("creative_planner")
    workflow.set_entry_point("flexible_planner")

    #      - **(결과 취합)** 모든 Worker 노드의 실행이 끝나면, 그 결과들을 모아 Supervisor 노드로 전달하는 엣지를 추가합니다.
    #        - LangGraph에서는 여러 개의 노드가 하나의 노드로 합쳐지는 지점을 명시적으로 만들어야 합니다.
    workflow.add_edge(["practical_planner", "creative_planner", "flexible_planner"], "supervisor")

    #      - **(종료)** Supervisor 노드가 실행된 후에는 그래프를 종료(END)하는 엣지를 추가합니다.
    workflow.add_edge("supervisor", END)


    # === 4. 실행 및 반환 (Execution & Response) ===

    # 4.1. 그래프 컴파일
    app = workflow.compile()

    # 4.2. 비동기 실행
    final_state = await app.ainvoke(travel_state)

    # 4.3. 응답(Response) 객체 생성 및 반환
    #      - final_state에 담긴 최종 계획(plan), Supervisor의 판단 근거 등을 PlanningResponse 스키마에 맞게 변환하여 반환합니다.
    return PlanningResponse(
        final_plan=final_state.get("plan")
    )