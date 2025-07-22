from pydantic import BaseModel
from typing import List

from ..core.constants import Domains

class SimilaritySearchRequest(BaseModel):
    context:str
    target_domain: Domains
    target_keywords : List[str]
    prompt_template: str
