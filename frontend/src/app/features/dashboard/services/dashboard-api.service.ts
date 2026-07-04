import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CreateAttendanceRequest, DashboardResponse } from '../models/dashboard.model';

export interface DashboardStreamHandlers {
  onError: () => void;
  onOpen: () => void;
  onSnapshot: (snapshot: DashboardResponse) => void;
}

export interface DashboardStreamConnection {
  close: () => void;
}

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);

  getDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>('/api/dashboard');
  }

  connectDashboardStream(handlers: DashboardStreamHandlers): DashboardStreamConnection {
    const eventSource = new EventSource('/api/dashboard/events');

    const handleSnapshot = (event: Event) => {
      const message = event as MessageEvent<string>;
      handlers.onSnapshot(JSON.parse(message.data) as DashboardResponse);
    };

    const handleHeartbeat = () => {};

    eventSource.addEventListener('dashboard-updated', handleSnapshot);
    eventSource.addEventListener('heartbeat', handleHeartbeat);
    eventSource.onopen = () => handlers.onOpen();
    eventSource.onerror = () => handlers.onError();

    return {
      close: () => {
        eventSource.removeEventListener('dashboard-updated', handleSnapshot);
        eventSource.removeEventListener('heartbeat', handleHeartbeat);
        eventSource.close();
      }
    };
  }

  createAttendance(payload: CreateAttendanceRequest): Observable<unknown> {
    return this.http.post('/api/attendances', payload);
  }

  finishAttendance(attendanceId: number): Observable<unknown> {
    return this.http.patch(`/api/attendances/${attendanceId}/finish`, {});
  }

  startAttendance(attendanceId: number): Observable<unknown> {
    return this.http.patch(`/api/attendances/${attendanceId}/start`, {});
  }
}
