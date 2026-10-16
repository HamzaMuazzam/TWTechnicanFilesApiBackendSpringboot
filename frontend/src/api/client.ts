import axios, { type AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { ApiError } from './errors';
import type { AdminAuthResponse, ApiEnvelope } from './types';
import { tokenStore } from '@/auth/tokenStore';

export const API_BASE = '/api/v1/admin';

/**
 * Page index convention sent in FilterRequest.page.
 * Spring `Pageable` is 0-based; flip to 1 if the backend decides otherwise.
 */
export const PAGE_BASE = 0;

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** internal: request already replayed once after a refresh */
    _retried?: boolean;
    /** set on auth endpoints so a 401 is not treated as "refresh me" */
    _noAuthRetry?: boolean;
    /** skip envelope unwrapping (blob downloads) */
    _raw?: boolean;
  }
}
type RetryableConfig = InternalAxiosRequestConfig;

export const http = axios.create({
  baseURL: API_BASE,
  timeout: 30_000,
  headers: { Accept: 'application/json' },
});

/** Bare client used for the refresh call so it never recurses through interceptors. */
const refreshClient = axios.create({ baseURL: API_BASE, timeout: 15_000 });

function isEnvelope(x: unknown): x is ApiEnvelope<unknown> {
  return !!x && typeof x === 'object' && 'success' in x && 'data' in x;
}

// ---- request: attach bearer ----
http.interceptors.request.use((config) => {
  const token = tokenStore.getAccess();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ---- refresh (single-flight) ----
let refreshing: Promise<string> | null = null;

export async function refreshAccessToken(): Promise<string> {
  if (refreshing) return refreshing;
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) throw new ApiError('NO_REFRESH_TOKEN', 401);
  refreshing = (async () => {
    try {
      const res = await refreshClient.post<ApiEnvelope<AdminAuthResponse>>('/auth/refresh', { refreshToken });
      const body = res.data;
      if (!isEnvelope(body) || !body.success) {
        throw new ApiError(body?.message ?? 'INVALID_TOKEN', 401);
      }
      tokenStore.setAccess(body.data.accessToken);
      tokenStore.setRefresh(body.data.refreshToken);
      return body.data.accessToken;
    } catch (e) {
      tokenStore.clear();
      throw e;
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}

async function blobToApiError(blob: Blob, status: number): Promise<ApiError> {
  try {
    const text = await blob.text();
    const json = JSON.parse(text) as unknown;
    if (isEnvelope(json)) return new ApiError(String(json.message || 'ERROR'), status);
  } catch {
    /* not JSON */
  }
  return new ApiError(`HTTP_${status}`, status);
}

// ---- response: unwrap envelope, normalise errors, refresh on 401 ----
http.interceptors.response.use(
  (response) => {
    const cfg = response.config as RetryableConfig;
    if (cfg._raw || cfg.responseType === 'blob') return response;
    const body: unknown = response.data;
    if (isEnvelope(body)) {
      if (!body.success) throw new ApiError(String(body.message || 'ERROR'), response.status);
      response.data = body.data;
    }
    return response;
  },
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) throw error;
    const axErr = error as AxiosError<unknown>;
    const cfg = axErr.config as RetryableConfig | undefined;
    const status = axErr.response?.status;

    // 401 → try one refresh, then replay the original request
    if (status === 401 && cfg && !cfg._retried && !cfg._noAuthRetry) {
      cfg._retried = true;
      try {
        const token = await refreshAccessToken();
        cfg.headers.Authorization = `Bearer ${token}`;
        return http.request(cfg);
      } catch {
        tokenStore.clear();
        throw new ApiError('TOKEN_EXPIRED', 401);
      }
    }

    if (axErr.response) {
      const data: unknown = axErr.response.data;
      if (data instanceof Blob) throw await blobToApiError(data, status ?? 0);
      if (isEnvelope(data)) throw new ApiError(String(data.message || `HTTP_${status}`), status);
      if (data && typeof data === 'object' && 'message' in data) {
        throw new ApiError(String((data as { message: unknown }).message), status);
      }
      throw new ApiError(`HTTP_${status}`, status);
    }
    if (axErr.code === 'ECONNABORTED') throw new ApiError('TIMEOUT');
    throw new ApiError('NETWORK_ERROR');
  },
);

/** Typed helpers — the interceptor has already unwrapped `data`. */
export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.get<T>(url, config);
  return res.data;
}
export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.post<T>(url, body, config);
  return res.data;
}
export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.put<T>(url, body, config);
  return res.data;
}
export async function patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.patch<T>(url, body, config);
  return res.data;
}
export async function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.delete<T>(url, config);
  return res.data;
}

/** Marks a request as an auth endpoint (no refresh-retry on 401). */
export const noAuthRetry: AxiosRequestConfig = { _noAuthRetry: true };
