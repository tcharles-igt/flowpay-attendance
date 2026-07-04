import { Routes } from '@angular/router';
import { DashboardPageComponent } from './features/dashboard/pages/dashboard-page.component';
import { AttendanceManagementPageComponent } from './features/attendances/pages/attendance-management-page.component';
import { AttendantManagementPageComponent } from './features/attendants/pages/attendant-management-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'dashboard',
    component: DashboardPageComponent
  },
  {
    path: 'atendimentos',
    component: AttendanceManagementPageComponent
  },
  {
    path: 'atendentes',
    component: AttendantManagementPageComponent
  }
];
