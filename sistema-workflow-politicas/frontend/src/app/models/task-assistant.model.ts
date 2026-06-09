export interface TaskAssistantResponse {
  summary: string;
  importantData: string[];
  missingData: string[];
  recommendedAction: string;
  source: 'AI' | 'LOCAL_FALLBACK' | string;
}
