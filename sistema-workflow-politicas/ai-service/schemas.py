from typing import Any, Optional

from pydantic import BaseModel, Field


class GenerateWorkflowRequest(BaseModel):
    prompt: str = Field(..., min_length=1)


class AssistFormRequest(BaseModel):
    prompt: str = Field(..., min_length=1)
    fieldName: str = Field(..., min_length=1)
    context: dict[str, Any] = Field(default_factory=dict)


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
