import type { EntityConfig } from './types';

export const configurationsEntity: EntityConfig = {
  name: 'configurations',
  title: 'Configurations',
  singular: 'configuration',
  path: '/configurations',
  idField: 'id',
  defaultSort: { field: 'id', dir: 'desc' },
  searchPlaceholder: 'Search uid, phone, S3 id…',
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 80, render: 'mono' },
    { field: 'sourceId', label: 'S3 id', type: 'text', width: 110, render: 'mono' },
    { field: 'uid', label: 'UID (tracker)', type: 'text', width: 140, render: 'mono' },
    { field: 'alertType', label: 'Alert type', type: 'text', width: 150, render: 'chip', hint: '* = all types' },
    { field: 'phoneE164', label: 'Phone', type: 'text', width: 160, render: 'mono' },
    { field: 'phoneRaw', label: 'Phone (as sent)', type: 'text', width: 150, render: 'mono', hidden: true },
    { field: 'crtBy', label: 'Created by', type: 'text', width: 120 },
    { field: 'dbdt', label: 'S3 time', type: 'datetime', width: 170, render: 'time' },
    { field: 'updatedAt', label: 'Updated', type: 'datetime', width: 170, render: 'time' },
  ],
};
