package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.dto.ChatResponse;
import com.fastcampus.toyproject4_team4.dto.TravelData;
import com.fastcampus.toyproject4_team4.service.TravelDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping
public class TravelDataController {
    private final TravelDataService travelDataService;

    public TravelDataController(TravelDataService travelDataService) {
        this.travelDataService = travelDataService;
    }

    @GetMapping("/init")
    public ResponseEntity<TravelData> index(){
        TravelData td = travelDataService.createTravelData();
        System.out.println("<--- checkpoint  --->  여기는 init 컨트롤러어어 ");
        System.out.println("<--- checkpoint  --->  Travel Data 잘 생성됐는지 확인  "+td);

        return ResponseEntity.ok(td);
    }

    @PostMapping("/check")
    public ResponseEntity<ChatResponse> checkDomain(@RequestBody TravelData td){

        System.out.println("<--- checkpoint  --->  여기는 check 컨트롤러어어 진입!!");
        System.out.println("<--- checkpoint  --->  초기 traveldata 확인 >>>   "+td);

        ChatResponse cr = travelDataService.checkEmptyDomain(td);

        System.out.println("<--- checkpoint  ---> check 컨트롤러 잘 돼가???");
        System.out.println("<<--- CHECK POINT --->>" + cr);
        System.out.println("<--- checkpoint  --->  재질문이 나온 다음엔???  >>>   "+td);
        return ResponseEntity.ok(cr);
    }
}
