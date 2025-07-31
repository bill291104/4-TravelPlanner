package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.*;
import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class TravelDataService {
    private static final Map<Domains, String> questions = new HashMap<>();

    @PostConstruct
    public void makeQuestions() {
        questions.put(Domains.PLACE, "어떤 장소에 가고 싶으신가요?");
        questions.put(Domains.ACCOM, "어떤 형태의 숙소를 찾으시나요?");
        questions.put(Domains.RESTAURANT, "식사는 어떤 종류로 찾아드릴까요? 좋아하는 음식이 있으신가요?");
    }

    public TravelData createTravelData(){
        return new TravelData(
                new ArrayList<PlaceDetail>(),
                new ArrayList<AccommodationDetail>(),
                new ArrayList<RestaurantDetail>(),
                new ArrayList<ChatMessage>());
    }

    public Domains checkEmptyDomain(TravelData td){
        log.debug("Checking empty domain...");

        if(td.places().isEmpty()){
            log.debug("Empty domain: place");
            return Domains.PLACE;
        }
        if(td.accommodations().isEmpty()){
            log.debug("Empty domain: accommodation");
            return Domains.ACCOM;
        }
        if(td.restaurants().isEmpty()){
            log.debug("Empty domain: restaurant");
            return Domains.RESTAURANT;
        }

        log.debug("No empty domain");
        return null;
    }

    public ChatResponse getQuestion(Domains domain) {
        return new ChatResponse(questions.get(domain), false);
    }
}