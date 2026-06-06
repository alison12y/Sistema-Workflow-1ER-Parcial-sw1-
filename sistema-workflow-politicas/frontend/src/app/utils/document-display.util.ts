export const MAX_DOCUMENT_SIZE_BYTES = 10 * 1024 * 1024;



export const ACCEPTED_DOCUMENT_EXTENSIONS =

  '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg,.txt,.csv,.zip,.rar,.mp4,.mp3';



const KNOWN_EXTENSIONS = new Set([

  'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',

  'png', 'jpg', 'jpeg', 'txt', 'csv', 'zip', 'rar', 'mp4', 'mp3',

]);



export function extractFileExtension(fileName: string): string {

  const dot = fileName.lastIndexOf('.');

  if (dot < 0 || dot === fileName.length - 1) return '';

  return fileName.substring(dot + 1).toLowerCase();

}



export function validateDocumentUpload(file: File): string | null {

  if (!file || file.size <= 0) {

    return 'Debe seleccionar un archivo válido';

  }

  if (file.size > MAX_DOCUMENT_SIZE_BYTES) {

    return 'El archivo no puede superar 10 MB';

  }

  return null;

}



export function isKnownDocumentExtension(fileName: string): boolean {

  const ext = extractFileExtension(fileName);

  return ext === '' || KNOWN_EXTENSIONS.has(ext);

}



export function formatFileSize(bytes?: number): string {

  if (bytes == null || bytes < 0) return '—';

  if (bytes < 1024) return `${bytes} B`;

  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;

  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;

}



export function documentTypeLabel(extension?: string, contentType?: string): string {

  const ext = (extension || '').toLowerCase();

  if (ext) return ext.toUpperCase();

  if (contentType?.includes('pdf')) return 'PDF';

  if (contentType?.startsWith('image/')) return 'Imagen';

  if (contentType?.startsWith('video/')) return 'Video';

  if (contentType?.startsWith('audio/')) return 'Audio';

  return contentType || 'Archivo';

}



export function documentStatusLabel(estado?: string): string {

  switch ((estado || '').toUpperCase()) {

    case 'ACTIVO':

      return 'Actual';

    case 'HISTORICO':

      return 'Histórico';

    case 'ELIMINADO':

      return 'Eliminado';

    default:

      return estado || '—';

  }

}



export function documentStatusClass(estado?: string): string {

  switch ((estado || '').toUpperCase()) {

    case 'ACTIVO':

      return 'badge-success';

    case 'HISTORICO':

      return 'badge-muted';

    case 'ELIMINADO':

      return 'badge-danger';

    default:

      return 'badge-muted';

  }

}



export function isPreviewableDocument(extension?: string, contentType?: string): boolean {

  const ext = (extension || '').toLowerCase();

  if (['pdf', 'png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) return true;

  const type = (contentType || '').toLowerCase();

  return type.startsWith('image/') || type.includes('pdf');

}



export type DocumentIconType = 'pdf' | 'word' | 'excel' | 'image' | 'text' | 'zip' | 'file';



export function documentIconType(extension?: string, contentType?: string): DocumentIconType {

  const ext = (extension || '').toLowerCase();

  const type = (contentType || '').toLowerCase();

  if (ext === 'pdf' || type.includes('pdf')) return 'pdf';

  if (['doc', 'docx'].includes(ext)) return 'word';

  if (['xls', 'xlsx'].includes(ext)) return 'excel';

  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext) || type.startsWith('image/')) return 'image';

  if (ext === 'txt' || type.includes('text/plain')) return 'text';

  if (['zip', 'rar'].includes(ext)) return 'zip';

  return 'file';

}



export function documentMaterialIcon(extension?: string, contentType?: string): string {

  const icons: Record<DocumentIconType, string> = {

    pdf: 'picture_as_pdf',

    word: 'description',

    excel: 'table_chart',

    image: 'image',

    text: 'article',

    zip: 'folder_zip',

    file: 'insert_drive_file',

  };

  return icons[documentIconType(extension, contentType)];

}



export function documentStatusChipClass(estado?: string): string {

  switch ((estado || '').toUpperCase()) {

    case 'ACTIVO':

      return 'status-chip-actual';

    case 'HISTORICO':

      return 'status-chip-historic';

    case 'ELIMINADO':

      return 'status-chip-deleted';

    default:

      return 'status-chip-historic';

  }

}


