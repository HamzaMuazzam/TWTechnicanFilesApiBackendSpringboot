import type { EntityName, FilterOp, SortClause } from '@/api/types';

export type FieldType = 'text' | 'number' | 'datetime' | 'enum' | 'boolean';

/** How a column renders in the grid. Defaults are derived from `type`. */
export type FieldRender = 'text' | 'chip' | 'time' | 'bool' | 'mono' | 'number' | 'coord' | 'color';

export interface FieldMeta {
  /** Row property returned by POST /query/{entity} (flat, see backend Rows.java). */
  field: string;
  /** JPA path used for filter + sort when it differs from `field` (e.g. `user.phoneE164`). */
  filterField?: string;
  label: string;
  type: FieldType;
  /** static enum options */
  options?: readonly string[];
  /** dynamic enum options loaded from the API */
  enumSource?: 'alertTypes';
  /** can be used in the Filter Builder (default true) */
  filterable?: boolean;
  /** can be used for server-side sort (default true) */
  sortable?: boolean;
  /** hidden from the grid until the user shows it via the column chooser */
  hidden?: boolean;
  width?: number;
  minWidth?: number;
  flex?: number;
  render?: FieldRender;
  /** optional short description shown in the Filter Builder field picker */
  hint?: string;
}

export interface EntityConfig {
  name: EntityName;
  /** page title */
  title: string;
  /** singular noun used in messages */
  singular: string;
  /** route under /admin */
  path: string;
  /** property that uniquely identifies a row */
  idField: string;
  fields: FieldMeta[];
  defaultSort: SortClause;
  searchPlaceholder: string;
  /** rows open a detail drawer */
  hasDetail?: boolean;
}

export const OPERATORS_BY_TYPE: Record<FieldType, FilterOp[]> = {
  text: ['contains', 'eq', 'neq', 'startsWith', 'in', 'isNull', 'notNull'],
  number: ['eq', 'neq', 'gt', 'gte', 'lt', 'lte', 'between', 'in', 'isNull', 'notNull'],
  datetime: ['between', 'gte', 'lte', 'gt', 'lt', 'isNull', 'notNull'],
  enum: ['eq', 'neq', 'in', 'isNull', 'notNull'],
  boolean: ['eq', 'isNull', 'notNull'],
};

export const OPERATOR_LABELS: Record<FilterOp, string> = {
  eq: 'is',
  neq: 'is not',
  contains: 'contains',
  startsWith: 'starts with',
  gt: '>',
  gte: '≥',
  lt: '<',
  lte: '≤',
  between: 'between',
  in: 'is any of',
  isNull: 'is empty',
  notNull: 'is not empty',
};

/** Read a dot-path from a row (`user.phoneE164`). */
/** Server-side name of a field (filter / sort). */
export function apiField(meta: Pick<FieldMeta, 'field' | 'filterField'>): string {
  return meta.filterField ?? meta.field;
}

export function getPath(row: unknown, path: string): unknown {
  if (!row || typeof row !== 'object') return undefined;
  // Direct hit first (backend may flatten related columns)
  if (path in (row as Record<string, unknown>)) return (row as Record<string, unknown>)[path];
  return path.split('.').reduce<unknown>((acc, key) => {
    if (acc && typeof acc === 'object' && key in (acc as Record<string, unknown>)) {
      return (acc as Record<string, unknown>)[key];
    }
    return undefined;
  }, row);
}
