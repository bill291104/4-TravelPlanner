#!/usr/bin/env python3
"""
간단한 ChromaDB 데이터 확인 스크립트
"""

import os
import chromadb
from chromadb.config import Settings

def inspect_chromadb():
    """ChromaDB 데이터를 직접 확인합니다."""
    print("ChromaDB 데이터 직접 검사")
    print("=" * 50)
    
    # ChromaDB 클라이언트 생성
    persist_directory = os.path.join(os.path.dirname(__file__), 'vector_db')
    
    if not os.path.exists(persist_directory):
        print(f"벡터 DB 디렉토리가 존재하지 않습니다: {persist_directory}")
        return
    
    client = chromadb.PersistentClient(path=persist_directory)
    
    # 모든 컬렉션 목록 가져오기
    collections = client.list_collections()
    print(f"총 컬렉션 수: {len(collections)}")
    
    for collection in collections:
        print(f"\n=== {collection.name} 컬렉션 ===")
        
        try:
            # 컬렉션 가져오기
            coll = client.get_collection(collection.name)
            
            # 모든 데이터 가져오기
            all_data = coll.get()
            
            print(f"문서 수: {len(all_data['ids'])}")
            
            # 각 문서 검사
            for i, (doc_id, metadata, document) in enumerate(zip(
                all_data['ids'],
                all_data['metadatas'] if all_data['metadatas'] else [],
                all_data['documents'] if all_data['documents'] else []
            )):
                print(f"\n문서 {i+1}:")
                print(f"  ID: {doc_id}")
                print(f"  Metadata: {metadata}")
                print(f"  Content: {document[:100] if document else 'No content'}...")
                
                # additionalProp1 확인
                if not metadata or 'additionalProp1' not in metadata:
                    print(f"  ⚠️ additionalProp1 누락!")
                elif metadata.get('additionalProp1') is None:
                    print(f"  ⚠️ additionalProp1이 None!")
                    
        except Exception as e:
            print(f"오류: {e}")

def fix_missing_ids():
    """누락된 ID를 직접 수정합니다."""
    print("\n" + "=" * 50)
    print("ID 수정 시작")
    
    persist_directory = os.path.join(os.path.dirname(__file__), 'vector_db')
    client = chromadb.PersistentClient(path=persist_directory)
    
    # 메인 컬렉션들만 처리
    main_collections = ['place', 'restaurant', 'accom']
    
    for coll_name in main_collections:
        try:
            coll = client.get_collection(coll_name)
            all_data = coll.get()
            
            print(f"\n{coll_name} 컬렉션 수정 중...")
            
            updated_count = 0
            for i, (doc_id, metadata, document) in enumerate(zip(
                all_data['ids'],
                all_data['metadatas'] if all_data['metadatas'] else [],
                all_data['documents'] if all_data['documents'] else []
            )):
                if not metadata or metadata.get('additionalProp1') is None:
                    # 새 ID 생성
                    new_pk_id = str(1000 + i)
                    
                    if not metadata:
                        metadata = {}
                    
                    metadata['additionalProp1'] = new_pk_id
                    
                    # 업데이트
                    coll.update(
                        ids=[doc_id],
                        metadatas=[metadata]
                    )
                    
                    print(f"  문서 {i+1}: additionalProp1 = {new_pk_id} 설정")
                    updated_count += 1
            
            print(f"{coll_name}: {updated_count}개 문서 수정 완료")
            
        except Exception as e:
            print(f"{coll_name} 처리 중 오류: {e}")

if __name__ == "__main__":
    # 1. 데이터 확인
    inspect_chromadb()
    
    # 2. 수정 여부 확인
    response = input("\n누락된 ID를 수정하시겠습니까? (y/n): ")
    if response.lower() == 'y':
        fix_missing_ids()
        print("\n수정 완료! API를 다시 테스트해보세요.")
    else:
        print("수정하지 않았습니다.")