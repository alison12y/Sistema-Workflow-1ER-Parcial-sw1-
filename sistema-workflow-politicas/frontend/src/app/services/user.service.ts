import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserDto, UserRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/users`;

  getAll(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(this.api);
  }

  create(body: UserRequest): Observable<UserDto> {
    return this.http.post<UserDto>(this.api, body);
  }

  update(id: string, body: UserRequest): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.api}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
