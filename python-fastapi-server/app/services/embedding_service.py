# app/services/embedding_service.py
from langchain_core.documents import Document
from openai import OpenAIError
from ..schemas.embedding import EmbeddingRequest
from typing import List

# 커스텀 예외 임포트
from ..core.exceptions import (
    EmbeddingCreationError,
    EmbeddingDeletionError,
    TravelDocumentNotFoundError,
    InvalidInputError
)


async def create_embedding(request: EmbeddingRequest, db_collection):
    """
    요청 데이터를 바탕으로 Document를 생성하고 DB에 저장합니다.
    (ContentTooLongError 적용)
    """
    if not request.content: # ""
        raise InvalidInputError(detail="임베딩 할 내용(Content)이 비어 있습니다.")


    content = request.content
    if request.related_contents:
        content += "\n\n추가 정보\n\n" + "\n".join(request.related_contents)

    document = Document(
        id=str(request.pk),
        page_content=content,
        metadata=request.metadata
    )
    print("request", request)
    print(f"DEBUG: Service: ChromaDB에 추가될 Document: {document.id}, {document.page_content[:30]}...")

    try:
        await db_collection.aadd_documents([document])
        print(f"Document with id = '{request.pk}' added to collection")

    except OpenAIError as e:
        raise EmbeddingCreationError(detail = f"OpenAI 임베딩 API 오류: {e}") from e
    except Exception as e:
        raise EmbeddingCreationError(detail=f"사용자 답변 임베딩 생성 중 알 수 없는 오류: {e}") from e

async def create_embeddings_batch(requests: List[EmbeddingRequest], db_collection):
    """
    여러 요청 데이터를 바탕으로 Document 리스트를 생성하고 DB에 한 번에 저장합니다.
    (ContentTooLongError 적용)
    """
    if not requests:
        raise InvalidInputError(detail="배치 임베딩을 위한 요청 목록이 비어 있습니다.")

    documents = []
    for request in requests:
        if not request.content:
            raise InvalidInputError(detail=f"PK {request.pk}의 사용자 답변(Content)이 비어 있습니다.")

        content = request.content
        if request.related_contents:
            content += "\n\n추가 정보\n\n" + "\n".join(request.related_contents)

        document = Document(
            id=str(request.pk),
            page_content=content,
            metadata=request.metadata
        )
        documents.append(document)

    try:
        if documents: # 빈 리스트일 경우 add_documents 호출 x
            await db_collection.aadd_documents(documents)
            print(f"{len(documents)} documents added to collection")

    except OpenAIError as e:
        raise EmbeddingCreationError(detail = f"OpenAI 임베딩 API 오류 (배치): {e}") from e

    except Exception as e:
        raise EmbeddingCreationError(detail=f"사용자 답변 배치 임베딩 생성 중 알 수 없는 오류 발생 : {e}") from e

async def delete_embedding(pk: int, db_collection):
    """
    주어진 pk(id)에 해당하는 문서를 DB에서 삭제합니다.
    """
    str_pk = str(pk)

    try:
        # 문서가 존재하면 삭제 진행
        await db_collection.adelete(ids=[str_pk])
        print(f"Document with id='{pk}' deleted from collection.")

    except Exception as e:
        raise EmbeddingDeletionError(detail=f"사용자 답변 임베딩 삭제 중 알 수 없는 오류: {e}") from e

async def delete_embeddings_batch(pks: List[int], db_collection):
    """
    주어진 pk 리스트에 해당하는 문서들을 DB에서 한 번에 삭제합니다.
    """
    if not pks:
        raise InvalidInputError(detail="배치 삭제를 위한 PK 목록이 비어 있습니다.")

    # ChromaDB는 id를 문자열 리스트로 받습니다.
    ids_to_delete = [str(pk) for pk in pks]
    try:
        if ids_to_delete:
            await db_collection.adelete(ids=ids_to_delete)
            print(f"{len(ids_to_delete)} documents deleted.")

    except Exception as e:
        raise EmbeddingDeletionError(detail=f"사용자 답변 배치 임베딩 삭제 중 알 수 없는 오류: {e}") from e
