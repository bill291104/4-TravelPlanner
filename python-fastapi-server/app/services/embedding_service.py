# app/services/embedding_service.py
from langchain_core.documents import Document
from ..schemas.embedding import EmbeddingRequest
from typing import List

async def create_embedding(request: EmbeddingRequest, db_collection):
    """
    요청 데이터를 바탕으로 Document를 생성하고 DB에 저장합니다.
    """
    content = request.content
    if request.related_contents:
        content += "\n\n추가 정보\n\n" + "\n".join(request.related_contents)

    document = Document(
        id=str(request.pk),
        page_content=content,
        metadata=request.metadata
    )
    db_collection.aadd_documents([document])

async def create_embeddings_batch(requests: List[EmbeddingRequest], db_collection):
    """
    여러 요청 데이터를 바탕으로 Document 리스트를 생성하고 DB에 한 번에 저장합니다.
    """
    documents = []
    for request in requests:
        content = request.content
        if request.related_contents:
            content += "\n\n추가 정보\n\n" + "\n".join(request.related_contents)

        document = Document(
            id=str(request.pk),
            page_content=content,
            metadata=request.metadata
        )
        documents.append(document)

    if documents:
        db_collection.aadd_documents(documents)

async def delete_embedding(pk: int, db_collection):
    """
    주어진 pk(id)에 해당하는 문서를 DB에서 삭제합니다.
    """
    await db_collection.adelete(ids=[str(pk)])

async def delete_embeddings_batch(pks: List[int], db_collection):
    """
    주어진 pk 리스트에 해당하는 문서들을 DB에서 한 번에 삭제합니다.
    """
    # ChromaDB는 id를 문자열 리스트로 받습니다.
    ids_to_delete = [str(pk) for pk in pks]
    if ids_to_delete:
        db_collection.adelete(ids=ids_to_delete)
        print(f"{len(ids_to_delete)} documents deleted.")