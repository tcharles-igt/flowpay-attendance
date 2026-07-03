export type AttendanceSubject = 'CARD_PROBLEM' | 'LOAN_REQUEST' | 'OTHER';
export type TeamType = 'CARDS' | 'LOANS' | 'OTHERS';
export type AttendanceStatus = 'WAITING' | 'IN_PROGRESS' | 'FINISHED';

export interface DashboardTeamSummary {
  team: TeamType;
  waiting: number;
  inProgress: number;
  finished: number;
  averageQueueTimeMinutes: number;
  averageServiceTimeMinutes: number;
}

export interface DashboardAttendant {
  id: number;
  name: string;
  team: TeamType;
  activeAttendances: number;
  availableSlots: number;
}

export interface DashboardQueueItem {
  id: number;
  customerName: string;
  subject: AttendanceSubject;
  team: TeamType;
  status: AttendanceStatus;
  createdAt: string;
}

export interface DashboardInProgressAttendance {
  id: number;
  customerName: string;
  subject: AttendanceSubject;
  team: TeamType;
  status: AttendanceStatus;
  attendantId: number;
  attendantName: string;
  startedAt: string;
}

export interface DashboardResponse {
  totalAttendances: number;
  waiting: number;
  inProgress: number;
  finished: number;
  averageQueueTimeMinutes: number;
  averageServiceTimeMinutes: number;
  teams: DashboardTeamSummary[];
  attendants: DashboardAttendant[];
  queue: DashboardQueueItem[];
  inProgressAttendances: DashboardInProgressAttendance[];
}

export type DashboardTeamFilter = TeamType | 'ALL';
export type DashboardStatusFilter = AttendanceStatus | 'ALL';

export interface CreateAttendanceRequest {
  customerName: string;
  subject: AttendanceSubject;
}

export interface AttendanceResponse {
  id: number;
  customerName: string;
  subject: AttendanceSubject;
  team: TeamType;
  status: AttendanceStatus;
  attendantId: number | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface ApiErrorResponse {
  message?: string;
  details?: string[];
}

export const attendanceSubjectOptions: Array<{ value: AttendanceSubject; label: string }> = [
  { value: 'CARD_PROBLEM', label: 'Cartao com problema' },
  { value: 'LOAN_REQUEST', label: 'Solicitacao de emprestimo' },
  { value: 'OTHER', label: 'Outro assunto' }
];

export const attendanceSubjectLabels: Record<AttendanceSubject, string> = {
  CARD_PROBLEM: 'Cartao',
  LOAN_REQUEST: 'Emprestimo',
  OTHER: 'Outros'
};

export const teamLabels: Record<TeamType, string> = {
  CARDS: 'Cartoes',
  LOANS: 'Emprestimos',
  OTHERS: 'Outros'
};

export const statusLabels: Record<AttendanceStatus, string> = {
  WAITING: 'Na fila',
  IN_PROGRESS: 'Em atendimento',
  FINISHED: 'Finalizado'
};

export const dashboardStatusOptions: Array<{ value: DashboardStatusFilter; label: string }> = [
  { value: 'ALL', label: 'Todos os status' },
  { value: 'WAITING', label: 'Na fila' },
  { value: 'IN_PROGRESS', label: 'Em atendimento' },
  { value: 'FINISHED', label: 'Finalizado' }
];
