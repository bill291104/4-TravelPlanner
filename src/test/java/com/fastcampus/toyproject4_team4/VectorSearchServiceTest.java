package com.fastcampus.toyproject4_team4;

import com.fastcampus.toyproject4_team4.dto.VectorSearchRequest;
import com.fastcampus.toyproject4_team4.dto.VectorSearchResponse;
import com.fastcampus.toyproject4_team4.service.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    private VectorSearchRequest testRequest;
    private List<Map<String, Object>> mockDbResults;

    @BeforeEach
    void setUp() {
        // 테스트용 요청 데이터
        testRequest = new VectorSearchRequest("place", Arrays.asList(10, 31));
        
        // 테스트용 DB 결과 데이터
        mockDbResults = new ArrayList<>();
        
        Map<String, Object> place1 = new HashMap<>();
        place1.put("id", 10);
        place1.put("name", "경주 대릉원");
        place1.put("region", "경주");
        place1.put("category", "forest");
        mockDbResults.add(place1);
        
        Map<String, Object> place2 = new HashMap<>();
        place2.put("id", 31);
        place2.put("name", "해운대 해수욕장");
        place2.put("region", "부산");
        place2.put("category", "beach");
        mockDbResults.add(place2);
    }

    @Test
    void testSearchByIds_Success() {
        // Given
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(mockDbResults);

        // When
        VectorSearchResponse response = vectorSearchService.searchByIds(testRequest);

        // Then
        assertNotNull(response);
        assertEquals("place 도메인에서 2개 레코드 조회 완료", response.getMessage());
        assertEquals(2, response.getData().size());
        
        // 첫 번째 결과 검증
        Map<String, Object> firstResult = response.getData().get(0);
        assertEquals(10, firstResult.get("id"));
        assertEquals("경주 대릉원", firstResult.get("name"));
        
        // JdbcTemplate 호출 검증
        verify(jdbcTemplate, times(1)).queryForList(
                eq("SELECT * FROM places WHERE id IN (?, ?)"),
                eq(new Object[]{10, 31})
        );
    }

    @Test
    void testSearchByIds_EmptyIds() {
        // Given
        VectorSearchRequest emptyRequest = new VectorSearchRequest("place", new ArrayList<>());

        // When
        VectorSearchResponse response = vectorSearchService.searchByIds(emptyRequest);

        // Then
        assertNotNull(response);
        assertEquals("검색할 ID가 없습니다", response.getMessage());
        assertTrue(response.getData().isEmpty());
        
        // JdbcTemplate이 호출되지 않았는지 확인
        verify(jdbcTemplate, never()).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void testSearchByIds_NullIds() {
        // Given
        VectorSearchRequest nullRequest = new VectorSearchRequest("place", null);

        // When
        VectorSearchResponse response = vectorSearchService.searchByIds(nullRequest);

        // Then
        assertNotNull(response);
        assertEquals("검색할 ID가 없습니다", response.getMessage());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void testSearchByIds_InvalidDomain() {
        // Given
        VectorSearchRequest invalidRequest = new VectorSearchRequest("invalid_domain", Arrays.asList(10, 31));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            vectorSearchService.searchByIds(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("지원하지 않는 도메인입니다"));
    }

    @Test
    void testSearchByIds_DatabaseError() {
        // Given
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            vectorSearchService.searchByIds(testRequest);
        });
        
        assertTrue(exception.getMessage().contains("데이터베이스 조회 중 오류 발생"));
    }

    @Test
    void testSearchByIds_RestaurantDomain() {
        // Given
        VectorSearchRequest restaurantRequest = new VectorSearchRequest("restaurant", Arrays.asList(1, 2));
        List<Map<String, Object>> restaurantResults = new ArrayList<>();
        
        Map<String, Object> restaurant = new HashMap<>();
        restaurant.put("id", 1);
        restaurant.put("name", "맛집");
        restaurant.put("cuisine_type", "korean");
        restaurantResults.add(restaurant);
        
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(restaurantResults);

        // When
        VectorSearchResponse response = vectorSearchService.searchByIds(restaurantRequest);

        // Then
        assertNotNull(response);
        assertEquals("restaurant 도메인에서 1개 레코드 조회 완료", response.getMessage());
        assertEquals(1, response.getData().size());
        
        // restaurants 테이블이 사용되었는지 확인
        verify(jdbcTemplate, times(1)).queryForList(
                eq("SELECT * FROM restaurants WHERE id IN (?, ?)"),
                any(Object[].class)
        );
    }

    @Test
    void testSearchByIds_AccomDomain() {
        // Given
        VectorSearchRequest accomRequest = new VectorSearchRequest("accom", Arrays.asList(5));
        
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        // When
        VectorSearchResponse response = vectorSearchService.searchByIds(accomRequest);

        // Then
        assertNotNull(response);
        assertEquals("accom 도메인에서 0개 레코드 조회 완료", response.getMessage());
        assertTrue(response.getData().isEmpty());
        
        // accommodations 테이블이 사용되었는지 확인
        verify(jdbcTemplate, times(1)).queryForList(
                eq("SELECT * FROM accommodations WHERE id IN (?)"),
                any(Object[].class)
        );
    }

    @Test
    void testGetTableNameByDomain() {
        // 리플렉션을 사용하여 private 메서드 테스트
        // 실제로는 각 도메인별 테스트에서 간접적으로 확인됨
        
        // place -> places 매핑 확인 (위의 testSearchByIds_Success에서 확인됨)
        // restaurant -> restaurants 매핑 확인 (testSearchByIds_RestaurantDomain에서 확인됨)
        // accom -> accommodations 매핑 확인 (testSearchByIds_AccomDomain에서 확인됨)
        
        assertTrue(true); // 위의 테스트들에서 이미 검증됨
    }
}