# test_chromadb_error.py
import chromadb
from chromadb.utils import embedding_functions

# 인메모리 클라이언트 (테스트용)
client = chromadb.Client()
collection_name = "test_collection"

# 기존 컬렉션 삭제 (테스트를 위해)
try:
    client.delete_collection(name=collection_name)
except Exception:
    pass # 없으면 무시

# 컬렉션 생성
collection = client.create_collection(name=collection_name)

# 첫 번째 문서 추가
collection.add(
    documents=["This is a document"],
    metadatas=[{"source": "my_source"}],
    ids=["doc1"]
)
print("First document added.")

try:
    # 동일한 ID로 문서 다시 추가 (중복 시 어떤 예외가 발생하는지 확인)
    collection.add(
        documents=["This is another document"],
        metadatas=[{"source": "another_source"}],
        ids=["doc1"]
    )
    print("Second document added with same ID (might be updated).")
except Exception as e:
    print(f"Error when adding duplicate ID: {type(e).__name__}: {e}")

# ID가 존재하지 않을 때 삭제를 시도했을 때
try:
    collection.delete(ids=["non_existent_id"])
    print("Deleted non-existent ID (might not raise error).")
except Exception as e:
    print(f"Error when deleting non-existent ID: {type(e).__name__}: {e}")

# ID 중복 시 에러를 강제하려면 update_or_add 옵션을 사용해야 할 수 있습니다.
# 또는 컬렉션 생성 시 unique_id=True 같은 옵션을 제공하는지 확인해야 합니다.