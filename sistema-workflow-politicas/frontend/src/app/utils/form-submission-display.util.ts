import { FormSubmission, FormSubmissionFileMeta, ResponseItemPayload } from '../models/my-activities.model';

export interface FormSubmissionView {
  activityName: string;
  submittedByName: string;
  updatedAt?: string;
  fields: FormSubmissionFieldView[];
}

export interface FormSubmissionFieldView {
  label: string;
  value: string;
  isFile?: boolean;
  fileId?: string;
  fileName?: string;
  downloadAvailable?: boolean;
}

export function isFileFieldType(fieldType?: string): boolean {
  return (fieldType ?? '').trim().toLowerCase() === 'file';
}

export function formatResponseValue(fieldType?: string, value?: string): string {
  if (value == null || value === '') {
    return '—';
  }
  if ((fieldType ?? '').toLowerCase() === 'checkbox') {
    if (value === 'true') {
      return 'Sí';
    }
    if (value === 'false') {
      return 'No';
    }
  }
  return value;
}

export function toFormSubmissionViews(submissions: FormSubmission[]): FormSubmissionView[] {
  return [...submissions]
    .sort((a, b) => a.taskOrder - b.taskOrder)
    .map((submission) => ({
      activityName: submission.activityName,
      submittedByName: submission.submittedByName?.trim() || '—',
      updatedAt: submission.updatedAt,
      fields: mapResponseFields(submission.responses),
    }))
    .filter((submission) => submission.fields.some((field) => hasMeaningfulValue(field)));
}

export function applySavedResponses(
  fields: Array<{ name?: string; label: string; type: string }>,
  values: Record<string, string>,
  fileAttachments: Record<string, FormSubmissionFileMeta | null>,
  responses?: ResponseItemPayload[]
): void {
  if (!responses?.length) {
    return;
  }

  const byName = new Map<string, ResponseItemPayload>();
  const byLabel = new Map<string, ResponseItemPayload>();

  for (const response of responses) {
    if (response.fieldName) {
      byName.set(response.fieldName, response);
    }
    if (response.fieldLabel?.trim()) {
      byLabel.set(response.fieldLabel.trim().toLowerCase(), response);
    }
  }

  for (const field of fields) {
    const key = field.name || field.label;
    const response = byName.get(key) ?? (field.name ? byName.get(field.name) : undefined)
      ?? byLabel.get(field.label.trim().toLowerCase());

    if (!response) {
      continue;
    }

    if (isFileFieldType(field.type)) {
      const fileName = response.fileName || response.value || '';
      values[key] = fileName;
      if (response.fileId?.trim()) {
        fileAttachments[key] = {
          fileId: response.fileId.trim(),
          fileName,
          contentType: response.contentType,
          size: response.size,
        };
      }
      continue;
    }

    values[key] = response.value ?? '';
  }
}

function mapResponseFields(responses?: ResponseItemPayload[]): FormSubmissionFieldView[] {
  if (!responses?.length) {
    return [];
  }

  return responses
    .filter((response) => response.fieldLabel?.trim())
    .map((response) => {
      const isFile = isFileFieldType(response.fieldType);
      const fileName = (response.fileName || response.value || '').trim();
      const fileId = response.fileId?.trim();

      if (isFile) {
        return {
          label: response.fieldLabel.trim(),
          value: fileId ? fileName : 'Archivo no disponible',
          isFile: true,
          fileId,
          fileName: fileName || undefined,
          downloadAvailable: !!fileId,
        };
      }

      return {
        label: response.fieldLabel.trim(),
        value: formatResponseValue(response.fieldType, response.value),
      };
    });
}

function hasMeaningfulValue(field: FormSubmissionFieldView): boolean {
  if (field.isFile) {
    return field.downloadAvailable || field.value !== '—';
  }
  return field.value !== '—';
}

export function triggerFileDownload(blob: Blob, fileName: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
}
