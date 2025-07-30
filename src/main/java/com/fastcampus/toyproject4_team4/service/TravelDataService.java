package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.dto.ChatMessage;
import com.fastcampus.toyproject4_team4.dto.ChatResponse;
import com.fastcampus.toyproject4_team4.dto.TravelData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;


@Service
public class TravelDataService {
    //con1
    public TravelData createTravelData(){
        return new TravelData(new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Object>(),new ArrayList<ChatMessage>());
    }// createTravelData()

    //cocn2
    public ChatResponse checkEmptyDomain(TravelData td){

        System.out.println("<--- checkpoint  --->  con2 서비스 시이작  ");

        if(td.places().isEmpty()){
            return new ChatResponse("어떤 장소에 가고 싶으신가요?",true);
        }
        if(td.accommodations().isEmpty()){
            return new ChatResponse("어떤 형태의 숙소를 찾으시나요?",true);
        }
        if(td.restaurants().isEmpty()){
            return new ChatResponse("식사는 어떤 종류로 찾아드릴까요? 좋아하는 음식이 있으신가요?",true);
        }

        System.out.println("<--- checkpoint  --->  컨2 서비스 곧 끝나아암 ");
        return new ChatResponse(" ",false);

        }//

    }