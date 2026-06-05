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
