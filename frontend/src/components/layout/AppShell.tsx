import { useEffect, useState } from 'react';
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router';
import {
  AppBar,
  Avatar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Menu,
  MenuItem,
  Toolbar,
  Tooltip,
  Typography,
  useColorScheme,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import LogoutIcon from '@mui/icons-material/Logout';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useAuth } from '@/auth/AuthContext';
import { StatusChip } from '@/components/StatusChip';
import { NAV_SECTIONS, pageTitleFor } from './nav';
import { SearchToNavigate } from './SearchToNavigate';

export const DRAWER_WIDTH = 240;

function NavContent({ onNavigate }: { onNavigate?: () => void }) {
  const { pathname } = useLocation();
  const { isAdmin } = useAuth();
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar sx={{ gap: 1.25, px: 2.5 }}>
        <Box sx={{ width: 32, height: 32, borderRadius: 2, bgcolor: 'primary.main', color: 'primary.contrastText', display: 'grid', placeItems: 'center' }}>
          <NotificationsIcon sx={{ fontSize: 20 }} />
        </Box>
        <Box sx={{ lineHeight: 1 }}>
          <Typography variant="subtitle1" sx={{ lineHeight: 1.2 }}>
            TW Notification
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Admin console
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <Box sx={{ flex: 1, overflowY: 'auto', py: 1 }}>
        {NAV_SECTIONS.map((section, si) => (
          <List
            key={si}
            dense
            disablePadding
            subheader={
              section.title ? (
                <ListSubheader disableSticky sx={{ bgcolor: 'transparent', lineHeight: '32px', fontSize: 11, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'text.secondary', px: 3 }}>
                  {section.title}
                </ListSubheader>
              ) : undefined
            }
            sx={{ mb: 1 }}
          >
            {section.items
              .filter((i) => !i.adminOnly || isAdmin)
              .map((item) => {
                const selected = item.path === '/' ? pathname === '/' : pathname.startsWith(item.path);
                return (
                  <ListItemButton key={item.path} component={RouterLink} to={item.path} selected={selected} onClick={onNavigate} sx={{ my: 0.25 }}>
                    <ListItemIcon>
                      <item.icon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontSize: 14, fontWeight: selected ? 500 : 400 } } }} />
                  </ListItemButton>
                );
              })}
          </List>
        ))}
      </Box>
      <Divider />
      <Typography variant="caption" color="text.secondary" sx={{ p: 2 }}>
        TrackingWorld · v{__APP_VERSION__}
      </Typography>
    </Box>
  );
}

export function AppShell() {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const { pathname } = useLocation();
  const { admin, logout } = useAuth();
  const navigate = useNavigate();
  const { mode, setMode } = useColorScheme();
  const [userAnchor, setUserAnchor] = useState<HTMLElement | null>(null);

  const isDark = mode === 'dark' || (mode === 'system' && theme.palette.mode === 'dark');
  const title = pageTitleFor(pathname);
  useEffect(() => {
    document.title = `${title} · TW Notification`;
  }, [title]);

  return (
    <Box sx={{ display: 'flex', height: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="fixed">
        <Toolbar sx={{ gap: 1.5 }}>
          {!isDesktop && (
            <IconButton edge="start" onClick={() => setMobileOpen(true)} aria-label="Open navigation" sx={{ flexShrink: 0 }}>
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="h6" component="div" sx={{ width: { md: DRAWER_WIDTH - 24 }, flexShrink: 0, display: { xs: 'none', sm: 'block' }, fontWeight: 400 }}>
            {title}
          </Typography>
          <Box sx={{ flex: 1, minWidth: 0, display: 'flex', justifyContent: 'center' }}>
            <SearchToNavigate />
          </Box>
          <Tooltip title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}>
            <IconButton onClick={() => setMode(isDark ? 'light' : 'dark')} aria-label="Toggle colour mode" sx={{ flexShrink: 0 }}>
              {isDark ? <LightModeOutlinedIcon /> : <DarkModeOutlinedIcon />}
            </IconButton>
          </Tooltip>
          {admin && <StatusChip value={admin.role} sx={{ display: { xs: 'none', sm: 'inline-flex' } }} />}
          <Tooltip title={admin?.username ?? ''}>
            <IconButton onClick={(e) => setUserAnchor(e.currentTarget)} aria-label="Account menu" sx={{ p: 0.5, flexShrink: 0 }}>
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main', fontSize: 14 }}>{admin?.username?.slice(0, 1).toUpperCase() ?? '?'}</Avatar>
            </IconButton>
          </Tooltip>
          <Menu open={!!userAnchor} anchorEl={userAnchor} onClose={() => setUserAnchor(null)} slotProps={{ paper: { sx: { minWidth: 220 } } }}>
            <Box sx={{ px: 2, py: 1.5 }}>
              <Typography variant="subtitle2">{admin?.username}</Typography>
              <Typography variant="caption" color="text.secondary">
                Role: {admin?.role}
              </Typography>
            </Box>
            <Divider />
            <MenuItem
              onClick={() => {
                setUserAnchor(null);
                logout();
                navigate('/login', { replace: true });
              }}
            >
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              Sign out
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        {isDesktop ? (
          <Drawer variant="permanent" open sx={{ zIndex: theme.zIndex.appBar - 1, '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box', bgcolor: 'background.paper', zIndex: theme.zIndex.appBar - 1 } }}>
            <NavContent />
          </Drawer>
        ) : (
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={() => setMobileOpen(false)}
            ModalProps={{ keepMounted: true }}
            sx={{ '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' } }}
          >
            <NavContent onNavigate={() => setMobileOpen(false)} />
          </Drawer>
        )}
      </Box>

      <Box component="main" sx={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
        <Toolbar />
        <Box sx={{ flex: 1, p: { xs: 2, md: 3 }, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'auto' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
