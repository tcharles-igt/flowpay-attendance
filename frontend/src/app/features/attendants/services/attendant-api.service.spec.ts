import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AttendantApiService } from './attendant-api.service';

describe('AttendantApiService', () => {
  let httpTestingController: HttpTestingController;
  let service: AttendantApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AttendantApiService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should request attendants list', () => {
    service.getAttendants().subscribe();

    const request = httpTestingController.expectOne('/api/attendants');

    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('should update attendant status', () => {
    service.updateAttendantStatus(7, { active: false }).subscribe();

    const request = httpTestingController.expectOne('/api/attendants/7/status');

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush({
      id: 7,
      name: 'Maria',
      team: 'CARDS',
      active: false,
      activeAttendances: 0,
      availableSlots: 0,
      createdAt: '2026-07-03T10:00:00Z',
      updatedAt: '2026-07-03T11:00:00Z'
    });
  });
});
