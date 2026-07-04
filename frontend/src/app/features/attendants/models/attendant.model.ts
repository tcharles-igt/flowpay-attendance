import { ApiErrorResponse, TeamType, teamLabels } from '../../dashboard/models/dashboard.model';

export type AttendantTeamFilter = TeamType | 'ALL';
export type AttendantStatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE';

export interface AttendantResponse {
  id: number;
  name: string;
  team: TeamType;
  active: boolean;
  activeAttendances: number;
  availableSlots: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAttendantRequest {
  name: string;
  team: TeamType;
  active?: boolean;
}

export interface UpdateAttendantRequest {
  name: string;
  team: TeamType;
  active?: boolean | null;
}

export interface UpdateAttendantStatusRequest {
  active: boolean;
}

export interface AttendantFormValue {
  name: string;
  team: TeamType;
}

export type { ApiErrorResponse, TeamType };
export { teamLabels };

export const attendantStatusOptions: Array<{ value: AttendantStatusFilter; label: string }> = [
  { value: 'ALL', label: 'Todos os status' },
  { value: 'ACTIVE', label: 'Ativos' },
  { value: 'INACTIVE', label: 'Inativos' }
];

export const attendantStatusLabels: Record<'ACTIVE' | 'INACTIVE', string> = {
  ACTIVE: 'Ativo',
  INACTIVE: 'Inativo'
};
