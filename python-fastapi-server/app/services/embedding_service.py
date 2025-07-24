# app/services/embedding_service.py
from langchain.smith.evaluation.string_run_evaluator import LLMStringRunMapper
from langchain_core.documents import Document
from openai import APIConnectionError, OpenAIError
from ..schemas.embedding import EmbeddingRequest
from typing import List

# 커스텀 예외 임포트
from ..core.exceptions import (
    EmbeddingCreationError,
    EmbeddingDeletionError,
    TravelDocumentNotFoundError,
    DatabaseConnectionError,
    InvalidInputError,
    ContentTooLongError,
    NoRelevantDocumentsFoundError,
    LLMServiceError,
)

MAX_CONTENT_LENGTH_FOR_EMBEDDING = 100 # 글자수 기준으로 임의 설정. 실제로는 토크나이저로 정확한 토큰 수 계산 권장.

async def create_embedding(request: EmbeddingRequest, db_collection):
    """
    요청 데이터를 바탕으로 Document를 생성하고 DB에 저장합니다.
    (ContentTooLongError 적용)
    """
    if not request.content: # ""
        raise InvalidInputError(detail="임베딩 할 사용자 답변(Content)이 비어 있습니다.")


    # ContentTooLongError
    if len(request.content) > MAX_CONTENT_LENGTH_FOR_EMBEDDING:
        raise ContentTooLongError(detail = f"입력 내용(content)이 {MAX_CONTENT_LENGTH_FOR_EMBEDDING}자를 초과합니다.")

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
        db_collection.add_documents([document])
        print(f"Document with id = '{request.pk}' added to collection")

    except APIConnectionError as e:
        raise DatabaseConnectionError(detail = f"ChromaDB 연결 오류: {e}") from e

    except OpenAIError as e:
        raise EmbeddingCreationError(detail = f"OpenAI 임베딩 API 오류 (배치): {e}") from e

    except Exception as e:
        # 그 외 모든 예상치 못한 오류를 캡슐화
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

        # ContentTooLongError
        if len(request.content) > MAX_CONTENT_LENGTH_FOR_EMBEDDING:
            raise ContentTooLongError(detail = f"PK {request.pk}의 입력 내용(content)이 {MAX_CONTENT_LENGTH_FOR_EMBEDDING}자를 초과합니다.")

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
            db_collection.add_documents(documents)
            print(f"{len(documents)} documents added to collection")

    except APIConnectionError as e:
        raise DatabaseConnectionError(detail = f"ChromaDB 연결 오류: {e}") from e

    except OpenAIError as e:
        raise EmbeddingCreationError(detail = f"OpenAI 임베딩 API 오류 (배치): {e}") from e

    except Exception as e:
        raise EmbeddingCreationError(detail = f"사용자 답변 배치 임베딩 생성 중 알 수 없는 오류 발생 : {e}") from e

async def delete_embedding(pk: int, db_collection):
    """
    주어진 pk(id)에 해당하는 문서를 DB에서 삭제합니다.
    """
    str_pk = str(pk)

    try:
        results = db_collection.get(ids=[str_pk])

        # results가 비어있거나 'ids' 필드가 없거나, 해당 str_pk가 'ids' 리스트에 없으면 문서를 찾을 수 없음
        if not results or not results.get('ids') or str_pk not in results['ids']:
            raise TravelDocumentNotFoundError(document_id=pk, detail = "삭제할 사용자 답변 키워드 데이터를 찾을 수 없습니다.")

        # 문서가 존재하면 삭제 진행
        await db_collection.adelete(ids=[str_pk])
        print(f"Document with id='{pk}' deleted from collection.")

        # 특정 ChromaDB 예외를 잡아서 처리
    except APIConnectionError as e:
        raise DatabaseConnectionError(detail=f"ChromaDB 연결 오류: {e}") from e

    except TravelDocumentNotFoundError as e: # 찾지 못한 예외는 그대로 다시 발생
        raise e

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

    except APIConnectionError as e:
        raise DatabaseConnectionError(detail=f"ChromaDB 연결 오류: {e}") from e

    except InvalidInputError as e:
        raise e

    except Exception as e:
        raise EmbeddingDeletionError(detail=f"사용자 답변 배치 임베딩 삭제 중 알 수 없는 오류: {e}") from e


# # 이 함수는 app/routers/embedding.py에 라우터도 추가
# def query_embeddings(query_text: str, domain: str, db_collection, k: int = 4):
#     """
#     쿼리 텍스트를 이용해 벡터 DB에서 유사한 문서를 검색
#     (NoRelevantDocumentsFoundError 적용, 사용자가 원하는 내용이 벡터 DB에 없을 때 발생)
#     """
#
#     if not query_text:
#         raise InvalidInputError(detail = "검색 쿼리가 비어 있습니다.")
#
#     try:
#         # 실제 ChromaDB 검색 호출
#         result_with_scores = db_collection.similarity_search_with_score(query = query_text, k = k)
#
#         # NoRelevantDocumentsFoundError
#         if not result_with_scores:
#             raise NoRelevantDocumentsFoundError(detail = f"'{query_text}' 쿼리에 대한 관련 문서를 찾을 수 없습니다.")
#
#         # 검색 결과를 필요한 형식으로 가공하여 반환
#         return [{"content": doc.page_content, "metadata": doc.metadata, "score": score} for doc, score in result_with_scores]
#
#     except APIConnectionError as e:
#         raise DatabaseConnectionError(detail = f"ChromaDB 연결 오류 (검색): {e}") from e
#
#     except OpenAIError as e: # 임베딩 모델을 통한 쿼리 임베딩 중 발생 가능성
#         raise EmbeddingCreationError(detail = f"검색 쿼리 임베딩 중 오류 발생: {e}") from e
#
#     except Exception as e:
#         raise LLMServiceError(detail = f"문서 검색 중 알 수 없는 오류 발생: {e}") from e