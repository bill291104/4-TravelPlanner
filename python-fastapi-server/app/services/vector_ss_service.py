from typing import List

from ..schemas.vector_ss import SimilaritySearchRequest
from langgraph.prebuilt import create_react_agent
from ..llm.client import get_llm
from .tools.vector_ss import main_domain_similarity_search, subdomain_filter

async def similarity_search(request: SimilaritySearchRequest):
    template = request.prompt_template
    subdomains = request.target_domain.get_subs
    prompt =  template.format(
        main_domain=request.target_domain.value,
        subdomains=subdomains,
        keywords=request.target_keywords
    )

    model = get_llm(temperature=0.1)
    tools = [main_domain_similarity_search, subdomain_filter]
    agent = create_react_agent(model=model, tools=tools, prompt=prompt, response_format=List[int], debug=True)

    message = f"""
    main_domain 에 해당하는 Collection 이름: {request.target_domain.value}
    main_domain 에 종속된 subdomain 의 Collection 이름들: {request.target_domain.get_subs}
    분류 후 유사도 검색에 사용될 target_keywords: {request.target_keywords}
    """
    return await agent.ainvoke({"messages": [("user", message)]})