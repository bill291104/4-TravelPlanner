package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.FastAPIDomainExtractRequest;
import com.fastcampus.toyproject4_team4.dto.FastAPIKeywordExtractRequest;
import com.fastcampus.toyproject4_team4.entity.Prompt;
import com.fastcampus.toyproject4_team4.entity.Scenario;
import com.fastcampus.toyproject4_team4.repository.PromptRepository;
import com.fastcampus.toyproject4_team4.repository.ScenarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class ExtractService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final PromptRepository promptRepository;
    private final ScenarioRepository scenarioRepository;

    public ExtractService(PromptRepository promptRepository, ScenarioRepository scenarioRepository) {
        this.promptRepository = promptRepository;
        this.scenarioRepository = scenarioRepository;
    }

    public List<String> service(String context) {
        Domains targetDomain = extractDomain(context);
        List<String> keywords = extractKeywords(context, targetDomain);
        return keywords;
    }

    public Domains extractDomain(String context) {
        Scenario scenario = scenarioRepository.findByCode("EXT_001").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);
        List<Domains> domains = Domains.getMains();
        FastAPIDomainExtractRequest payload = new FastAPIDomainExtractRequest(context, domains, prompt.getPromptTemplate());
        try (HttpClient client = HttpClient.newHttpClient()) {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/extract/domain"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<Domains>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<String> extractKeywords(String context, Domains targetDomain) {
        Scenario scenario = scenarioRepository.findByCode("EXT_002").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);
        FastAPIKeywordExtractRequest payload = new FastAPIKeywordExtractRequest(context, targetDomain, prompt.getPromptTemplate());
        try (HttpClient client = HttpClient.newHttpClient()) {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/extract/keyword"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<List<String>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
