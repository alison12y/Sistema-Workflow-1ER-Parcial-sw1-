export interface AnalyticsRequest {
  message?: string;
  audioText?: string;
  policyId?: string;
  status?: string;
  fromDate?: string;
  toDate?: string;
}

export interface AnalyticsSummaryCard {
  label: string;
  value: string;
  hint?: string;
  severity?: 'info' | 'warning' | 'danger' | 'success';
}

export interface AnalyticsChart {
  type?: 'bar' | 'pie';
  title?: string;
  labels?: string[];
  values?: number[];
}

export interface AnalyticsReportResponse {
  title?: string;
  explanation?: string;
  /** Respuesta directa a la consulta del usuario. */
  conclusion?: string;
  reportType?: string;
  columns?: string[];
  rows?: Record<string, unknown>[];
  appliedFilters?: Record<string, unknown>;
  suggestedFormat?: string;
  source?: string;
  warnings?: string[];
  cards?: AnalyticsSummaryCard[];
  chart?: AnalyticsChart;
}

export interface AnalyticsRiskItem {
  type?: string;
  severity?: string;
  title?: string;
  description?: string;
  entityType?: string;
  entityId?: string;
  entityLabel?: string;
}

export interface AnalyticsRiskResponse {
  summary?: string;
  source?: string;
  risks?: AnalyticsRiskItem[];
  cards?: AnalyticsSummaryCard[];
  warnings?: string[];
}

export interface AnalyticsRecommendationItem {
  priority?: string;
  type?: string;
  title?: string;
  action?: string;
  rationale?: string;
  tramiteCode?: string;
  activityName?: string;
}

export interface AnalyticsRecommendationResponse {
  summary?: string;
  source?: string;
  recommendations?: AnalyticsRecommendationItem[];
  cards?: AnalyticsSummaryCard[];
  warnings?: string[];
}

export interface AnalyticsFullResult {
  report: AnalyticsReportResponse | null;
  risks: AnalyticsRiskResponse | null;
  recommendations: AnalyticsRecommendationResponse | null;
}
