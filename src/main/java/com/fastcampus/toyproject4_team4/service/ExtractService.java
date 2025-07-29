package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.FastAPIDomainExtractRequest;
import com.fastcampus.toyproject4_team4.dto.FastAPIKeywordExtractRequest;
import com.fastcampus.toyproject4_team4.entity.Prompt;
import com.fastcampus.toyproject4_team4.entity.Scenario;
import com.fastcampus.toyproject4_team4.repository.PromptRepository;
import com.fastcampus.toyproject4_team4.repository.ScenarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExtractService {
    private final PromptRepository promptRepository;
    private final ScenarioRepository scenarioRepository;

    public ExtractService(PromptRepository promptRepository, ScenarioRepository scenarioRepository) {
        this.promptRepository = promptRepository;
        this.scenarioRepository = scenarioRepository;
    }

    public Domains extractDomain(String context) {
        Scenario scenario = scenarioRepository.findByCode("EXT_001").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);
        List<Domains> domains = Domains.getMains();
        FastAPIDomainExtractRequest payload = new FastAPIDomainExtractRequest(context, domains, prompt.getPromptTemplate());
        return APIUtil.sendPostRequest("http://localhost:8000/extract/domain", payload, new TypeReference<Domains>(){});
    }

    public List<String> extractKeywords(String context, Domains targetDomain) {
        Scenario scenario = scenarioRepository.findByCode("EXT_002").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);
        FastAPIKeywordExtractRequest payload = new FastAPIKeywordExtractRequest(context, targetDomain, prompt.getPromptTemplate());
        return APIUtil.sendPostRequest("http://localhost:8000/extract/keyword", payload, new TypeReference<List<String>>() {});
    }
}
