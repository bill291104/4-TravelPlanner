package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.FastAPIDomainExtractRequest;
import com.fastcampus.toyproject4_team4.dto.FastAPIKeywordExtractRequest;
import com.fastcampus.toyproject4_team4.entity.Prompt;
import com.fastcampus.toyproject4_team4.entity.Scenario;
import com.fastcampus.toyproject4_team4.repository.PromptRepository;
import com.fastcampus.toyproject4_team4.repository.ScenarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ExtractService {
    private final PromptRepository promptRepository;
    private final ScenarioRepository scenarioRepository;

    public ExtractService(PromptRepository promptRepository, ScenarioRepository scenarioRepository) {
        this.promptRepository = promptRepository;
        this.scenarioRepository = scenarioRepository;
    }

    public Domains extractDomain(String context) {
        log.debug("Extracting Domains Service Called\nArguments\ncontext: {}", context);
        Scenario scenario = scenarioRepository.findByCode("EXT_001").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);
        List<Domains> domains = Domains.getMains();
        FastAPIDomainExtractRequest payload = new FastAPIDomainExtractRequest(context, domains, prompt.getPromptTemplate());
        log.debug("Payload: {}", payload);
        Domains targetDomain = APIUtil.sendPostRequest("http://localhost:8000/extract/domain", payload, new TypeReference<Domains>() {});
        log.debug("Domain Extract Successfully\nResult\ntargetDomain: {}", targetDomain);
        return targetDomain;
    }

    public List<String> extractKeywords(String context, Domains targetDomain) {
        log.debug("Extracting Keywords Service Called\nArguments\ncontext: {}", context);
        Scenario scenario = scenarioRepository.findByCode("EXT_002").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);
        FastAPIKeywordExtractRequest payload = new FastAPIKeywordExtractRequest(context, targetDomain, prompt.getPromptTemplate());
        log.debug("Payload: {}", payload);
        List<String> keywords = APIUtil.sendPostRequest("http://localhost:8000/extract/keyword", payload, new TypeReference<List<String>>() {});
        log.debug("Keyword Extract Successfully\nResult\nkeywords: {}", keywords);
        return keywords;
    }
}
