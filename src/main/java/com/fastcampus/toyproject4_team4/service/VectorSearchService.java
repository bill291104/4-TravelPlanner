package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.dto.VectorSearchRequest;
import com.fastcampus.toyproject4_team4.dto.VectorSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 파이썬에서 전송된 ID 리스트로 해당 도메인의 RDB 테이블에서 데이터 조회
     */
    public VectorSearchResponse searchByIds(VectorSearchRequest request) {
        String domain = request.getDomain();
        List<Integer> ids = request.getIds();
        
        if (ids == null || ids.isEmpty()) {
            return new VectorSearchResponse("검색할 ID가 없습니다", List.of());
        }
        
        try {
            // 도메인에 따른 테이블명 결정
            String tableName = getTableNameByDomain(domain);
            
            // IN 절을 위한 플레이스홀더 생성
            String placeholders = ids.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(", "));
            
            // SQL 쿼리 생성
            String sql = String.format("SELECT * FROM %s WHERE id IN (%s)", tableName, placeholders);
            
            // 쿼리 실행
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, ids.toArray());
            
            String message = String.format("%s 도메인에서 %d개 레코드 조회 완료", domain, results.size());
            return new VectorSearchResponse(message, results);
            
        } catch (Exception e) {
            throw new RuntimeException("데이터베이스 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    /**
     * 도메인명에 따른 RDB 테이블명 매핑
     */
    private String getTableNameByDomain(String domain) {
        switch (domain.toLowerCase()) {
            case "place":
                return "places";
            case "restaurant":
                return "restaurants";
            case "accom":
                return "accommodations";
            case "place_review":
                return "place_reviews";
            case "restaurant_review":
                return "restaurant_reviews";
            case "accom_review":
                return "accommodation_reviews";
            case "travel_style":
                return "travel_styles";
            case "travel_trend":
                return "travel_trends";
            default:
                throw new IllegalArgumentException("지원하지 않는 도메인입니다: " + domain);
        }
    }
}