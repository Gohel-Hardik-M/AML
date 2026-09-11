import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { AlertsPagedComponent } from './components/alerts-paged/alerts-paged.component';
import { OfficersComponent } from './components/officers/officers.component';
import { RulesComponent } from './components/rules/rules.component';
import { UploadComponent } from './components/upload/upload.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { roleGuard } from './role.guard';
import { MasterComponent } from './components/master/master.component';
import { guestGuard } from './guest.guard';
import { ActivityComponent } from './components/activity/activity.component';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent, canActivate: [guestGuard] },
	{ path: 'reset-password', component: ResetPasswordComponent, canActivate: [authGuard] },
	{ path: 'dashboard', component: DashboardComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TENANT_ADMIN', 'COMPLIANCE_OFFICER'], redirectTo: '/master' } },
	{ path: 'alerts', component: AlertsPagedComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TENANT_ADMIN', 'COMPLIANCE_OFFICER'] } },
	{ path: 'officers', component: OfficersComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TENANT_ADMIN'] } },
	{ path: 'rules', component: RulesComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TENANT_ADMIN'] } },
	{ path: 'activity', component: ActivityComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TENANT_ADMIN'] } },
	{ path: 'upload', component: UploadComponent, canActivate: [authGuard, roleGuard], data: { roles: ['TENANT_ADMIN'] } },
	{ path: 'master', component: MasterComponent, canActivate: [authGuard, roleGuard], data: { roles: ['SYSTEM_ADMIN'] } },
	{ path: '', pathMatch: 'full', redirectTo: 'dashboard' },
	{ path: '**', redirectTo: 'dashboard' }
];
