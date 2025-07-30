package com.fastcampus.toyproject4_team4;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VectorSearchController.class)
class VectorSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VectorSearchService vectorSearchService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSearchByIds_Success() throws Exception {
        // Given
        VectorSearchRequest request = new VectorSearchRequest("place", Arrays.asList(10, 31));
        
        List<Map<String, Object>> mockData = new ArrayList<>();
        Map<String, Object> place1 = new HashMap<>();
        place1.put("id", 10);
        place1.put("name", "경주 대릉원");
        place1.put("region", "경주");
        mockData.add(place1);
        
        VectorSearchResponse mockResponse = new VectorSearchResponse(
                "place 도메인에서 1개 레코드 조회 완료", 
                mockData
        );
        
        when(vectorSearchService.searchByIds(any(VectorSearchRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/vector/search-by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("place 도메인에서 1개 레코드 조회 완료"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("경주 대릉원"))
                .andExpect(jsonPath("$.data[0].region").value("경주"));
    }

    @Test
    void testSearchByIds_EmptyResult() throws Exception {
        // Given
        VectorSearchRequest request = new VectorSearchRequest("place", Arrays.asList(999));
        VectorSearchResponse mockResponse = new VectorSearchResponse(
                "place 도메인에서 0개 레코드 조회 완료", 
                new ArrayList<>()
        );
        
        when(vectorSearchService.searchByIds(any(VectorSearchRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/vector/search-by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("place 도메인에서 0개 레코드 조회 완료"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testSearchByIds_ServiceException() throws Exception {
        // Given
        VectorSearchRequest request = new VectorSearchRequest("invalid", Arrays.asList(10));
        
        when(vectorSearchService.searchByIds(any(VectorSearchRequest.class)))
                .thenThrow(new RuntimeException("지원하지 않는 도메인입니다"));

        // When & Then
        mockMvc.perform(post("/api/vector/search-by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("검색 중 오류 발생: 지원하지 않는 도메인입니다"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testSearchByIds_InvalidRequestFormat() throws Exception {
        // Given - 잘못된 JSON 형식
        String invalidJson = "{\"domain\": \"place\", \"ids\": \"not_an_array\"}";

        // When & Then
        mockMvc.perform(post("/api/vector/search-by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByIds_MissingFields() throws Exception {
        // Given - 필수 필드 누락
        String incompleteJson = "{\"domain\": \"place\"}";

        // When & Then
        mockMvc.perform(post("/api/vector/search-by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteJson))
                .andExpect(status().isOk()) // null ids는 서비스에서 처리됨
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testSearchByIds_MultipleResults() throws Exception {
        // Given
        VectorSearchRequest request = new VectorSearchRequest("restaurant", Arrays.asList(1, 2, 3));
        
        List<Map<String, Object>> mockData = new ArrayList<>();
        
        Map<String, Object> restaurant1 = new HashMap<>();
        restaurant1.put("id", 1);
        restaurant1.put("name", "한식당");
        restaurant1.put("cuisine_type", "korean");
        mockData.add(restaurant1);
        
        Map<String, Object> restaurant2 = new HashMap<>();
        restaurant2.put("id", 2);
        restaurant2.put("name", "일식당");
        restaurant2.put("cuisine_type", "japanese");
        mockData.add(restaurant2);
        
        VectorSearchResponse mockResponse = new VectorSearchResponse(
                "restaurant 도메인에서 2개 레코드 조회 완료", 
                mockData
        );
        
        when(vectorSearchService.searchByIds(any(VectorSearchRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/vector/search-by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("restaurant 도메인에서 2개 레코드 조회 완료"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("한식당"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("일식당"));
    }
}