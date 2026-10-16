import type { EntityConfig } from './types';

export const devicesEntity: EntityConfig = {
  name: 'devices',
  title: 'Devices',
  singular: 'device',
  path: '/devices',
  idField: 'id',
  defaultSort: { field: 'lastSeenAt', dir: 'desc' },
  searchPlaceholder: 'Search phone, model, device id…',
  fields: [
    { field: 'id', label: 'ID', type: 'number', width: 80, render: 'mono' },
    { field: 'userPhone', filterField: 'user.phoneE164', label: 'Phone', type: 'text', width: 150, render: 'mono' },
    { field: 'manufacturer', label: 'Manufacturer', type: 'text', width: 120 },
    { field: 'model', label: 'Model', type: 'text', width: 150 },
    { field: 'osVersion', label: 'OS', type: 'text', width: 80 },
    { field: 'appVersion', label: 'App', type: 'text', width: 90 },
    { field: 'active', label: 'Active', type: 'boolean', width: 80, render: 'bool' },
    { field: 'lastSeenAt', label: 'Last seen', type: 'datetime', width: 170, render: 'time' },
    { field: 'batteryOptIgnored', label: 'Battery opt. ignored', type: 'boolean', width: 150, render: 'bool' },
    { field: 'unusedRestrictionsDisabled', label: 'Unused restr. off', type: 'boolean', width: 140, render: 'bool' },
    { field: 'notificationsEnabled', label: 'Notif. enabled', type: 'boolean', width: 120, render: 'bool' },
    { field: 'hasFcmToken', label: 'Has token', type: 'boolean', width: 100, render: 'bool', filterable: false, sortable: false },
    { field: 'deviceId', label: 'Device id', type: 'text', width: 290, render: 'mono', hidden: true },
  ],
};
