import type { EntityName } from '@/api/types';
import { configurationsEntity } from './configurations';
import { alertsEntity } from './alerts';
import { notificationsEntity } from './notifications';
import { deliveriesEntity } from './deliveries';
import { usersEntity } from './users';
import { devicesEntity } from './devices';
import { discardsEntity } from './discards';
import { sourcesEntity } from './sources';
import { alertTypesEntity } from './alertTypes';
import { adminsEntity } from './admins';
import type { EntityConfig } from './types';

export const ENTITIES: Record<EntityName, EntityConfig> = {
  configurations: configurationsEntity,
  alerts: alertsEntity,
  notifications: notificationsEntity,
  deliveries: deliveriesEntity,
  users: usersEntity,
  devices: devicesEntity,
  discards: discardsEntity,
  sources: sourcesEntity,
  'alert-types': alertTypesEntity,
  admins: adminsEntity,
};

export * from './types';
export { configurationsEntity, alertsEntity, notificationsEntity, deliveriesEntity, usersEntity, devicesEntity, discardsEntity, sourcesEntity, alertTypesEntity, adminsEntity };
