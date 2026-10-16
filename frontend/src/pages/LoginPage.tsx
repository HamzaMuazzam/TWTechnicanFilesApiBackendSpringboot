import { useState, type FormEvent } from 'react';
import { Alert, Box, Button, IconButton, InputAdornment, Paper, TextField, Typography, useColorScheme, useTheme } from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import { useAuth } from '@/auth/AuthContext';
import { friendlyError } from '@/api/errors';

export function LoginPage() {
  const { login } = useAuth();
  const theme = useTheme();
  const { mode, setMode } = useColorScheme();
  const isDark = mode === 'dark' || (mode === 'system' && theme.palette.mode === 'dark');

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError('Enter your username and password.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await login(username.trim(), password);
      // <AnonymousOnly> redirects to the page the user originally asked for.
    } catch (err) {
      setError(friendlyError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', bgcolor: 'background.default', p: 2, position: 'relative' }}>
      <IconButton onClick={() => setMode(isDark ? 'light' : 'dark')} aria-label="Toggle colour mode" sx={{ position: 'absolute', top: 16, right: 16 }}>
        {isDark ? <LightModeOutlinedIcon /> : <DarkModeOutlinedIcon />}
      </IconButton>
      <Paper component="form" onSubmit={submit} noValidate sx={{ width: '100%', maxWidth: 400, p: 4, borderRadius: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
          <Box sx={{ width: 40, height: 40, borderRadius: 2.5, bgcolor: 'primary.main', color: 'primary.contrastText', display: 'grid', placeItems: 'center' }}>
            <NotificationsIcon />
          </Box>
          <Box>
            <Typography variant="h5" sx={{ lineHeight: 1.2 }}>
              TW Notification
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Sign in to the admin console
            </Typography>
          </Box>
        </Box>

        {error && (
          <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TextField
          label="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          fullWidth
          autoFocus
          autoComplete="username"
          margin="normal"
          size="medium"
        />
        <TextField
          label="Password"
          type={showPw ? 'text' : 'password'}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          fullWidth
          autoComplete="current-password"
          margin="normal"
          size="medium"
          slotProps={{
            input: {
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowPw((s) => !s)} edge="end" aria-label={showPw ? 'Hide password' : 'Show password'}>
                    {showPw ? <VisibilityOffOutlinedIcon /> : <VisibilityOutlinedIcon />}
                  </IconButton>
                </InputAdornment>
              ),
            },
          }}
        />
        <Button type="submit" variant="contained" fullWidth size="large" loading={busy} sx={{ mt: 3, borderRadius: 20 }}>
          Sign in
        </Button>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 3, textAlign: 'center' }}>
          TrackingWorld · internal use only
        </Typography>
      </Paper>
    </Box>
  );
}
