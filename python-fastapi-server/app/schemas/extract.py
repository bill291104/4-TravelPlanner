# app/schemas/extract.py
from typing import List
from pydantic import BaseModel

from ..core.constants import Domains

class DomainExtractRequest(BaseModel):
    context: str
    domains: List[Domains]
    prompt_template:str

class KeywordExtractRequest(BaseModel):
    context:str
    target_domain: Domains
    prompt_template:str