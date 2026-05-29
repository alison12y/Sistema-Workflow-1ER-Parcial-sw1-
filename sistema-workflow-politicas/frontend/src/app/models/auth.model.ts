export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  fullName: string;
  role?: string;
  roleName?: string;
  roles?: string[];
  permissions?: string[];
}

export interface User {
  id?: string;
  username: string;
  email?: string;
  fullName?: string;
  departmentId?: string;
  roleIds?: string[];
  active?: boolean;
}

export interface Role {
  id?: string;
  name: string;
  description?: string;
  permissionIds?: string[];
  active?: boolean;
}

export interface Department {
  id?: string;
  name: string;
  description?: string;
  managerId?: string;
}

export interface BusinessPolicy {
  id?: string;
  name: string;
  description?: string;
  type?: string;
  status?: string;
  createdBy?: string;
  responsible?: string;
  version?: string;
  createdAt?: string;
  updatedAt?: string;
}
