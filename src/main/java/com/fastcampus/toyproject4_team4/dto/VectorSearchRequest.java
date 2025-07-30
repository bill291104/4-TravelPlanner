package com.fastcampus.toyproject4_team4.dto;

import java.util.List;

public class VectorSearchRequest {
    private String domain;
    private List<Integer> ids;
    
    public VectorSearchRequest() {}
    
    public VectorSearchRequest(String domain, List<Integer> ids) {
        this.domain = domain;
        this.ids = ids;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public List<Integer> getIds() {
        return ids;
    }
    
    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }
    
    @Override
    public String toString() {
        return "VectorSearchRequest{" +
                "domain='" + domain + '\'' +
                ", ids=" + ids +
                '}';
    }
}