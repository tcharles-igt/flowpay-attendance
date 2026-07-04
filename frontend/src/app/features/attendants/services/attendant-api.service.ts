import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AttendantResponse,
  CreateAttendantRequest,
  UpdateAttendantRequest,
  UpdateAttendantStatusRequest
} from '../models/attendant.model';

@Injectable({ providedIn: 'root' })
export class AttendantApiService {
  private readonly http = inject(HttpClient);

  getAttendants(): Observable<AttendantResponse[]> {
    return this.http.get<AttendantResponse[]>('/api/attendants');
  }

  createAttendant(payload: CreateAttendantRequest): Observable<AttendantResponse> {
    return this.http.post<AttendantResponse>('/api/attendants', payload);
  }

  updateAttendant(attendantId: number, payload: UpdateAttendantRequest): Observable<AttendantResponse> {
    return this.http.put<AttendantResponse>(`/api/attendants/${attendantId}`, payload);
  }

  updateAttendantStatus(attendantId: number, payload: UpdateAttendantStatusRequest): Observable<AttendantResponse> {
    return this.http.patch<AttendantResponse>(`/api/attendants/${attendantId}/status`, payload);
  }
}
