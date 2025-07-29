package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.FastAPISimilaritySearchRequest;
import com.fastcampus.toyproject4_team4.entity.Prompt;
import com.fastcampus.toyproject4_team4.entity.Scenario;
import com.fastcampus.toyproject4_team4.repository.PromptRepository;
import com.fastcampus.toyproject4_team4.repository.ScenarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorService {
    private final PromptRepository promptRepository;
    private final ScenarioRepository scenarioRepository;

    public VectorService(PromptRepository promptRepository, ScenarioRepository scenarioRepository) {
        this.promptRepository = promptRepository;
        this.scenarioRepository = scenarioRepository;
    }

    public List<Integer> service(String context, Domains targetDomain, List<String> keywords) {
        Scenario scenario = scenarioRepository.findByCode("VSS_001").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);

        FastAPISimilaritySearchRequest payload = new FastAPISimilaritySearchRequest(context, targetDomain, keywords, prompt.getPromptTemplate());
        return APIUtil.sendPostRequest("http://localhost:8000/vector_ss/pks", payload, new TypeReference<List<Integer>>(){});
    }
}
