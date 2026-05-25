export interface UserRequest {
  username: string;
  password?: string | null;
  email?: string;
  fullName?: string;
  departmentId?: string;
  roleIds?: string[];
  active: boolean;
}

export interface UserDto {
  id?: string;
  username: string;
  email?: string;
  fullName?: string;
  departmentId?: string;
  roleIds?: string[];
  active?: boolean;
}

export interface DepartmentDto {
  id?: string;
  name: string;
  description?: string;
  managerId?: string;
  status?: string;
}
