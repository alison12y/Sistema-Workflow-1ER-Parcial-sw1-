package com.workflow.politicas.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.onlyoffice")
public class OnlyOfficeProperties {

    private boolean enabled = false;
    private String documentServerUrl = "http://localhost:8082";
    /** URL base que OnlyOffice (Docker) usa para alcanzar el backend, ej. http://host.docker.internal:8080 */
    private String backendPublicUrl = "http://host.docker.internal:8080";
    private String jwtSecret = "onlyoffice-local-dev-secret";
    private boolean jwtEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDocumentServerUrl() {
        return documentServerUrl;
    }

    public void setDocumentServerUrl(String documentServerUrl) {
        this.documentServerUrl = documentServerUrl;
    }

    public String getBackendPublicUrl() {
        return backendPublicUrl;
    }

    public void setBackendPublicUrl(String backendPublicUrl) {
        this.backendPublicUrl = backendPublicUrl;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public boolean isJwtEnabled() {
        return jwtEnabled;
    }

    public void setJwtEnabled(boolean jwtEnabled) {
        this.jwtEnabled = jwtEnabled;
    }

    public String getDocumentServerApiScriptUrl() {
        String base = documentServerUrl != null ? documentServerUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/web-apps/apps/api/documents/api.js";
    }
}
