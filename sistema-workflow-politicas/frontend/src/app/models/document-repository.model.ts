export interface DocumentRepository {
  id: string;
  tramiteId: string;
  tramiteCodigo: string;
  nombre: string;
  descripcion?: string;
  fechaCreacion?: string;
  creadoPor?: string;
  estado: string;
}

export interface DocumentRecord {
  id: string;
  documentFamilyId?: string;
  repositoryId: string;
  tramiteId: string;
  tramiteCodigo?: string;
  nombreArchivo: string;
  nombreOriginal: string;
  extension: string;
  contentType: string;
  tamano: number;
  s3Key: string;
  bucket: string;
  version: number;
  fechaSubida?: string;
  subidoPor?: string;
  estado: string;
}

export interface DocumentDownloadResponse {
  documento: DocumentRecord;
  presignedDownloadUrl: string;
  urlExpiraEn?: string;
  urlExpiraEnMinutos?: number;
  storageProvider?: string;
}
