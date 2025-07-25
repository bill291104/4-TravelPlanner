import asyncio
import pytest
from unittest.mock import Mock, AsyncMock, patch
from typing import List
from pydantic import BaseModel

# 테스트를 위한 스키마 정의 (실제 스키마가 없으므로 임시로 생성)
class SimilaritySearchRequest(BaseModel):
    context: str
    prompt_template: str
    target_keywords: List[str]

class Domains(BaseModel):
    name: str
    type: str

# 테스트할 함수 (제공된 코드를 약간 수정하여 임포트 가능하게 함)
async def main_domain_search(request: SimilaritySearchRequest) -> List[str]:
    """
    핵심 도메인 컬렉션에서 유사도 검색을 수행합니다.
    """
    from langchain_core.prompts import ChatPromptTemplate
    from chromadb import Client

    # 1. 프롬프트 템플릿
    template = ChatPromptTemplate.from_template(request.prompt_template)
    main_keywords = request.target_keywords

    # 2. 프롬프트 메세지
    prompt = template.format_messages(
        context=request.context,
        domains=",".join(main_keywords)
    )

    # 3. LLM모델 (모킹을 위해 간단히 처리)
    # llm = get_llm(temperature=0.1)

    # 벡터 DB 설정
    vector_db = Client()
    main_collection = vector_db.get_collection("travel_main_domain")

    # Tool 1 유사도 검사 결과 +Tool2로 전달할 목록
    main_domain_pks = []

    try:
        # 모킹된 LLM 응답 (실제로는 llm.ainvoke(prompt) 호출)
        mock_response_content = "여행, 숙박, 관광지, 맛집"
        extracted_keywords = [keyword.strip() for keyword in mock_response_content.split(',') if keyword.strip()]

        # 각 키워드별 유사도 검색을 수행
        for main_keyword in extracted_keywords:
            # 벡터 유사도 검색 실행
            result = main_collection.query(
                query_texts=[main_keyword],
                where={"domain_type": "main"},
                n_results=3
            )

            # 안전한 결과 추출
            if result['ids'] and result['ids'][0]:
                found_ids = result['ids'][0]
                main_domain_pks.extend(found_ids)
                print(f"키워드'{main_keyword}' 유사도 검색 결과:{found_ids}")

        # 중복 제거
        unique_domain_pks = list(set(main_domain_pks))
        print(f"Tool1 최종 결과 : {unique_domain_pks}")
        return unique_domain_pks

    except Exception as e:
        print(f"핵심 도메인 검색 중 오류 발생: {e}")
        raise Exception(f"LLM API 호출 실패 또는 응답 형식 오류: {str(e)}")


