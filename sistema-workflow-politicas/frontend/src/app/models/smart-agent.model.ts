export interface SmartAgentSuggestedField {
  name: string;
  label: string;
  type: string;
  required: boolean;
  suggestedValue?: string;
}

export interface SmartAgentAnalyzeResponse {
  detectedIntent: string;
  recommendedPolicyId?: string;
  recommendedPolicyName?: string;
  confidenceScore?: number;
  explanation?: string;
  requiredDocuments?: string[];
  suggestedFields?: SmartAgentSuggestedField[];
  source?: string;
  warnings?: string[];
  attachmentFileName?: string;
}

export interface SmartAgentAnalyzeRequest {
  message: string;
  audioText?: string;
  documentId?: string;
  requesterName?: string;
}

export interface SmartAgentStartTramiteRequest {
  policyId: string;
  description: string;
  requestedBy: string;
  priority?: string;
  detectedIntent?: string;
  agentExplanation?: string;
}

export interface SmartAgentStartTramiteResponse {
  tramite: {
    id: string;
    code: string;
    policyName?: string;
  };
  attachedDocument?: {
    id: string;
    nombreOriginal?: string;
  };
  message?: string;
}
