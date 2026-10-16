import { ADMIN_ROLES } from '@/api/types';
import type { EntityConfig } from './types';

export const adminsEntity: EntityConfig = {
  name: 'admins',
  title: 'Admins',
  singular: 'admin',
  path: '/admins',
  idField: 'id',
  defaultSort: { field: 'id', dir: 'asc' },
  searchPlaceholder: 'Search username…',
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 80, render: 'mono' },
    { field: 'username', label: 'Username', type: 'text', flex: 1, minWidth: 160 },
    { field: 'role', label: 'Role', type: 'enum', options: ADMIN_ROLES, width: 110, render: 'chip' },
    { field: 'active', label: 'Active', type: 'boolean', width: 90, render: 'bool' },
    { field: 'createdAt', label: 'Created', type: 'datetime', width: 170, render: 'time' },
    { field: 'lastLoginAt', label: 'Last login', type: 'datetime', width: 170, render: 'time' },
  ],
};
