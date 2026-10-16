import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import type { FilterClause, FilterLogic, FilterOp, SortClause } from '@/api/types';
import type { EntityConfig } from '@/entities/types';

export interface GridQueryState {
  page: number;
  size: number;
  sort: SortClause | null;
  filters: FilterClause[];
  logic: FilterLogic;
  search: string;
}

export const PAGE_SIZES = [25, 50, 100, 200] as const;
const DEFAULT_SIZE = 50;

const VALID_OPS: ReadonlySet<string> = new Set<FilterOp>([
  'eq', 'neq', 'contains', 'startsWith', 'gt', 'gte', 'lt', 'lte', 'between', 'in', 'isNull', 'notNull',
]);

export function parseFilters(raw: string | null): FilterClause[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((f): f is FilterClause => !!f && typeof f === 'object' && typeof (f as FilterClause).field === 'string' && VALID_OPS.has((f as FilterClause).op))
      .map((f) => ({ field: f.field, op: f.op, ...(f.value === undefined ? {} : { value: f.value }) }));
  } catch {
    return [];
  }
}

/** Build a shareable `?f=…` query string for a set of filters. */
export function filtersToSearch(filters: FilterClause[], extra?: Record<string, string>): string {
  const p = new URLSearchParams(extra);
  if (filters.length) p.set('f', JSON.stringify(filters));
  const s = p.toString();
  return s ? `?${s}` : '';
}

/**
 * Grid state lives in the URL so links are shareable and the back button works.
 * Keys: page, size, sort (field:dir), logic, f (JSON filters), q (quick search).
 */
export function useGridQueryState(entity: EntityConfig) {
  const [params, setParams] = useSearchParams();

  const state = useMemo<GridQueryState>(() => {
    const page = Math.max(0, Number(params.get('page') ?? 0) || 0);
    const sizeRaw = Number(params.get('size') ?? DEFAULT_SIZE) || DEFAULT_SIZE;
    const size = (PAGE_SIZES as readonly number[]).includes(sizeRaw) ? sizeRaw : DEFAULT_SIZE;
    const sortRaw = params.get('sort');
    let sort: SortClause | null = entity.defaultSort;
    if (sortRaw === 'none') sort = null;
    else if (sortRaw) {
      const [field, dir] = sortRaw.split(':');
      if (field) sort = { field, dir: dir === 'asc' ? 'asc' : 'desc' };
    }
    const logic: FilterLogic = params.get('logic') === 'OR' ? 'OR' : 'AND';
    return {
      page,
      size,
      sort,
      filters: parseFilters(params.get('f')),
      logic,
      search: params.get('q') ?? '',
    };
  }, [params, entity.defaultSort]);

  const update = useCallback(
    (patch: Partial<GridQueryState>, opts?: { resetPage?: boolean }) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          const merged = { ...state, ...patch };
          if (opts?.resetPage) merged.page = 0;
          const setOrDelete = (k: string, v: string | null) => (v ? next.set(k, v) : next.delete(k));
          setOrDelete('page', merged.page > 0 ? String(merged.page) : null);
          setOrDelete('size', merged.size !== DEFAULT_SIZE ? String(merged.size) : null);
          const isDefaultSort = merged.sort && merged.sort.field === entity.defaultSort.field && merged.sort.dir === entity.defaultSort.dir;
          setOrDelete('sort', merged.sort === null ? 'none' : isDefaultSort ? null : `${merged.sort.field}:${merged.sort.dir}`);
          setOrDelete('logic', merged.logic === 'OR' ? 'OR' : null);
          setOrDelete('f', merged.filters.length ? JSON.stringify(merged.filters) : null);
          setOrDelete('q', merged.search || null);
          return next;
        },
        { replace: true },
      );
    },
    [setParams, state, entity.defaultSort],
  );

  return { state, update } as const;
}
