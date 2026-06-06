import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  DocumentDownloadResponse,
  DocumentRecord,
  DocumentRepository,
} from '../models/document-repository.model';

@Injectable({ providedIn: 'root' })
export class DocumentRepositoryService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/document-repositories`;

  getByTramite(tramiteId: string): Observable<DocumentRepository> {
    return this.http
      .get<DocumentRepository>(`${this.api}/tramite/${encodeURIComponent(tramiteId)}`)
      .pipe(catchError((error) => throwError(() => error)));
  }

  listDocuments(repositoryId: string): Observable<DocumentRecord[]> {
    return this.http.get<DocumentRecord[]>(
      `${this.api}/${encodeURIComponent(repositoryId)}/documents`
    );
  }

  listDocumentVersions(documentId: string): Observable<DocumentRecord[]> {
    return this.http.get<DocumentRecord[]>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/versions`
    );
  }

  uploadDocument(repositoryId: string, file: File): Observable<DocumentRecord> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<DocumentRecord>(
      `${this.api}/${encodeURIComponent(repositoryId)}/upload`,
      formData
    );
  }

  getDownloadInfo(documentId: string): Observable<DocumentDownloadResponse> {
    return this.http.get<DocumentDownloadResponse>(
      `${this.api}/documents/${encodeURIComponent(documentId)}`
    );
  }

  deleteDocument(documentId: string): Observable<DocumentRecord> {
    return this.http.delete<DocumentRecord>(
      `${this.api}/documents/${encodeURIComponent(documentId)}`
    );
  }
}
