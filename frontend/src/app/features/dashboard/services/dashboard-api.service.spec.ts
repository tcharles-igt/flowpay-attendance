import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { DashboardResponse } from '../models/dashboard.model';
import { DashboardApiService } from './dashboard-api.service';

class FakeEventSource {
  static lastInstance: FakeEventSource | null = null;

  readonly close = vi.fn();
  onerror: (() => void) | null = null;
  onopen: (() => void) | null = null;
  private readonly listeners = new Map<string, Set<(event: Event) => void>>();

  constructor(readonly url: string) {
    FakeEventSource.lastInstance = this;
  }

  addEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    const callback =
      typeof listener === 'function' ? listener : (event: Event) => listener.handleEvent(event);
    const listeners = this.listeners.get(type) ?? new Set<(event: Event) => void>();
    listeners.add(callback);
    this.listeners.set(type, listeners);
  }

  removeEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    const callback =
      typeof listener === 'function' ? listener : (event: Event) => listener.handleEvent(event);
    this.listeners.get(type)?.delete(callback);
  }

  emit(type: string, data?: string): void {
    const event = { data } as MessageEvent<string>;
    this.listeners.get(type)?.forEach((listener) => listener(event));
  }
}

describe('DashboardApiService', () => {
  let originalEventSource: typeof EventSource | undefined;
  let service: DashboardApiService;

  beforeEach(() => {
    originalEventSource = globalThis.EventSource;
    globalThis.EventSource = FakeEventSource as unknown as typeof EventSource;

    TestBed.configureTestingModule({
      providers: [provideHttpClient()]
    });

    service = TestBed.inject(DashboardApiService);
  });

  afterEach(() => {
    globalThis.EventSource = originalEventSource as typeof EventSource;
    FakeEventSource.lastInstance = null;
  });

  it('should connect to dashboard stream and parse snapshot events', () => {
    const onSnapshot = vi.fn();
    const connection = service.connectDashboardStream({
      onSnapshot,
      onOpen: vi.fn(),
      onError: vi.fn()
    });

    const response: DashboardResponse = {
      totalAttendances: 3,
      waiting: 1,
      inProgress: 1,
      finished: 1,
      averageQueueTimeMinutes: 5,
      averageServiceTimeMinutes: 8,
      teams: [],
      attendants: [],
      queue: [],
      inProgressAttendances: []
    };

    FakeEventSource.lastInstance?.emit('dashboard-updated', JSON.stringify(response));

    expect(onSnapshot).toHaveBeenCalledWith(response);

    connection.close();

    expect(FakeEventSource.lastInstance?.close).toHaveBeenCalled();
  });

  it('should expose open and error callbacks from EventSource', () => {
    const onOpen = vi.fn();
    const onError = vi.fn();

    service.connectDashboardStream({
      onSnapshot: vi.fn(),
      onOpen,
      onError
    });

    FakeEventSource.lastInstance?.onopen?.();
    FakeEventSource.lastInstance?.onerror?.();

    expect(onOpen).toHaveBeenCalled();
    expect(onError).toHaveBeenCalled();
  });
});
