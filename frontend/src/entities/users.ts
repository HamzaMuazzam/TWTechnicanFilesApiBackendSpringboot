import { USER_STATUSES } from '@/api/types';
import type { EntityConfig } from './types';

export const usersEntity: EntityConfig = {
  name: 'users',
  title: 'Users',
  singular: 'user',
  path: '/users',
  idField: 'id',
  defaultSort: { field: 'createdAt', dir: 'desc' },
  searchPlaceholder: 'Search phone, name…',
  hasDetail: true,
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 90, render: 'mono' },
    { field: 'phoneE164', label: 'Phone', type: 'text', width: 160, render: 'mono' },
    { field: 'displayName', label: 'Name', type: 'text', flex: 1, minWidth: 160 },
    { field: 'status', label: 'Status', type: 'enum', options: USER_STATUSES, width: 110, render: 'chip' },
    { field: 'createdAt', label: 'Created', type: 'datetime', width: 170, render: 'time' },
    { field: 'lastLoginAt', label: 'Last login', type: 'datetime', width: 170, render: 'time' },
  ],
};
