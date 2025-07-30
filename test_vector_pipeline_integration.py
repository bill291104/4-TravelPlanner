#!/usr/bin/env python3
"""
벡터 DB → 자바 RDB 검색 파이프라인 통합 테스트 스크립트
"""

import asyncio
import json
import sys
import os
import time
from typing import Dict, Any, List

# FastAPI 클라이언트 import
import httpx

# 프로젝트 경로 추가
sys.path.append(os.path.join(os.path.dirname(__file__), 'python-fastapi-server'))

try:
    from python_fastapi_server.app.services.vector_ss_service import (
        similarity_search, 
        send_ids_to_java, 
        vector_to_rdb_search
    )
    from python_fastapi_server.app.schemas.vector_ss import SimilaritySearchRequest, Domains
except ImportError as e:
    print(f"⚠️ FastAPI 모듈 import 실패: {e}")
    print("Python 환경에서 직접 테스트를 진행합니다.")


class VectorPipelineIntegrationTest:
    """벡터 DB → 자바 RDB 파이프라인 통합 테스트 클래스"""
    
    def __init__(self):
        self.fastapi_url = "http://localhost:8000"
        self.java_url = "http://localhost:8080"
        self.test_results = []
    
    async def test_java_api_direct(self) -> bool:
        """자바 API 직접 테스트"""
        print("\n🔧 1. 자바 API 직접 테스트...")
        
        test_payload = {
            "domain": "place",
            "ids": [10, 31]
        }
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.java_url}/api/vector/search-by-ids",
                    json=test_payload,
                    timeout=10.0
                )
                response.raise_for_status()
                
                result = response.json()
                print(f"   ✅ 자바 API 응답: {result}")
                self.test_results.append({
                    "test": "java_api_direct",
                    "status": "success",
                    "data": result
                })
                return True
                
        except httpx.ConnectError:
            print("   ❌ 자바 서버가 실행되지 않았습니다 (localhost:8080)")
            self.test_results.append({
                "test": "java_api_direct",
                "status": "failed",
                "error": "Java server not running"
            })
            return False
        except Exception as e:
            print(f"   ❌ 자바 API 테스트 실패: {e}")
            self.test_results.append({
                "test": "java_api_direct",
                "status": "failed",
                "error": str(e)
            })
            return False
    
    async def test_fastapi_vector_search(self) -> bool:
        """FastAPI 벡터 검색 테스트"""
        print("\n🔍 2. FastAPI 벡터 검색 테스트...")
        
        test_payload = {
            "target_domain": "place",
            "target_keywords": ["경주", "숲"]
        }
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.fastapi_url}/api/vector/similarity-search",
                    json=test_payload,
                    timeout=30.0
                )
                response.raise_for_status()
                
                result = response.json()
                print(f"   ✅ FastAPI 벡터 검색 응답: {result}")
                self.test_results.append({
                    "test": "fastapi_vector_search",
                    "status": "success",
                    "data": result
                })
                return True
                
        except httpx.ConnectError:
            print("   ❌ FastAPI 서버가 실행되지 않았습니다 (localhost:8000)")
            self.test_results.append({
                "test": "fastapi_vector_search",
                "status": "failed",
                "error": "FastAPI server not running"
            })
            return False
        except Exception as e:
            print(f"   ❌ FastAPI 벡터 검색 실패: {e}")
            self.test_results.append({
                "test": "fastapi_vector_search",
                "status": "failed",
                "error": str(e)
            })
            return False
    
    async def test_full_pipeline_via_api(self) -> bool:
        """전체 파이프라인 API 테스트"""
        print("\n🚀 3. 전체 파이프라인 API 테스트...")
        
        test_payload = {
            "target_domain": "place",
            "target_keywords": ["경주", "대릉원"]
        }
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.fastapi_url}/api/vector/vector-to-rdb-search",
                    json=test_payload,
                    timeout=60.0
                )
                response.raise_for_status()
                
                result = response.json()
                print(f"   ✅ 전체 파이프라인 응답: {result}")
                self.test_results.append({
                    "test": "full_pipeline_api",
                    "status": "success",
                    "data": result
                })
                return True
                
        except httpx.ConnectError:
            print("   ❌ 서버 연결 실패")
            self.test_results.append({
                "test": "full_pipeline_api",
                "status": "failed",
                "error": "Server connection failed"
            })
            return False
        except Exception as e:
            print(f"   ❌ 전체 파이프라인 테스트 실패: {e}")
            self.test_results.append({
                "test": "full_pipeline_api",
                "status": "failed",
                "error": str(e)
            })
            return False
    
    async def test_direct_function_call(self) -> bool:
        """함수 직접 호출 테스트 (서버 없이)"""
        print("\n⚙️ 4. 함수 직접 호출 테스트...")
        
        try:
            # SimilaritySearchRequest 생성
            request = SimilaritySearchRequest(
                target_domain=Domains.PLACE,
                target_keywords=["경주", "숲"]
            )
            
            # 벡터 검색만 테스트 (자바 연결 없이)
            print("   4-1. 벡터 DB 유사도 검색...")
            ids = await similarity_search(request)
            print(f"   추출된 ID 리스트: {ids}")
            
            if ids:
                print("   4-2. 자바 API 전송 테스트...")
                try:
                    # 자바 서버 연결 테스트
                    rdb_result = await send_ids_to_java("place", ids)
                    print(f"   자바 API 응답: {rdb_result}")
                    
                    self.test_results.append({
                        "test": "direct_function_call",
                        "status": "success",
                        "vector_ids": ids,
                        "rdb_result": rdb_result
                    })
                    return True
                    
                except Exception as java_error:
                    print(f"   ⚠️ 자바 연결 실패 (예상됨): {java_error}")
                    self.test_results.append({
                        "test": "direct_function_call",
                        "status": "partial_success",
                        "vector_ids": ids,
                        "java_error": str(java_error)
                    })
                    return True  # 벡터 검색은 성공했으므로 부분 성공
            else:
                print("   ❌ 벡터 검색에서 결과가 없습니다")
                self.test_results.append({
                    "test": "direct_function_call",
                    "status": "failed",
                    "error": "No vector search results"
                })
                return False
                
        except Exception as e:
            print(f"   ❌ 함수 직접 호출 실패: {e}")
            self.test_results.append({
                "test": "direct_function_call",
                "status": "failed",
                "error": str(e)
            })
            return False
    
    async def test_performance(self) -> Dict[str, Any]:
        """성능 테스트"""
        print("\n⏱️ 5. 성능 테스트...")
        
        test_cases = [
            {"domain": "place", "keywords": ["경주"]},
            {"domain": "place", "keywords": ["부산", "해운대"]},
            {"domain": "restaurant", "keywords": ["맛집"]},
        ]
        
        performance_results = []
        
        for i, test_case in enumerate(test_cases):
            try:
                start_time = time.time()
                
                # 벡터 검색 시간 측정
                if 'similarity_search' in globals():
                    request = SimilaritySearchRequest(
                        target_domain=getattr(Domains, test_case["domain"].upper()),
                        target_keywords=test_case["keywords"]
                    )
                    ids = await similarity_search(request)
                    search_time = time.time() - start_time
                    
                    print(f"   테스트 {i+1}: {test_case} -> {len(ids)}개 ID, {search_time:.3f}초")
                    performance_results.append({
                        "test_case": test_case,
                        "result_count": len(ids),
                        "time_seconds": search_time
                    })
                else:
                    print(f"   테스트 {i+1}: 함수를 사용할 수 없음 (import 실패)")
                    
            except Exception as e:
                print(f"   테스트 {i+1} 실패: {e}")
                performance_results.append({
                    "test_case": test_case,
                    "error": str(e)
                })
        
        return performance_results
    
    def print_summary(self):
        """테스트 결과 요약 출력"""
        print("\n" + "="*60)
        print("🎯 통합 테스트 결과 요약")
        print("="*60)
        
        success_count = sum(1 for result in self.test_results if result["status"] == "success")
        partial_count = sum(1 for result in self.test_results if result["status"] == "partial_success")
        total_count = len(self.test_results)
        
        print(f"✅ 성공: {success_count}/{total_count}")
        print(f"⚠️ 부분 성공: {partial_count}/{total_count}")
        print(f"❌ 실패: {total_count - success_count - partial_count}/{total_count}")
        
        for result in self.test_results:
            status_icon = "✅" if result["status"] == "success" else "⚠️" if result["status"] == "partial_success" else "❌"
            print(f"{status_icon} {result['test']}: {result['status']}")
            if "error" in result:
                print(f"    오류: {result['error']}")
    
    async def run_all_tests(self):
        """모든 테스트 실행"""
        print("🧪 벡터 DB → 자바 RDB 파이프라인 통합 테스트 시작")
        print("="*60)
        
        # 1. 자바 API 직접 테스트
        await self.test_java_api_direct()
        
        # 2. FastAPI 벡터 검색 테스트
        await self.test_fastapi_vector_search()
        
        # 3. 전체 파이프라인 API 테스트
        await self.test_full_pipeline_via_api()
        
        # 4. 함수 직접 호출 테스트
        if 'similarity_search' in globals():
            await self.test_direct_function_call()
        else:
            print("\n⚙️ 4. 함수 직접 호출 테스트 - SKIP (import 실패)")
        
        # 5. 성능 테스트
        performance_results = await self.test_performance()
        
        # 결과 요약
        self.print_summary()
        
        # 결과를 파일로 저장
        test_result_file = "test_results.json"
        with open(test_result_file, 'w', encoding='utf-8') as f:
            json.dump({
                "test_results": self.test_results,
                "performance_results": performance_results,
                "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
            }, f, ensure_ascii=False, indent=2)
        
        print(f"\n📋 상세 결과가 {test_result_file}에 저장되었습니다.")


async def main():
    """메인 실행 함수"""
    tester = VectorPipelineIntegrationTest()
    await tester.run_all_tests()


if __name__ == "__main__":
    print("사용법:")
    print("1. 자바 서버 실행: ./mvnw spring-boot:run")
    print("2. FastAPI 서버 실행: cd python-fastapi-server && uvicorn app.main:app --reload")
    print("3. 이 스크립트 실행: python test_vector_pipeline_integration.py")
    print()
    
    asyncio.run(main())