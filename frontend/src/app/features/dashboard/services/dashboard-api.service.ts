import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CreateAttendanceRequest, DashboardResponse } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);

  getDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>('/api/dashboard');
  }

  createAttendance(payload: CreateAttendanceRequest): Observable<unknown> {
    return this.http.post('/api/attendances', payload);
  }

  finishAttendance(attendanceId: number): Observable<unknown> {
    return this.http.patch(`/api/attendances/${attendanceId}/finish`, {});
  }
}
