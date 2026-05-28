import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CompleteActivityPayload, MyActivity } from '../models/my-activities.model';
import { Tramite } from '../models/tramite.model';

@Injectable({ providedIn: 'root' })
export class MyActivitiesService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/my-activities`;

  getAll(): Observable<MyActivity[]> {
    return this.http.get<MyActivity[]>(this.api);
  }

  getById(tramiteId: string): Observable<MyActivity> {
    return this.http.get<MyActivity>(`${this.api}/${encodeURIComponent(tramiteId)}`);
  }

  complete(tramiteId: string, payload: CompleteActivityPayload): Observable<Tramite> {
    return this.http.put<Tramite>(`${this.api}/${encodeURIComponent(tramiteId)}/complete`, payload);
  }
}
