package com.fastcampus.toyproject4_team4.client;

import com.fastcampus.toyproject4_team4.dto.TravelData;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AiClient {     //외부 api 호출 (repository가 db랑 연결된는 개념)

    private final RestTemplate restTemplate = new RestTemplate();
    private final String fastapiUrl = "http://localhost:8000/";

//    public Question callApi(TravelData dataDto){
//        return restTemplate.postForObject(fastapiUrl, dataDto, Question.class);
//    }

}
