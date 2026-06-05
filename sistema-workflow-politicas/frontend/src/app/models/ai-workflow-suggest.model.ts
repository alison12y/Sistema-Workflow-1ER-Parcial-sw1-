export interface AiWorkflowContextActivity {
  id?: string;
  name: string;
  activityType?: string;
  responsibleName?: string;
  responsibleType?: string;
  orderIndex?: number;
}

export interface AiWorkflowContextTransition {
  id?: string;
  fromActivityId?: string;
  fromActivityName?: string;
  toActivityId?: string;
  toActivityName?: string;
  transitionType?: string;
  conditionLabel?: string;
}

export interface AiWorkflowContextLane {
  name: string;
  activityCount?: number;
}

export interface AiWorkflowSuggestRequest {
  policyId: string;
  prompt: string;
  userId?: string;
  activities: AiWorkflowContextActivity[];
  transitions: AiWorkflowContextTransition[];
  lanes: AiWorkflowContextLane[];
}

export interface AiSuggestActivityItem {
  operation?: string;
  name: string;
  activityType?: string;
  responsibleName?: string;
  responsibleType?: string;
  orderIndex?: number;
  connectAfterActivityName?: string;
}

export interface AiSuggestTransitionItem {
  operation?: string;
  fromActivityName: string;
  toActivityName: string;
  transitionType?: string;
  conditionLabel?: string;
}

export interface AiSuggestResponsibleItem {
  name: string;
  type?: string;
}

export interface AiWorkflowSuggestResponse {
  aiAvailable?: boolean;
  fallbackUsed?: boolean;
  error?: string;
  explanation?: string;
  flowType?: string;
  intent?: string;
  requiresConfirmation?: boolean;
  suggestedActivities?: AiSuggestActivityItem[];
  suggestedTransitions?: AiSuggestTransitionItem[];
  suggestedResponsibles?: AiSuggestResponsibleItem[];
  suggestions?: string[];
  warnings?: string[];
}
