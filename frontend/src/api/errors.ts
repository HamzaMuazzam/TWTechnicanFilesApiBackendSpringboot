export class ApiError extends Error {
  readonly code: string;
  readonly status: number | undefined;

  constructor(code: string, status?: number, detail?: string) {
    super(detail ?? code);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

const FRIENDLY: Record<string, string> = {
  INVALID_CREDENTIALS: 'Incorrect username or password.',
  BAD_CREDENTIALS: 'Incorrect username or password.',
  INVALID_TOKEN: 'Your session is invalid. Please sign in again.',
  TOKEN_EXPIRED: 'Your session has expired. Please sign in again.',
  UNAUTHORIZED: 'You need to sign in to do that.',
  FORBIDDEN: 'You do not have permission to do that.',
  ACCESS_DENIED: 'You do not have permission to do that.',
  ADMIN_ROLE_REQUIRED: 'This action requires the ADMIN role.',
  USER_DISABLED: 'This user is disabled.',
  NOT_FOUND: 'The requested item was not found.',
  UNKNOWN_FIELD: 'One of the filter or sort fields is not allowed for this entity.',
  VALIDATION_ERROR: 'Some of the values are invalid.',
  DUPLICATE: 'An item with the same identifier already exists.',
  CONFLICT: 'An item with the same identifier already exists.',
  RATE_LIMITED: 'Too many requests — slow down and try again.',
  SQL_NOT_ALLOWED: 'Only a single read-only SELECT / WITH statement is allowed.',
  SQL_ERROR: 'The database rejected the query.',
  NETWORK_ERROR: 'Cannot reach the server. Check your connection.',
  TIMEOUT: 'The server took too long to respond.',
  INTERNAL_ERROR: 'Something went wrong on the server.',
};

const BY_STATUS: Record<number, string> = {
  400: 'The request was rejected as invalid.',
  401: 'You need to sign in to do that.',
  403: 'You do not have permission to do that.',
  404: 'The requested item was not found.',
  409: 'An item with the same identifier already exists.',
  429: 'Too many requests — slow down and try again.',
  500: 'Something went wrong on the server.',
  502: 'The server is unreachable (bad gateway).',
  503: 'The server is temporarily unavailable.',
};

/** Human-readable message for any thrown error. */
export function friendlyError(err: unknown): string {
  if (err instanceof ApiError) {
    const known = FRIENDLY[err.code];
    if (known) return known;
    if (err.status && BY_STATUS[err.status]) return `${BY_STATUS[err.status]} (${err.code})`;
    // Humanise SOME_ERROR_CODE → "Some error code"
    if (/^[A-Z0-9_]+$/.test(err.code)) {
      const words = err.code.toLowerCase().replace(/_/g, ' ');
      return words.charAt(0).toUpperCase() + words.slice(1) + '.';
    }
    return err.message || err.code;
  }
  if (err instanceof Error) return err.message;
  return String(err);
}

export function errorCode(err: unknown): string | undefined {
  return err instanceof ApiError ? err.code : undefined;
}
