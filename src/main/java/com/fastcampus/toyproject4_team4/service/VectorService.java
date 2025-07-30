package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.FastAPISimilaritySearchRequest;
import com.fastcampus.toyproject4_team4.entity.Prompt;
import com.fastcampus.toyproject4_team4.entity.Scenario;
import com.fastcampus.toyproject4_team4.repository.PromptRepository;
import com.fastcampus.toyproject4_team4.repository.ScenarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class VectorService {
    @Value("${fastapi.url}")
    String fastApiUrl;

    private final PromptRepository promptRepository;
    private final ScenarioRepository scenarioRepository;

    public VectorService(PromptRepository promptRepository, ScenarioRepository scenarioRepository) {
        this.promptRepository = promptRepository;
        this.scenarioRepository = scenarioRepository;
    }

    public List<Long> service(String context, Domains targetDomain, List<String> keywords) {
        log.debug("Vector Service Called\nArguments\ncontext: {}\ntargetDomain: {}\nkeywords: {}", context, targetDomain.toString(), keywords);
        Scenario scenario = scenarioRepository.findByCode("VSS_001").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);

        FastAPISimilaritySearchRequest payload = new FastAPISimilaritySearchRequest(context, targetDomain, keywords, prompt.getPromptTemplate());
        log.debug("Payload: {}", payload);
        List<Long> pks = APIUtil.sendPostRequest(fastApiUrl + "/vector_ss/pks", payload, new TypeReference<List<Long>>() {});
        log.debug("PK Extract Successfully\nResult\npks: {}", pks);
        return pks;
    }
}
