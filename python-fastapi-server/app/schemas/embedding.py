# app/schemas/embedding.py
from pydantic import BaseModel
from typing import List, Union, Dict

class EmbeddingRequest(BaseModel):
    pk: int
    content: str
    metadata: Dict[str, str]
    related_contents: Union[List[str], None] = None