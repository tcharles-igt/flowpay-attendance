import { Subject, of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AttendantManagementPageComponent } from './attendant-management-page.component';
import { AttendantApiService } from '../services/attendant-api.service';
import { AttendantResponse } from '../models/attendant.model';

describe('AttendantManagementPageComponent', () => {
  let dialog: { open: ReturnType<typeof vi.fn> };
  let snackBar: { openFromComponent: ReturnType<typeof vi.fn> };
  let service: {
    getAttendants: ReturnType<typeof vi.fn>;
    createAttendant: ReturnType<typeof vi.fn>;
    updateAttendant: ReturnType<typeof vi.fn>;
    updateAttendantStatus: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    dialog = {
      open: vi.fn()
    };
    snackBar = {
      openFromComponent: vi.fn()
    };
    service = {
      getAttendants: vi.fn(),
      createAttendant: vi.fn(),
      updateAttendant: vi.fn(),
      updateAttendantStatus: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AttendantManagementPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AttendantApiService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();
  });

  it('should render loading state while the first request is pending', () => {
    const response$ = new Subject<AttendantResponse[]>();
    service.getAttendants.mockReturnValue(response$);

    const fixture = TestBed.createComponent(AttendantManagementPageComponent);

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Carregando atendentes');
  });

  it('should render empty state when no attendants are returned', () => {
    service.getAttendants.mockReturnValue(of([]));

    const fixture = TestBed.createComponent(AttendantManagementPageComponent);

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhum atendente cadastrado ainda');
  });

  it('should render attendants table and summary cards', () => {
    service.getAttendants.mockReturnValue(
      of([
        attendantResponse({ id: 1, name: 'Maria', team: 'CARDS', active: true, activeAttendances: 2, availableSlots: 1 }),
        attendantResponse({ id: 2, name: 'Pedro', team: 'LOANS', active: false, activeAttendances: 0, availableSlots: 0 })
      ])
    );

    const fixture = TestBed.createComponent(AttendantManagementPageComponent);

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Gerenciamento de atendentes');
    expect(fixture.nativeElement.textContent).toContain('Maria');
    expect(fixture.nativeElement.textContent).toContain('Capacidade disponivel');
    expect(fixture.nativeElement.textContent).toContain('Ativo');
    expect(fixture.nativeElement.textContent).toContain('Inativo');
  });
});

function attendantResponse(overrides: Partial<AttendantResponse>): AttendantResponse {
  return {
    id: 1,
    name: 'Ana',
    team: 'OTHERS',
    active: true,
    activeAttendances: 0,
    availableSlots: 3,
    createdAt: '2026-07-03T10:00:00Z',
    updatedAt: '2026-07-03T11:00:00Z',
    ...overrides
  };
}
