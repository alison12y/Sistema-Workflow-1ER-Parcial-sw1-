package com.workflow.politicas.dto;

public class AiSuggestedEndpointDto {
    private String method;
    private String url;

    public AiSuggestedEndpointDto() {}

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
