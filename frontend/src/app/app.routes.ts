import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MfaComponent } from './features/auth/mfa/mfa.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { DashboardComponent } from './features/dashboard/analytics-dashboard/analytics-dashboard.component';
import { UserListComponent } from './features/admin/user-list/user-list.component';
import { UserFormComponent } from './features/admin/user-form/user-form.component';
import { UserDetailComponent } from './features/admin/user-list/user-detail.component';
import { RoleListComponent } from './features/admin/role-list/role-list.component';
import { RoleFormComponent } from './features/admin/role-form/role-form.component';
import { RoleDetailComponent } from './features/admin/role-list/role-detail.component';
import { MerchantListComponent } from './features/inquiries/merchant-list/merchant-list.component';
import { MerchantDetailComponent } from './features/inquiries/merchant-detail/merchant-detail.component';
import { MerchantFormComponent } from './features/inquiries/merchant-form/merchant-form.component';
import { TransactionListComponent } from './features/inquiries/transaction-list/transaction-list.component';
import { TransactionDetailComponent } from './features/inquiries/transaction-detail/transaction-detail.component';
import { CreditAdviceListComponent } from './features/inquiries/credit-advice-list/credit-advice-list.component';
import { CreditAdviceDetailComponent } from './features/inquiries/credit-advice-detail/credit-advice-detail.component';
import { RefundListComponent } from './features/inquiries/refund-list/refund-list.component';
import { RefundDetailComponent } from './features/inquiries/refund-detail/refund-detail.component';
import { SettlementDetailComponent } from './features/inquiries/settlement-detail/settlement-detail.component';
import { ViewProfileComponent } from './features/profile/view-profile/view-profile.component';
import { EditProfileComponent } from './features/profile/edit-profile/edit-profile.component';
import { ChangePasswordComponent } from './features/profile/change-password/change-password.component';
import { authGuard } from './core/guards/auth-guard';
import { permissionGuard } from './core/guards/permission-guard';
import { LayoutComponent } from './shared/components/layout/layout.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'mfa', component: MfaComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'reset-password/:token', component: ResetPasswordComponent },

  { 
    path: '', 
    component: LayoutComponent,
    canActivate: [authGuard],
    runGuardsAndResolvers: 'always',
    children: [
      { path: 'dashboard', component: DashboardComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_DASHBOARD' }, runGuardsAndResolvers: 'always' },
      { path: 'ai-analytics', component: DashboardComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_DASHBOARD', tab: 'ai-analytics' }, runGuardsAndResolvers: 'always' },

      // User management - permission guarded
      { path: 'users', component: UserListComponent, canActivate: [permissionGuard], data: { permissions: ['MANAGE_USERS', 'MANAGE_CHILD_USERS'] }, runGuardsAndResolvers: 'always' },
      { path: 'users/new', component: UserFormComponent, canActivate: [permissionGuard], data: { permissions: ['MANAGE_USERS', 'MANAGE_CHILD_USERS'] }, runGuardsAndResolvers: 'always' },
      { path: 'users/:id/edit', component: UserFormComponent, canActivate: [permissionGuard], data: { permissions: ['MANAGE_USERS', 'MANAGE_CHILD_USERS'] }, runGuardsAndResolvers: 'always' },
      { path: 'users/:id/view', component: UserDetailComponent, canActivate: [permissionGuard], data: { permissions: ['MANAGE_USERS', 'MANAGE_CHILD_USERS'] }, runGuardsAndResolvers: 'always' },
      
      // Role management - permission guarded
      { path: 'roles', component: RoleListComponent, canActivate: [permissionGuard], data: { permission: 'MANAGE_ROLES' }, runGuardsAndResolvers: 'always' },
      { path: 'roles/new', component: RoleFormComponent, canActivate: [permissionGuard], data: { permission: 'MANAGE_ROLES' }, runGuardsAndResolvers: 'always' },
      { path: 'roles/:id/edit', component: RoleFormComponent, canActivate: [permissionGuard], data: { permission: 'MANAGE_ROLES' }, runGuardsAndResolvers: 'always' },
      { path: 'roles/:id/view', component: RoleDetailComponent, canActivate: [permissionGuard], data: { permission: 'MANAGE_ROLES' }, runGuardsAndResolvers: 'always' },

      // Merchant inquiry - permission guarded
      { path: 'merchants', component: MerchantListComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_OWN_DATA' }, runGuardsAndResolvers: 'always' },
      { path: 'merchants/:id', component: MerchantDetailComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_OWN_DATA' }, runGuardsAndResolvers: 'always' },

      // Transaction inquiry - permission guarded
      { path: 'transactions', component: TransactionListComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_TRANSACTIONS' }, runGuardsAndResolvers: 'always' },
      { path: 'transactions/:id', component: TransactionDetailComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_TRANSACTIONS' }, runGuardsAndResolvers: 'always' },

      // Credit Advice inquiry - permission guarded
      { path: 'credit-advices', component: CreditAdviceListComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_CREDIT_ADVICES' }, runGuardsAndResolvers: 'always' },
      { path: 'credit-advices/:id', component: CreditAdviceDetailComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_CREDIT_ADVICES' }, runGuardsAndResolvers: 'always' },

      // Refund inquiry - permission guarded
      { path: 'refunds', component: RefundListComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_REFUNDS' }, runGuardsAndResolvers: 'always' },
      { path: 'refunds/:id', component: RefundDetailComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_REFUNDS' }, runGuardsAndResolvers: 'always' },

      // Settlement detail - accessible from credit advice detail (no sidebar link)
      { path: 'settlements/:id', component: SettlementDetailComponent, canActivate: [permissionGuard], data: { permission: 'VIEW_CREDIT_ADVICES' }, runGuardsAndResolvers: 'always' },

      // Profile management - accessible by all authenticated users
      { path: 'profile', component: ViewProfileComponent, runGuardsAndResolvers: 'always' },
      { path: 'profile/edit', component: EditProfileComponent, runGuardsAndResolvers: 'always' },
      { path: 'profile/change-password', component: ChangePasswordComponent, runGuardsAndResolvers: 'always' },

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];