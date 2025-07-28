package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.dto.VectorSearchRequest;
import com.fastcampus.toyproject4_team4.dto.VectorSearchResponse;
import com.fastcampus.toyproject4_team4.service.VectorSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vector")
@CrossOrigin(origins = "*")
public class VectorSearchController {
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    /**
     * 파이썬에서 전송된 ID 리스트로 RDB에서 데이터 조회
     * @param request 도메인과 ID 리스트를 포함한 요청
     * @return 매칭되는 RDB 레코드들
     */
    @PostMapping("/search-by-ids")
    public ResponseEntity<VectorSearchResponse> searchByIds(@RequestBody VectorSearchRequest request) {
        try {
            VectorSearchResponse response = vectorSearchService.searchByIds(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            VectorSearchResponse errorResponse = new VectorSearchResponse();
            errorResponse.setMessage("검색 중 오류 발생: " + e.getMessage());
            errorResponse.setData(List.of());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}