import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AttendanceResponse, CreateAttendanceRequest } from '../../dashboard/models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class AttendanceApiService {
  private readonly http = inject(HttpClient);

  getAttendances(): Observable<AttendanceResponse[]> {
    return this.http.get<AttendanceResponse[]>('/api/attendances');
  }

  createAttendance(payload: CreateAttendanceRequest): Observable<AttendanceResponse> {
    return this.http.post<AttendanceResponse>('/api/attendances', payload);
  }

  finishAttendance(attendanceId: number): Observable<AttendanceResponse> {
    return this.http.patch<AttendanceResponse>(`/api/attendances/${attendanceId}/finish`, {});
  }
}
