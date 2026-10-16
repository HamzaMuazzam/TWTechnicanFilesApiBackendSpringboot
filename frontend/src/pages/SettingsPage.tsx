import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Box, Button, Paper, Skeleton, TextField, Typography } from '@mui/material';
import { z } from 'zod';
import { settingsApi } from '@/api/endpoints';
import { queryKeys } from '@/api/hooks';
import type { Settings } from '@/api/types';
import { PageHeader } from '@/components/PageHeader';
import { ErrorState } from '@/components/ErrorState';
import { AdminOnly } from '@/components/AdminOnly';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useAuth } from '@/auth/AuthContext';

const schema = z.object({
  retention_days: z.number().int('Whole number').min(1, 'At least 1 day').max(3650, 'At most 3650 days'),
  max_devices_per_user: z.number().int('Whole number').min(1, 'At least 1').max(50, 'At most 50'),
  dispatch_max_attempts: z.number().int('Whole number').min(1, 'At least 1').max(20, 'At most 20'),
  severity_keywords_critical: z.string().max(500, 'Too long'),
  severity_keywords_warning: z.string().max(500, 'Too long'),
});

type Form = Record<keyof Settings, string>;
const toForm = (s: Settings): Form => ({
  retention_days: String(s.retention_days),
  max_devices_per_user: String(s.max_devices_per_user),
  dispatch_max_attempts: String(s.dispatch_max_attempts),
  severity_keywords_critical: s.severity_keywords_critical,
  severity_keywords_warning: s.severity_keywords_warning,
});

const FIELDS: Array<{ key: keyof Settings; label: string; help: string; unit?: string; kind: 'number' | 'keywords' }> = [
  { key: 'retention_days', label: 'Retention', help: 'Alerts older than this are deleted nightly at 03:00 Asia/Karachi (cascades to notifications, deliveries and discards).', unit: 'days', kind: 'number' },
  { key: 'max_devices_per_user', label: 'Max devices per user', help: 'When a user signs in on more devices than this, the oldest device is deactivated.', unit: 'devices', kind: 'number' },
  { key: 'dispatch_max_attempts', label: 'Dispatch max attempts', help: 'FCM sends are retried with exponential back-off (30 s × 2ⁿ) up to this many attempts before a delivery is marked FAILED.', unit: 'attempts', kind: 'number' },
  { key: 'severity_keywords_critical', label: 'Critical keywords', help: 'Alert types are discovered automatically the first time S3 sends a new alertType code. A new code containing any of these keywords (comma-separated, case-insensitive) starts as CRITICAL.', kind: 'keywords' },
  { key: 'severity_keywords_warning', label: 'Warning keywords', help: 'A new code containing any of these starts as WARNING; everything else starts as INFO. You can always change a type afterwards on the Alert types page.', kind: 'keywords' },
];

export function SettingsPage() {
  const snackbar = useSnackbar();
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();
  const q = useQuery({ queryKey: queryKeys.settings, queryFn: settingsApi.get });
  const [form, setForm] = useState<Form | null>(null);
  const [errors, setErrors] = useState<Partial<Record<keyof Settings, string>>>({});

  // Seed the form once the settings have loaded.
  if (q.data && form === null) setForm(toForm(q.data));

  const save = useMutation({
    mutationFn: (s: Settings) => settingsApi.put(s),
    onSuccess: (saved) => {
      snackbar.success('Settings saved');
      queryClient.setQueryData(queryKeys.settings, saved);
      setForm(toForm(saved));
    },
    onError: (e) => snackbar.error(e, 'Save failed'),
  });

  const parsed = form
    ? schema.safeParse({
        retention_days: form.retention_days === '' ? NaN : Number(form.retention_days),
        max_devices_per_user: form.max_devices_per_user === '' ? NaN : Number(form.max_devices_per_user),
        dispatch_max_attempts: form.dispatch_max_attempts === '' ? NaN : Number(form.dispatch_max_attempts),
        severity_keywords_critical: form.severity_keywords_critical,
        severity_keywords_warning: form.severity_keywords_warning,
      })
    : null;

  const dirty = !!form && !!q.data && JSON.stringify(form) !== JSON.stringify(toForm(q.data));

  const submit = () => {
    if (!parsed) return;
    if (!parsed.success) {
      const next: Partial<Record<keyof Settings, string>> = {};
      for (const issue of parsed.error.issues) {
        const k = issue.path[0] as keyof Settings;
        if (!next[k]) next[k] = issue.message === 'Invalid input: expected number, received NaN' ? 'Required' : issue.message;
      }
      setErrors(next);
      return;
    }
    setErrors({});
    save.mutate(parsed.data);
  };

  return (
    <Box>
      <PageHeader title="Settings" subtitle="Runtime settings stored in app_setting. Changes apply immediately; no restart needed." />
      {q.isError && <ErrorState error={q.error} onRetry={() => q.refetch()} />}
      <Paper sx={{ maxWidth: 720 }}>
        {!form ? (
          <Box sx={{ p: 3 }}>
            <Skeleton height={56} />
            <Skeleton height={56} />
            <Skeleton height={56} />
          </Box>
        ) : (
          <Box
            component="form"
            noValidate
            onSubmit={(e) => {
              e.preventDefault();
              submit();
            }}
          >
            {FIELDS.map((f, i) => (
              <Box key={f.key} sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 200px' }, gap: 2, alignItems: 'start', p: 3, borderTop: i ? '1px solid' : 'none', borderColor: 'divider' }}>
                <Box>
                  <Typography variant="subtitle2">{f.label}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    {f.help}
                  </Typography>
                </Box>
                <TextField
                  type={f.kind === 'number' ? 'number' : 'text'}
                  multiline={f.kind === 'keywords'}
                  minRows={f.kind === 'keywords' ? 2 : undefined}
                  placeholder={f.kind === 'keywords' ? 'SOS, PANIC, POWER' : undefined}
                  value={form[f.key]}
                  onChange={(e) => {
                    setForm({ ...form, [f.key]: e.target.value });
                    setErrors((er) => ({ ...er, [f.key]: undefined }));
                  }}
                  error={!!errors[f.key]}
                  helperText={errors[f.key]}
                  disabled={!isAdmin}
                  slotProps={
                    f.kind === 'number'
                      ? { input: { endAdornment: <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>{f.unit}</Typography> }, htmlInput: { min: 1, step: 1 } }
                      : { htmlInput: { style: { fontFamily: 'monospace', fontSize: 13 } } }
                  }
                  fullWidth
                />
              </Box>
            ))}
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, p: 2, borderTop: '1px solid', borderColor: 'divider', bgcolor: 'background.default', borderRadius: '0 0 12px 12px' }}>
              <Button disabled={!dirty || save.isPending} onClick={() => q.data && setForm(toForm(q.data))}>
                Reset
              </Button>
              <AdminOnly>
                <Button type="submit" variant="contained" disabled={!dirty} loading={save.isPending}>
                  Save changes
                </Button>
              </AdminOnly>
            </Box>
          </Box>
        )}
      </Paper>
    </Box>
  );
}
