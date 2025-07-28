#!/usr/bin/env python3
"""
FastAPI 테스트 엔드포인트 간편 실행 스크립트
"""

import asyncio
import json
import httpx
from typing import Dict, Any

class EndpointTester:
    """FastAPI 엔드포인트 테스트 클래스"""
    
    def __init__(self):
        self.base_url = "http://localhost:8000"
        
    async def test_vector_search_only(self):
        """벡터 DB 검색만 테스트"""
        print("🔍 1. 벡터 DB 유사도 검색 테스트")
        
        payload = {
            "target_domain": "place",
            "target_keywords": ["경주", "숲"]
        }
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.base_url}/vector_ss/pks",
                    json=payload,
                    timeout=30.0
                )
                response.raise_for_status()
                
                result = response.json()
                print(f"   ✅ 응답: {result}")
                print(f"   추출된 ID 개수: {len(result)}")
                return result
                
        except Exception as e:
            print(f"   ❌ 실패: {e}")
            return None
    
    async def test_java_connection(self):
        """자바 API 연결 테스트"""
        print("\n🔗 2. 자바 API 연결 테스트")
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.base_url}/vector_ss/test-java-connection",
                    timeout=10.0
                )
                response.raise_for_status()
                
                result = response.json()
                print(f"   상태: {result['status']}")
                print(f"   메시지: {result['message']}")
                if result.get('test_result'):
                    print(f"   테스트 결과: {result['test_result']}")
                return result
                
        except Exception as e:
            print(f"   ❌ 실패: {e}")
            return None
    
    async def test_full_pipeline(self):
        """전체 파이프라인 테스트"""
        print("\n🚀 3. 전체 파이프라인 테스트")
        
        payload = {
            "target_domain": "place",
            "target_keywords": ["경주", "대릉원"]
        }
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.base_url}/vector_ss/vector-to-rdb-search",
                    json=payload,
                    timeout=60.0
                )
                response.raise_for_status()
                
                result = response.json()
                print(f"   ✅ 응답: {result}")
                if 'data' in result:
                    print(f"   조회된 레코드 수: {len(result['data'])}")
                    for i, record in enumerate(result['data'][:3]):  # 처음 3개만 출력
                        print(f"   레코드 {i+1}: {record}")
                return result
                
        except Exception as e:
            print(f"   ❌ 실패: {e}")
            return None
    
    async def test_different_domains(self):
        """다양한 도메인 테스트"""
        print("\n📊 4. 다양한 도메인 테스트")
        
        test_cases = [
            {"domain": "place", "keywords": ["경주"]},
            {"domain": "restaurant", "keywords": ["맛집"]},
            {"domain": "accom", "keywords": ["호텔"]},
        ]
        
        for i, test_case in enumerate(test_cases):
            print(f"\n   4-{i+1}. {test_case['domain']} 도메인 테스트")
            
            payload = {
                "target_domain": test_case["domain"],
                "target_keywords": test_case["keywords"]
            }
            
            try:
                async with httpx.AsyncClient() as client:
                    response = await client.post(
                        f"{self.base_url}/vector_ss/pks",
                        json=payload,
                        timeout=30.0
                    )
                    response.raise_for_status()
                    
                    result = response.json()
                    print(f"      결과: {len(result)}개 ID - {result}")
                    
            except Exception as e:
                print(f"      ❌ 실패: {e}")
    
    async def run_all_tests(self):
        """모든 테스트 실행"""
        print("🧪 FastAPI 엔드포인트 테스트 시작")
        print("="*50)
        
        # 1. 벡터 검색만
        await self.test_vector_search_only()
        
        # 2. 자바 연결 테스트
        await self.test_java_connection()
        
        # 3. 전체 파이프라인
        await self.test_full_pipeline()
        
        # 4. 다양한 도메인
        await self.test_different_domains()
        
        print("\n✅ 모든 테스트 완료")


async def main():
    """메인 실행 함수"""
    print("FastAPI 서버가 localhost:8000에서 실행 중인지 확인하세요.")
    print("실행 명령: cd python-fastapi-server && uvicorn app.main:app --reload")
    print()
    
    tester = EndpointTester()
    await tester.run_all_tests()


if __name__ == "__main__":
    asyncio.run(main())