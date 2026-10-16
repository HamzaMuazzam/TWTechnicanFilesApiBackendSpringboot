import { Alert, AlertTitle, Button } from '@mui/material';
import { friendlyError, errorCode } from '@/api/errors';

interface ErrorStateProps {
  error: unknown;
  title?: string;
  onRetry?: () => void;
  compact?: boolean;
}

export function ErrorState({ error, title = 'Could not load data', onRetry, compact }: ErrorStateProps) {
  const code = errorCode(error);
  return (
    <Alert
      severity="error"
      variant="outlined"
      sx={{ m: compact ? 1 : 2, alignItems: 'center' }}
      action={
        onRetry ? (
          <Button color="inherit" size="small" onClick={onRetry}>
            Retry
          </Button>
        ) : undefined
      }
    >
      {!compact && <AlertTitle>{title}</AlertTitle>}
      {friendlyError(error)}
      {code && code !== friendlyError(error) ? ` (${code})` : ''}
    </Alert>
  );
}
