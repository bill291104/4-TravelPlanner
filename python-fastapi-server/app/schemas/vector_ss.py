from builtins import str

from pydantic import BaseModel
from typing import List

from ..core.constants import Domains

class SimilaritySearchRequest(BaseModel):
    context:str
    target_domain: Domains
    target_keywords : List[str]
    prompt_template: str
    # main_keywords: List[str]
    # sub_keywords: List[str]
