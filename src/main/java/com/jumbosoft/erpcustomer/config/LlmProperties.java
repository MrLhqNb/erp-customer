package com.jumbosoft.erpcustomer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey;
    private String chatModel = "deepseek-chat";
    private double temperature = 0.3;
    private int maxTokens = 2048;
    private int chatTimeoutSeconds = 60;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public int getChatTimeoutSeconds() { return chatTimeoutSeconds; }
    public void setChatTimeoutSeconds(int chatTimeoutSeconds) { this.chatTimeoutSeconds = chatTimeoutSeconds; }
}
