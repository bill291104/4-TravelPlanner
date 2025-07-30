#!/usr/bin/env python3
"""
ChromaDB 데이터 확인 및 수정 스크립트
"""

import os
import sys
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings

# FastAPI 앱의 패키지를 import하기 위해 경로 추가
sys.path.append(os.path.join(os.path.dirname(__file__), 'python-fastapi-server'))

try:
    from python_fastapi_server.app.core.config import settings
    from python_fastapi_server.app.core.constants import Domains
except ImportError:
    print("FastAPI 앱을 import할 수 없습니다. 직접 설정을 사용합니다.")
    # 직접 설정
    class Settings:
        OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    settings = Settings()
    
    from enum import Enum
    class Domains(Enum):
        PLACE = "place"
        RESTAURANT = "restaurant"
        ACCOM = "accom"
        PLACE_REVIEW = "place_review"
        RESTAURANT_REVIEW = "restaurant_review"
        ACCOM_REVIEW = "accom_review"
        TRAVEL_STYLE = "travel_style"
        TRAVEL_TREND = "travel_trend"

def initialize_collections():
    """ChromaDB 컬렉션들을 초기화합니다."""
    print("ChromaDB 컬렉션을 초기화하는 중...")
    
    embedding_function = OpenAIEmbeddings(
        model="text-embedding-3-large",
        api_key=settings.OPENAI_API_KEY
    )
    
    persist_directory = os.path.join(os.path.dirname(__file__), 'vector_db')
    
    collections = {}
    for domain in Domains:
        collection_name = domain.value
        collections[collection_name] = Chroma(
            collection_name=collection_name,
            embedding_function=embedding_function,
            persist_directory=persist_directory
        )
        print(f"  - Collection '{collection_name}' 초기화 완료")
    
    return collections

def inspect_collection(collection, collection_name):
    """컬렉션의 모든 데이터를 검사합니다."""
    print(f"\n=== {collection_name} 컬렉션 검사 ===")
    
    try:
        # 모든 문서 가져오기
        all_docs = collection.get()
        
        print(f"총 문서 수: {len(all_docs['ids'])}")
        
        for i, (doc_id, metadata, document) in enumerate(zip(
            all_docs['ids'], 
            all_docs['metadatas'], 
            all_docs['documents']
        )):
            print(f"\n문서 {i+1}:")
            print(f"  ChromaDB ID: {doc_id}")
            print(f"  Metadata: {metadata}")
            print(f"  Content: {document[:100]}...")
            
            # additionalProp1 확인
            if metadata and 'additionalProp1' not in metadata:
                print(f"  ⚠️ 경고: additionalProp1이 없습니다!")
            elif metadata and metadata.get('additionalProp1') is None:
                print(f"  ⚠️ 경고: additionalProp1이 None입니다!")
                
    except Exception as e:
        print(f"오류 발생: {e}")

def fix_missing_ids(collection, collection_name):
    """누락된 ID를 수정합니다."""
    print(f"\n=== {collection_name} 컬렉션 ID 수정 ===")
    
    try:
        all_docs = collection.get()
        
        updated_count = 0
        for i, (doc_id, metadata, document) in enumerate(zip(
            all_docs['ids'], 
            all_docs['metadatas'], 
            all_docs['documents']
        )):
            if not metadata or metadata.get('additionalProp1') is None:
                # 새로운 ID 생성 (예: 1000 + 인덱스)
                new_id = str(1000 + i)
                
                if not metadata:
                    metadata = {}
                
                metadata['additionalProp1'] = new_id
                
                # 문서 업데이트
                collection.update_document(
                    document_id=doc_id,
                    document=document,
                    metadata=metadata
                )
                
                print(f"  문서 {i+1}: ID {new_id} 할당")
                updated_count += 1
        
        print(f"총 {updated_count}개 문서의 ID를 수정했습니다.")
        
    except Exception as e:
        print(f"수정 중 오류 발생: {e}")

def main():
    """메인 함수"""
    print("ChromaDB 데이터 검사 및 수정 도구")
    print("=" * 50)
    
    # 컬렉션 초기화
    collections = initialize_collections()
    
    # 메인 도메인만 검사 (place, restaurant, accom)
    main_domains = ['place', 'restaurant', 'accom']
    
    for domain_name in main_domains:
        if domain_name in collections:
            collection = collections[domain_name]
            
            # 데이터 검사
            inspect_collection(collection, domain_name)
            
            # 수정 여부 묻기
            response = input(f"\n{domain_name} 컬렉션의 누락된 ID를 수정하시겠습니까? (y/n): ")
            if response.lower() == 'y':
                fix_missing_ids(collection, domain_name)
    
    print("\n작업 완료!")

if __name__ == "__main__":
    main()