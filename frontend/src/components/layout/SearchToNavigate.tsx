import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router';
import { Box, InputBase, List, ListItemButton, ListItemIcon, ListItemText, Paper, Popper, Typography, alpha, useTheme } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import NotificationsActiveOutlinedIcon from '@mui/icons-material/NotificationsActiveOutlined';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import type { FilterClause } from '@/api/types';
import { filtersToSearch } from '@/components/datagrid/useGridQueryState';
import type { SvgIconComponent } from '@mui/icons-material';

interface Target {
  key: string;
  label: string;
  hint: string;
  icon: SvgIconComponent;
  to: string;
}

/** Guess what the user typed and offer the grids that can show it, filter prefilled. */
function targetsFor(raw: string): Target[] {
  const q = raw.trim();
  if (!q) return [];
  const out: Target[] = [];
  const digitsOnly = q.replace(/[\s()-]/g, '');
  const isInt = /^\d+$/.test(q);
  const looksPhone = /^\+?\d{7,15}$/.test(digitsOnly) && !(isInt && q.length <= 6);

  const go = (path: string, filters: FilterClause[]) => `${path}${filtersToSearch(filters)}`;

  if (isInt && q.length <= 12) {
    out.push({ key: 'alert', label: `Alert #${q}`, hint: 'Open alert by id', icon: NotificationsActiveOutlinedIcon, to: go('/alerts', [{ field: 'id', op: 'eq', value: Number(q) }]) });
    out.push({ key: 'notif', label: `Notification #${q}`, hint: 'Open notification by id', icon: SendOutlinedIcon, to: go('/notifications', [{ field: 'id', op: 'eq', value: Number(q) }]) });
  }
  if (looksPhone) {
    const tail = digitsOnly.replace(/^\+?92|^0/, '');
    out.push({ key: 'phone-user', label: `Users with phone …${tail}`, hint: 'phoneE164 contains', icon: PersonOutlineIcon, to: go('/users', [{ field: 'phoneE164', op: 'contains', value: tail }]) });
    out.push({ key: 'phone-notif', label: `Notifications for …${tail}`, hint: 'phone contains', icon: SendOutlinedIcon, to: go('/notifications', [{ field: 'userPhone', op: 'contains', value: tail }]) });
  }
  out.push({ key: 'uid', label: `Alerts for UID “${q}”`, hint: 'uid is', icon: DirectionsCarFilledOutlinedIcon, to: go('/alerts', [{ field: 'uid', op: 'eq', value: q }]) });
  if (!looksPhone && !isInt) {
    out.push({ key: 'msg', label: `Alerts mentioning “${q}”`, hint: 'message contains', icon: NotificationsActiveOutlinedIcon, to: go('/alerts', [{ field: 'message', op: 'contains', value: q }]) });
  }
  return out;
}

export function SearchToNavigate() {
  const theme = useTheme();
  const navigate = useNavigate();
  const [value, setValue] = useState('');
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const [anchorEl, setAnchorEl] = useState<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const targets = useMemo(() => targetsFor(value), [value]);

  // "/" focuses the search box from anywhere (unless typing in a field).
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = document.activeElement as HTMLElement | null;
      const typing = el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable);
      if (e.key === '/' && !typing) {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const pick = (t: Target) => {
    navigate(t.to);
    setValue('');
    setOpen(false);
    inputRef.current?.blur();
  };

  return (
    <Box ref={setAnchorEl} sx={{ position: 'relative', width: '100%', maxWidth: { xs: '100%', sm: 320, md: 420 }, minWidth: 0 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          px: 1.5,
          height: 40,
          borderRadius: 20,
          bgcolor: theme.palette.mode === 'light' ? theme.palette.surfaceContainer.main : theme.palette.surfaceContainer.high,
          '&:focus-within': { bgcolor: 'background.paper', boxShadow: `0 0 0 2px ${alpha(theme.palette.primary.main, 0.5)}` },
        }}
      >
        <SearchIcon fontSize="small" sx={{ color: 'text.secondary' }} />
        <InputBase
          inputRef={inputRef}
          placeholder="Jump to alert id, phone or UID…"
          value={value}
          fullWidth
          inputProps={{ 'aria-label': 'Search to navigate', autoComplete: 'off' }}
          onChange={(e) => {
            setValue(e.target.value);
            setOpen(true);
            setActive(0);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          onKeyDown={(e) => {
            if (e.key === 'ArrowDown') {
              e.preventDefault();
              setActive((a) => Math.min(a + 1, targets.length - 1));
            } else if (e.key === 'ArrowUp') {
              e.preventDefault();
              setActive((a) => Math.max(a - 1, 0));
            } else if (e.key === 'Enter' && targets[active]) {
              pick(targets[active]);
            } else if (e.key === 'Escape') {
              setValue('');
              setOpen(false);
              inputRef.current?.blur();
            }
          }}
          sx={{ fontSize: 14 }}
        />
        <Typography variant="caption" sx={{ color: 'text.disabled', border: '1px solid', borderColor: 'divider', borderRadius: 1, px: 0.6, display: { xs: 'none', md: 'block' } }}>
          /
        </Typography>
      </Box>
      <Popper open={open && targets.length > 0} anchorEl={anchorEl} placement="bottom-start" sx={{ zIndex: theme.zIndex.modal + 1, width: anchorEl?.clientWidth }}>
        <Paper sx={{ mt: 0.5, overflow: 'hidden' }}>
          <List dense disablePadding>
            {targets.map((t, i) => (
              <ListItemButton key={t.key} selected={i === active} onMouseDown={(e) => e.preventDefault()} onClick={() => pick(t)} sx={{ borderRadius: 0, mx: 0 }}>
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <t.icon fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={t.label} secondary={t.hint} slotProps={{ secondary: { sx: { fontFamily: 'monospace', fontSize: 11 } } }} />
              </ListItemButton>
            ))}
          </List>
        </Paper>
      </Popper>
    </Box>
  );
}
