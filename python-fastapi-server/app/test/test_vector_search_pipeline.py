#!/usr/bin/env python3
"""
벡터 DB → 자바 RDB 검색 파이프라인 테스트
"""

import pytest
import asyncio
from unittest.mock import AsyncMock, patch, MagicMock
from typing import List

import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from services.vector_ss_service import (
    similarity_search, 
    send_ids_to_java, 
    vector_to_rdb_search
)
from schemas.vector_ss import SimilaritySearchRequest, Domains


class TestVectorSearchPipeline:
    """벡터 검색 파이프라인 단위 테스트"""
    
    @pytest.fixture
    def sample_request(self):
        """테스트용 검색 요청"""
        return SimilaritySearchRequest(
            target_domain=Domains.PLACE,
            target_keywords=["경주", "숲"]
        )
    
    @pytest.fixture
    def mock_documents(self):
        """테스트용 Document 목록"""
        from langchain.schema import Document
        
        return [
            Document(
                page_content="경주 대릉원의 고분 건축과 역사",
                metadata={"additionalProp1": "10", "region": "경주", "category": "forest"}
            ),
            Document(
                page_content="해운대 해수욕장의 모래 미끄럼틀",
                metadata={"additionalProp1": "31", "region": "부산", "category": "beach"}
            )
        ]
    
    @pytest.mark.asyncio
    async def test_similarity_search_success(self, sample_request, mock_documents):
        """벡터 DB 유사도 검색 성공 테스트"""
        with patch('services.vector_ss_service.main_domain_search') as mock_main, \
             patch('services.vector_ss_service.subdomain_filter') as mock_sub:
            
            mock_main.return_value = mock_documents
            mock_sub.return_value = mock_documents
            
            result = await similarity_search(sample_request)
            
            assert result == [10, 31]
            mock_main.assert_called_once_with(sample_request)
            mock_sub.assert_called_once_with(mock_documents, sample_request)
    
    @pytest.mark.asyncio
    async def test_similarity_search_no_results(self, sample_request):
        """벡터 DB 검색 결과 없음 테스트"""
        with patch('services.vector_ss_service.main_domain_search') as mock_main:
            mock_main.return_value = []
            
            result = await similarity_search(sample_request)
            
            assert result == []
    
    @pytest.mark.asyncio
    async def test_send_ids_to_java_success(self):
        """자바 API 전송 성공 테스트"""
        mock_response_data = {
            "message": "place 도메인에서 2개 레코드 조회 완료",
            "data": [
                {"id": 10, "name": "경주 대릉원", "region": "경주"},
                {"id": 31, "name": "해운대 해수욕장", "region": "부산"}
            ]
        }
        
        with patch('httpx.AsyncClient') as mock_client:
            mock_response = AsyncMock()
            mock_response.json.return_value = mock_response_data
            mock_response.raise_for_status = MagicMock()
            
            mock_client.return_value.__aenter__ = AsyncMock(return_value=mock_client.return_value)
            mock_client.return_value.__aexit__ = AsyncMock()
            mock_client.return_value.post = AsyncMock(return_value=mock_response)
            
            result = await send_ids_to_java("place", [10, 31])
            
            assert result == mock_response_data
            assert len(result["data"]) == 2
    
    @pytest.mark.asyncio
    async def test_send_ids_to_java_connection_error(self):
        """자바 API 연결 실패 테스트"""
        import httpx
        
        with patch('httpx.AsyncClient') as mock_client:
            mock_client.return_value.__aenter__ = AsyncMock()
            mock_client.return_value.__aexit__ = AsyncMock()
            mock_client.return_value.post = AsyncMock(side_effect=httpx.RequestError("Connection failed"))
            
            with pytest.raises(Exception, match="자바 백엔드 연결 실패"):
                await send_ids_to_java("place", [10, 31])
    
    @pytest.mark.asyncio
    async def test_vector_to_rdb_search_full_pipeline(self, sample_request):
        """전체 파이프라인 통합 테스트"""
        mock_java_response = {
            "message": "성공",
            "data": [{"id": 10, "name": "경주 대릉원"}]
        }
        
        with patch('services.vector_ss_service.similarity_search') as mock_similarity, \
             patch('services.vector_ss_service.send_ids_to_java') as mock_send:
            
            mock_similarity.return_value = [10, 31]
            mock_send.return_value = mock_java_response
            
            result = await vector_to_rdb_search(sample_request)
            
            assert result == mock_java_response
            mock_similarity.assert_called_once_with(sample_request)
            mock_send.assert_called_once_with("place", [10, 31])
    
    @pytest.mark.asyncio
    async def test_vector_to_rdb_search_no_vector_results(self, sample_request):
        """벡터 검색 결과 없을 때 파이프라인 테스트"""
        with patch('services.vector_ss_service.similarity_search') as mock_similarity:
            mock_similarity.return_value = []
            
            result = await vector_to_rdb_search(sample_request)
            
            assert result == {"message": "검색 결과가 없습니다", "data": []}


if __name__ == "__main__":
    # 개별 테스트 실행
    async def run_manual_test():
        """수동 테스트 실행"""
        print("🧪 벡터 검색 파이프라인 테스트 시작...")
        
        # 실제 요청 생성
        request = SimilaritySearchRequest(
            target_domain=Domains.PLACE,
            target_keywords=["경주", "숲"]
        )
        
        try:
            # 벡터 DB 검색만 테스트 (자바 서버 없이)
            print("1. 벡터 DB 유사도 검색 테스트...")
            ids = await similarity_search(request)
            print(f"   추출된 ID: {ids}")
            
            if ids:
                print("2. 자바 API 연결 테스트...")
                # 실제 자바 서버가 실행 중이면 테스트
                try:
                    result = await send_ids_to_java(request.target_domain.value, ids)
                    print(f"   자바 API 응답: {result}")
                except Exception as e:
                    print(f"   자바 API 연결 실패: {e}")
            
            print("✅ 테스트 완료")
            
        except Exception as e:
            print(f"❌ 테스트 실패: {e}")
    
    # asyncio.run(run_manual_test())