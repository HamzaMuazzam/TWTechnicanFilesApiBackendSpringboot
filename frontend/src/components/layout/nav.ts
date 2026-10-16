import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import type { SvgIconComponent } from '@mui/icons-material';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import NotificationsActiveOutlinedIcon from '@mui/icons-material/NotificationsActiveOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline';
import PhoneAndroidOutlinedIcon from '@mui/icons-material/PhoneAndroidOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined';
import TerminalOutlinedIcon from '@mui/icons-material/TerminalOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';

export interface NavItem {
  label: string;
  path: string;
  icon: SvgIconComponent;
  adminOnly?: boolean;
}
export interface NavSection {
  title?: string;
  items: NavItem[];
}

export const NAV_SECTIONS: NavSection[] = [
  {
    items: [{ label: 'Overview', path: '/', icon: DashboardOutlinedIcon }],
  },
  {
    title: 'Pipeline',
    items: [
      { label: 'Configurations', path: '/configurations', icon: TuneOutlinedIcon },
      { label: 'Alerts', path: '/alerts', icon: NotificationsActiveOutlinedIcon },
      { label: 'Notifications', path: '/notifications', icon: SendOutlinedIcon },
      { label: 'Deliveries', path: '/deliveries', icon: LocalShippingOutlinedIcon },
      { label: 'Discarded', path: '/discarded', icon: BlockOutlinedIcon },
    ],
  },
  {
    title: 'Customers',
    items: [
      { label: 'Users', path: '/users', icon: PeopleOutlineIcon },
      { label: 'Devices', path: '/devices', icon: PhoneAndroidOutlinedIcon },
    ],
  },
  {
    title: 'Configuration',
    items: [
      { label: 'Ingest sources', path: '/sources', icon: KeyOutlinedIcon },
      { label: 'Alert types', path: '/alert-types', icon: CategoryOutlinedIcon },
      { label: 'Admins', path: '/admins', icon: AdminPanelSettingsOutlinedIcon, adminOnly: true },
      { label: 'SQL console', path: '/sql', icon: TerminalOutlinedIcon, adminOnly: true },
      { label: 'Settings', path: '/settings', icon: SettingsOutlinedIcon },
    ],
  },
];

export function pageTitleFor(pathname: string): string {
  for (const s of NAV_SECTIONS) {
    for (const i of s.items) {
      if (i.path === '/' ? pathname === '/' : pathname.startsWith(i.path)) return i.label;
    }
  }
  return 'TW Notification';
}
