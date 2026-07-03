import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AttendanceResponse } from '../../dashboard/models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class AttendanceApiService {
  private readonly http = inject(HttpClient);

  getAttendances(): Observable<AttendanceResponse[]> {
    return this.http.get<AttendanceResponse[]>('/api/attendances');
  }
}
