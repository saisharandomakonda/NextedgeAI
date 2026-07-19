package com.syncari.core.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncari.core.model.llm.*;
import com.syncari.core.repositories.customer.llm.LLMPromptRepo;
import com.syncari.core.service.secrets.SecretManager;
import com.syncari.core.token.TokenHelper;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Slf4j
@Component
public class LLMService {
    private static final String OPENAI_API_KEY = "openai_apikey";
    private static final String OPENI_COMPLETION_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    @Autowired
    protected TokenHelper tokenHelper;
    @Autowired
    RestTemplate llmRestTemplate;
    @Autowired
    LLMPromptRepo promptRepo;
    @Autowired
    SecretManager secretManager;
    @Autowired
    ObjectMapper mapper;
    public static final LLMProvider DEFAULT_LLM_PROVIDER = LLMProvider.OPENAI;

    public LLMResponse ask(String promptKey, LLMContext context) {
        final Optional<LLMPrompt> optionalPrompt = promptRepo.findByKeyAndProvider(promptKey, DEFAULT_LLM_PROVIDER.name());
        return optionalPrompt.map(prompt -> evaluate(prompt, context)).orElse(LLMResponse.unknownPrompt(promptKey));
    }

    public LLMResponse generate(String system, String user, LLMContext context) {
        LLMPrompt prompt = new LLMPrompt()
                .setSystemPrompt(system)
                .setUserPrompt(user)
                .setProvider(LLMProvider.OPENAI)
                .setPromptConfig(Map.of("model", "gpt-4-turbo", "temperature", 0, "top_p", 0.01));
        return evaluate(prompt, context);
    }

    private LLMResponse evaluate(LLMPrompt prompt, LLMContext context) {
        switch (prompt.getProvider()) {
            case OPENAI:
                return askOpenAI(prompt, context);
            default:
                throw new RuntimeException("We support only OpenAI at this point");
        }
    }

    @SneakyThrows
    private LLMResponse askOpenAI(LLMPrompt prompt, LLMContext context) {
        Map<String, Object> payload = new HashMap<>(prompt.getPromptConfig());
        List<Map<String, String>> messages = new ArrayList<>();
        final String resolvedSystemPrompt = prompt.getResolvedSystemPrompt(context, tokenHelper);
        final String resolvedUserPrompt = prompt.getResolvedUserPrompt(context, tokenHelper);
        if (!StringUtils.isBlank(resolvedSystemPrompt)) {
            messages.add(Map.of("role", "system", "content", resolvedSystemPrompt));
        }
        messages.add(Map.of("role", "user", "content", resolvedUserPrompt));
        payload.put("messages", messages);
        HttpHeaders headers = new HttpHeaders();
        final String secret = secretManager.getSecret(OPENAI_API_KEY);
        headers.set("Authorization", "Bearer " + secret);
        headers.set(HttpHeaders.USER_AGENT, "syncari.com/llm-agent/1.0");
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set(HttpHeaders.ACCEPT, "*/*");
        final String payloadString = mapper.writer().writeValueAsString(payload);
        log.info("LLM Payload:{}", payloadString);
        HttpEntity httpEntity = new HttpEntity(payloadString, headers);
        try {
            final ResponseEntity<String> result = llmRestTemplate.exchange(URI.create(OPENI_COMPLETION_ENDPOINT), HttpMethod.POST, httpEntity, String.class);

            if (result.getStatusCode().isError()) {
                return LLMResponse.error(result.getBody().toString());
            }
            log.info("LLM Response:{}", result.getBody());
            return LLMResponse.response(extractResponse(result), ResponseType.MARKDOWN);
        } catch (HttpClientErrorException e) {
            log.error(e.getResponseBodyAsString(), e);
            return LLMResponse.error(e.getMessage().toString() + ":" + e.getResponseBodyAsString());
        }
    }

    @SneakyThrows
    private String extractResponse(ResponseEntity<String> result) {
        final OpenAIResponse response = mapper.readValue(result.getBody(), OpenAIResponse.class);
        return Optional.ofNullable(response.getContent()).map(m -> m.toString()).orElse("");
    }
}

@Data
class OpenAIResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices = List.of();
    private Usage usage;
    String system_fingerprint;

    public String getContent() {
        if (choices.isEmpty() || choices.get(0).message == null) {
            return null;
        }
        return choices.get(0).message.content;
    }
}

@Data
class Choice {
    int index;
    Message message;
    String logprobs;
    String finish_reason;

}

@Data
class Message {
    String role;
    String content;
}

@Data
class Usage {
    int prompt_tokens;
    int completion_tokens;
    int total_tokens;
}