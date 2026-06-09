package com.workflow.politicas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OnlyOfficeCallbackRequest {
    private int status;
    private String url;
    private String key;
    private String[] users;

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String[] getUsers() { return users; }
    public void setUsers(String[] users) { this.users = users; }
}
