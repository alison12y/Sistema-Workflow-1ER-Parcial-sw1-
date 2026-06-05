package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class AiWorkflowSuggestResponse {
    private Boolean aiAvailable;
    private Boolean fallbackUsed;
    private String error;
    private String explanation;
    private String flowType;
    private String intent;
    private Boolean requiresConfirmation = true;
    private List<AiSuggestActivityItem> suggestedActivities = new ArrayList<>();
    private List<AiSuggestTransitionItem> suggestedTransitions = new ArrayList<>();
    private List<AiSuggestResponsibleItem> suggestedResponsibles = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public Boolean getAiAvailable() { return aiAvailable; }
    public void setAiAvailable(Boolean aiAvailable) { this.aiAvailable = aiAvailable; }

    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Boolean getRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(Boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public List<AiSuggestActivityItem> getSuggestedActivities() { return suggestedActivities; }
    public void setSuggestedActivities(List<AiSuggestActivityItem> suggestedActivities) {
        this.suggestedActivities = suggestedActivities;
    }

    public List<AiSuggestTransitionItem> getSuggestedTransitions() { return suggestedTransitions; }
    public void setSuggestedTransitions(List<AiSuggestTransitionItem> suggestedTransitions) {
        this.suggestedTransitions = suggestedTransitions;
    }

    public List<AiSuggestResponsibleItem> getSuggestedResponsibles() { return suggestedResponsibles; }
    public void setSuggestedResponsibles(List<AiSuggestResponsibleItem> suggestedResponsibles) {
        this.suggestedResponsibles = suggestedResponsibles;
    }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
