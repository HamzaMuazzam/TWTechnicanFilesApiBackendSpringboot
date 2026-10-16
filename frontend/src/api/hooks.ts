import { useQuery } from '@tanstack/react-query';
import { alertTypesApi } from './endpoints';
import type { AlertTypeRow } from './types';

export const queryKeys = {
  alertTypes: ['alert-types'] as const,
  entity: (name: string) => ['query', name] as const,
  alertDetail: (id: number) => ['alert', id] as const,
  userDetail: (id: number) => ['user', id] as const,
  overview: (from: string, to: string) => ['overview', from, to] as const,
  dispatch: ['dispatch-status'] as const,
  settings: ['settings'] as const,
};

/** Alert types are used all over (filters, chips, charts) — cache them for a while. */
export function useAlertTypes() {
  return useQuery<AlertTypeRow[]>({
    queryKey: queryKeys.alertTypes,
    queryFn: alertTypesApi.list,
    staleTime: 5 * 60_000,
  });
}

export function useAlertTypeLabel(): (code: string | null | undefined) => string {
  const { data } = useAlertTypes();
  return (code) => {
    if (!code) return '—';
    return data?.find((t) => t.code === code)?.label ?? code;
  };
}
