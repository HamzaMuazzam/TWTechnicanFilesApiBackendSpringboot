import type { EntityConfig } from './types';

export const sourcesEntity: EntityConfig = {
  name: 'sources',
  title: 'Ingest sources',
  singular: 'ingest source',
  path: '/sources',
  idField: 'id',
  defaultSort: { field: 'id', dir: 'asc' },
  searchPlaceholder: 'Search name…',
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 80, render: 'mono' },
    { field: 'name', label: 'Name', type: 'text', flex: 1, minWidth: 160 },
    { field: 'apiKeyPrefix', label: 'Key prefix', type: 'text', width: 130, render: 'mono' },
    { field: 'active', label: 'Active', type: 'boolean', width: 90, render: 'bool' },
    { field: 'createdAt', label: 'Created', type: 'datetime', width: 170, render: 'time' },
    { field: 'lastUsedAt', label: 'Last used', type: 'datetime', width: 170, render: 'time' },
  ],
};
