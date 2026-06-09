from typing import Any, Optional

from pydantic import BaseModel, Field


class GenerateWorkflowRequest(BaseModel):
    prompt: str = Field(..., min_length=1)


class WorkflowSuggestRequest(BaseModel):
    policyId: str = Field(default="")
    prompt: str = Field(..., min_length=1)
    activities: list[dict[str, Any]] = Field(default_factory=list)
    transitions: list[dict[str, Any]] = Field(default_factory=list)
    lanes: list[dict[str, Any]] = Field(default_factory=list)


class AssistFormRequest(BaseModel):
    report: str = Field(default="")
    prompt: str = Field(default="")
    policyId: str | None = None
    tramiteId: str | None = None
    workflowActivityId: str | None = None
    formId: str | None = None
    activityName: str | None = None
    fields: list[dict[str, Any]] = Field(default_factory=list)
    currentValues: dict[str, Any] = Field(default_factory=dict)
    context: dict[str, Any] = Field(default_factory=dict)

    def effective_report(self) -> str:
        text = (self.report or self.prompt or "").strip()
        if not text:
            raise ValueError("report is required")
        return text


class ValidateDiagramRequest(BaseModel):
    activities: list[dict[str, Any]] = Field(default_factory=list)
    transitions: list[dict[str, Any]] = Field(default_factory=list)


class AssistantRequest(BaseModel):
    prompt: str = Field(..., min_length=1)
    module: str = Field(default="policies")
    context: dict[str, Any] = Field(default_factory=dict)


class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None


class AgentAnalyzeRequest(BaseModel):
    message: str = Field(..., min_length=1)
    audioText: str | None = None
    requesterName: str | None = None
    attachmentFileName: str | None = None
    documentContext: dict[str, Any] = Field(default_factory=dict)
    policies: list[dict[str, Any]] = Field(default_factory=list)


class AnalyticsRequest(BaseModel):
    message: str = Field(default="")
    audioText: str | None = None
    policyId: str | None = None
    status: str | None = None
    fromDate: str | None = None
    toDate: str | None = None
    tramiteCount: int = 0
    kpiSummary: dict[str, Any] | None = None
    bottlenecks: list[dict[str, Any]] = Field(default_factory=list)
    employeeLoad: list[dict[str, Any]] = Field(default_factory=list)
    tramiteSample: list[dict[str, Any]] = Field(default_factory=list)

    def effective_message(self) -> str:
        text = (self.message or "").strip()
        if self.audioText:
            text = f"{text} {self.audioText}".strip()
        return text


class TaskAssistantRequest(BaseModel):
    taskId: str | None = None
    tramiteId: str | None = None
    tramiteName: str | None = None
    activityName: str | None = None
    activityDescription: str | None = None
    taskStatus: str | None = None
    assignedTo: str | None = None
    formData: dict[str, Any] = Field(default_factory=dict)
    documents: list[dict[str, Any]] = Field(default_factory=list)
    observations: str | None = None
    createdAt: str | None = None


class TaskAssistantResponse(BaseModel):
    summary: str
    importantData: list[str] = Field(default_factory=list)
    missingData: list[str] = Field(default_factory=list)
    recommendedAction: str
    source: str = "AI"

