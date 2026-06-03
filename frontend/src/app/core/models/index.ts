// ==========================================
// TypeScript Interfaces for MSP Portal
// ==========================================

// --- User ---
export interface User {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  displayName?: string;
  role: 'ADMIN' | 'MERCHANT';
  contactNumber: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'INACTIVE' | 'PENDING';
  mfaEnabled: boolean;
  createdAt?: string;
}

export interface UserCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  contactNumber: string;
  displayName?: string;
  role: string;
  status: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: string;
  mfaEnabled: boolean;
  mfaRequired?: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// --- Merchant ---
export interface Merchant {
  merchantId: number;
  userId?: number;
  merchantName: string;
  contact?: string;
  addressLine1?: string;
  addressLine2?: string;
  postcode?: string;
  city?: string;
  country?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING';
}

export interface MerchantCreateRequest {
  merchantName: string;
  contact?: string;
  addressLine1?: string;
  addressLine2?: string;
  postcode?: string;
  city?: string;
  country?: string;
  status: string;
  userId?: number;
}

// --- Transaction ---
export interface Transaction {
  transactionId: number;
  merchantId: number;
  merchantName?: string;
  settlementId?: number;
  paymentChannel?: string;
  amount: number;
  currency: string;
  status: 'APPROVED' | 'PENDING' | 'DECLINED';
  refNo?: string;
  cardNo?: string;
  txnDate?: string;
  postedDate?: string;
  txnDescription?: string;
  discountAmount?: number;
  nettAmount?: number;
}

// --- Refund ---
export interface Refund {
  refundId: number;
  transactionId: number;
  merchantId: number;
  merchantName?: string;
  cardNo?: string;
  submissionDate?: string;
  postedDate?: string;
  currency: string;
  amount: number;
  refundType?: string;
  refundRefNo?: string;
  refundAmount?: number;
  transactionDate?: string;
  status: string;
}

// --- Settlement ---
export interface Settlement {
  settlementId: number;
  creditAdviceId?: number;
  merchantId?: number;
  merchantName?: string;
  settlementDate?: string;
  settlementNo?: string;
  settlementType?: string;
  currency: string;
  settlementAmount: number;
  paymentAmount?: number;
}

// --- Credit Advice ---
export interface CreditAdvice {
  creditAdviceId: number;
  merchantId: number;
  merchantName?: string;
  accountNo?: string;
  paymentDate?: string;
  currency: string;
  amount: number;
  accountId?: string;
}

// --- Role ---
export interface Role {
  roleId: number;
  roleName: string;
  description?: string;
  roleType?: string;
}

// --- Permission ---
export interface Permission {
  permissionId: number;
  permissionName: string;
  description?: string;
  module?: string;
}

// --- Dashboard ---
export interface DashboardStats {
  totalUsers?: number;
  totalMerchants: number;
  activeMerchants: number;
  pendingMerchants: number;
  totalTransactions: number;
  totalSettlements: number;
  recentUsers?: any[];
}

// --- Pagination ---
export interface PaginationState {
  currentPage: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
}