# 테스트 함수들
class TestMainDomainSearch:
    """main_domain_search 함수를 테스트하는 클래스"""

    @pytest.fixture
    def sample_request(self):
        """테스트용 샘플 요청 데이터"""
        return SimilaritySearchRequest(
            context="부산 여행 계획을 세우고 싶습니다. 3박 4일 일정으로 맛집과 관광지를 중심으로 계획해주세요.",
            prompt_template="다음 컨텍스트에서 여행 관련 핵심 도메인을 추출해주세요: {context}. 대상 도메인: {domains}",
            target_keywords=["여행", "숙박", "관광", "음식"]
        )

    @pytest.fixture
    def mock_vector_db_response(self):
        """벡터 DB 응답 모킹 데이터"""
        return {
            'ids': [['travel_001', 'travel_002', 'travel_003']],
            'distances': [[0.1, 0.2, 0.3]],
            'metadatas': [[
                {'domain_type': 'main', 'name': '여행'},
                {'domain_type': 'main', 'name': '관광'},
                {'domain_type': 'main', 'name': '여행지'}
            ]]
        }

    @patch('chromadb.Client')
    async def test_main_domain_search_success(self, mock_client_class, sample_request, mock_vector_db_response):
        """정상적인 경우의 테스트"""
        # Mock 설정
        mock_client = Mock()
        mock_collection = Mock()
        mock_client.get_collection.return_value = mock_collection
        mock_collection.query.return_value = mock_vector_db_response
        mock_client_class.return_value = mock_client

        # 함수 실행
        result = await main_domain_search(sample_request)

        # 검증
        assert isinstance(result, list)
        assert len(result) > 0
        assert all(isinstance(item, str) for item in result)

        # Mock 호출 검증
        mock_client.get_collection.assert_called_once_with("travel_main_domain")
        assert mock_collection.query.call_count > 0

    @patch('chromadb.Client')
    async def test_main_domain_search_empty_result(self, mock_client_class, sample_request):
        """빈 결과가 반환되는 경우의 테스트"""
        # Mock 설정 - 빈 결과
        mock_client = Mock()
        mock_collection = Mock()
        mock_client.get_collection.return_value = mock_collection
        mock_collection.query.return_value = {'ids': [[]], 'distances': [[]], 'metadatas': [[]]}
        mock_client_class.return_value = mock_client

        # 함수 실행
        result = await main_domain_search(sample_request)

        # 검증
        assert isinstance(result, list)
        assert len(result) == 0

    @patch('chromadb.Client')
    async def test_main_domain_search_exception(self, mock_client_class, sample_request):
        """예외가 발생하는 경우의 테스트"""
        # Mock 설정 - 예외 발생
        mock_client = Mock()
        mock_collection = Mock()
        mock_client.get_collection.return_value = mock_collection
        mock_collection.query.side_effect = Exception("벡터 DB 연결 실패")
        mock_client_class.return_value = mock_client

        # 함수 실행 및 예외 검증
        with pytest.raises(Exception) as exc_info:
            await main_domain_search(sample_request)

        assert "LLM API 호출 실패 또는 응답 형식 오류" in str(exc_info.value)


# 간단한 실행 테스트 함수 (pytest 없이도 실행 가능)
async def simple_test():
    """간단한 테스트 실행 함수"""
    print("=== 간단한 테스트 시작 ===")

    # 테스트 데이터 준비
    test_request = SimilaritySearchRequest(
        context="서울 여행 계획을 세우고 싶습니다. 2박 3일 일정으로 쇼핑과 문화체험을 중심으로 계획해주세요.",
        prompt_template="다음 컨텍스트에서 여행 관련 핵심 도메인을 추출해주세요: {context}. 대상 도메인: {domains}",
        target_keywords=["여행", "쇼핑", "문화", "체험"]
    )

    try:
        # Mock을 사용한 테스트
        with patch('chromadb.Client') as mock_client_class:
            # Mock 설정
            mock_client = Mock()
            mock_collection = Mock()
            mock_client.get_collection.return_value = mock_collection
            mock_collection.query.return_value = {
                'ids': [['shopping_001', 'culture_002', 'travel_003']],
                'distances': [[0.1, 0.15, 0.2]],
                'metadatas': [[
                    {'domain_type': 'main', 'name': '쇼핑'},
                    {'domain_type': 'main', 'name': '문화'},
                    {'domain_type': 'main', 'name': '여행'}
                ]]
            }
            mock_client_class.return_value = mock_client

            # 함수 실행
            result = await main_domain_search(test_request)

            print(f"테스트 결과: {result}")
            print(f"결과 타입: {type(result)}")
            print(f"결과 개수: {len(result)}")

            # 기본 검증
            assert isinstance(result, list), "결과는 리스트여야 합니다"
            print("✅ 테스트 성공!")

    except Exception as e:
        print(f"❌ 테스트 실패: {e}")
        raise


# 메인 실행 함수
async def run_all_tests():
    """모든 테스트를 실행하는 함수"""
    print("=== 벡터 유사도 검색 함수 테스트 시작 ===\n")

    # 간단한 테스트 실행
    await simple_test()

    print("\n=== 모든 테스트 완료 ===")
    print("\n추가 테스트를 위해서는 다음 명령어를 사용하세요:")
    print("pytest test_vector_ss.py -v")


if __name__ == "__main__":
    # 비동기 함수 실행
    asyncio.run(run_all_tests())
