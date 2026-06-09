package com.workflow.politicas.dto;

import com.workflow.politicas.model.DocumentRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocumentEditorSessionResponse {
    private DocumentRecordResponse document;
    private DocumentAccessResponse access;
    private DocumentCollaborationStateResponse collaboration;
    private List<DocumentCollaborationRecentActionDto> recentActions;
    private boolean onlyOfficeEnabled;
    private boolean fallbackMode;
    private boolean readOnly;
    private String onlyOfficeApiScriptUrl;
    private Map<String, Object> onlyOfficeConfig;

    public DocumentRecordResponse getDocument() { return document; }
    public void setDocument(DocumentRecordResponse document) { this.document = document; }
    public DocumentAccessResponse getAccess() { return access; }
    public void setAccess(DocumentAccessResponse access) { this.access = access; }
    public DocumentCollaborationStateResponse getCollaboration() { return collaboration; }
    public void setCollaboration(DocumentCollaborationStateResponse collaboration) { this.collaboration = collaboration; }
    public List<DocumentCollaborationRecentActionDto> getRecentActions() { return recentActions; }
    public void setRecentActions(List<DocumentCollaborationRecentActionDto> recentActions) { this.recentActions = recentActions; }
    public boolean isOnlyOfficeEnabled() { return onlyOfficeEnabled; }
    public void setOnlyOfficeEnabled(boolean onlyOfficeEnabled) { this.onlyOfficeEnabled = onlyOfficeEnabled; }
    public boolean isFallbackMode() { return fallbackMode; }
    public void setFallbackMode(boolean fallbackMode) { this.fallbackMode = fallbackMode; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    public String getOnlyOfficeApiScriptUrl() { return onlyOfficeApiScriptUrl; }
    public void setOnlyOfficeApiScriptUrl(String onlyOfficeApiScriptUrl) { this.onlyOfficeApiScriptUrl = onlyOfficeApiScriptUrl; }
    public Map<String, Object> getOnlyOfficeConfig() { return onlyOfficeConfig; }
    public void setOnlyOfficeConfig(Map<String, Object> onlyOfficeConfig) { this.onlyOfficeConfig = onlyOfficeConfig; }
}
