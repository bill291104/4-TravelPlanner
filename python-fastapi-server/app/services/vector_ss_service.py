from ..schemas.vector_ss import SimilaritySearchRequest
from langgraph.prebuilt import create_react_agent
from ..llm.client import get_llm

async def similarity_search(request: SimilaritySearchRequest):
    def get_weather(city: str) -> str:
        """Get weather for a given city."""
        return f"It's always sunny in {city}!"

    prompt = "You are a helpful assistant"

    model = get_llm(temperature=0.1)
    agent = create_react_agent(model=model, tools=[get_weather], prompt=prompt)

    result = await agent.ainvoke(
        {"messages": [{"role": "user", "content": "what is the weather in sf"}]}
    )

    print(result.values())
    return [1,2,3]