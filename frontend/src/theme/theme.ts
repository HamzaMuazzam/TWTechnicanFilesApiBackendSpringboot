import { createTheme } from '@mui/material/styles';

/** SPEC §8.4 palette tokens. */
export const light = {
  primary: '#0B57D0',
  onPrimary: '#FFFFFF',
  primaryContainer: '#D3E3FD',
  onPrimaryContainer: '#041E49',
  secondary: '#00639B',
  surface: '#FFFFFF',
  surfaceContainerLow: '#F8F9FA',
  surfaceContainer: '#F0F4F9',
  surfaceContainerHigh: '#E9EEF6',
  outline: '#747775',
  outlineVariant: '#C4C7C5',
  onSurface: '#1F1F1F',
  onSurfaceVariant: '#444746',
  error: '#B3261E',
} as const;

export const dark = {
  primary: '#A8C7FA',
  onPrimary: '#062E6F',
  primaryContainer: '#0842A0',
  onPrimaryContainer: '#D3E3FD',
  secondary: '#7FCFFF',
  surface: '#131314',
  surfaceContainerLow: '#1B1B1C',
  surfaceContainer: '#1E1F20',
  surfaceContainerHigh: '#282A2C',
  outline: '#8E918F',
  outlineVariant: '#444746',
  onSurface: '#E3E3E3',
  onSurfaceVariant: '#C4C7C5',
  error: '#F2B8B5',
} as const;

/** Severity colours (text/icon) + containers, per mode. */
export const severityPalette = {
  light: {
    CRITICAL: { main: '#B3261E', container: '#F9DEDC' },
    WARNING: { main: '#B26A00', container: '#FDEFC8' },
    INFO: { main: '#0B57D0', container: '#D3E3FD' },
    SUCCESS: { main: '#146C2E', container: '#C4EED0' },
    NEUTRAL: { main: '#444746', container: '#E9EEF6' },
  },
  dark: {
    CRITICAL: { main: '#F2B8B5', container: '#601410' },
    WARNING: { main: '#FFB95F', container: '#5C3A00' },
    INFO: { main: '#A8C7FA', container: '#0842A0' },
    SUCCESS: { main: '#6DD58C', container: '#0F5223' },
    NEUTRAL: { main: '#C4C7C5', container: '#282A2C' },
  },
} as const;

export type ToneKey = keyof typeof severityPalette.light;

declare module '@mui/material/styles' {
  interface Palette {
    surfaceContainer: { low: string; main: string; high: string };
  }
  interface PaletteOptions {
    surfaceContainer?: { low: string; main: string; high: string };
  }
}

export const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  defaultColorScheme: 'light',
  colorSchemes: {
    light: {
      palette: {
        mode: 'light',
        primary: { main: light.primary, contrastText: light.onPrimary },
        secondary: { main: light.secondary },
        error: { main: light.error },
        warning: { main: '#B26A00' },
        success: { main: '#146C2E' },
        info: { main: light.primary },
        background: { default: light.surfaceContainerLow, paper: light.surface },
        text: { primary: light.onSurface, secondary: light.onSurfaceVariant },
        divider: light.outlineVariant,
        action: { hover: 'rgba(31,31,31,0.04)', selected: light.primaryContainer },
        surfaceContainer: { low: light.surfaceContainerLow, main: light.surfaceContainer, high: light.surfaceContainerHigh },
      },
    },
    dark: {
      palette: {
        mode: 'dark',
        primary: { main: dark.primary, contrastText: dark.onPrimary },
        secondary: { main: dark.secondary },
        error: { main: dark.error },
        warning: { main: '#FFB95F' },
        success: { main: '#6DD58C' },
        info: { main: dark.primary },
        background: { default: dark.surface, paper: dark.surfaceContainerLow },
        text: { primary: dark.onSurface, secondary: dark.onSurfaceVariant },
        divider: dark.outlineVariant,
        action: { hover: 'rgba(227,227,227,0.06)', selected: dark.primaryContainer },
        surfaceContainer: { low: dark.surfaceContainerLow, main: dark.surfaceContainer, high: dark.surfaceContainerHigh },
      },
    },
  },
  spacing: 8,
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: 'Roboto, "Helvetica Neue", Arial, sans-serif',
    fontSize: 14,
    h1: { fontSize: '1.75rem', fontWeight: 400, letterSpacing: 0 },
    h2: { fontSize: '1.5rem', fontWeight: 400 },
    h3: { fontSize: '1.375rem', fontWeight: 400 },
    h4: { fontSize: '1.25rem', fontWeight: 500 },
    h5: { fontSize: '1.125rem', fontWeight: 500 },
    h6: { fontSize: '1rem', fontWeight: 500 },
    subtitle1: { fontSize: '0.9375rem', fontWeight: 500 },
    subtitle2: { fontSize: '0.8125rem', fontWeight: 500, letterSpacing: '0.01em' },
    body2: { fontSize: '0.8125rem' },
    caption: { fontSize: '0.75rem', letterSpacing: '0.02em' },
    button: { textTransform: 'none', fontWeight: 500, letterSpacing: '0.01em' },
    overline: { fontSize: '0.6875rem', fontWeight: 500, letterSpacing: '0.08em' },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: { fontFeatureSettings: '"tnum" 1' },
        '*::-webkit-scrollbar': { width: 10, height: 10 },
        '*::-webkit-scrollbar-thumb': { borderRadius: 8, border: '2px solid transparent', backgroundClip: 'content-box', backgroundColor: 'rgba(128,128,128,0.45)' },
      },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: ({ theme: t }) => ({ backgroundImage: 'none', border: `1px solid ${t.vars.palette.divider}` }),
        outlined: ({ theme: t }) => ({ border: `1px solid ${t.vars.palette.divider}` }),
      },
    },
    MuiCard: { defaultProps: { elevation: 0 }, styleOverrides: { root: { borderRadius: 12 } } },
    MuiAppBar: {
      defaultProps: { elevation: 0, color: 'inherit' },
      styleOverrides: {
        root: ({ theme: t }) => ({
          '--AppBar-background': t.vars.palette.background.paper,
          '--AppBar-color': t.vars.palette.text.primary,
          backgroundColor: t.vars.palette.background.paper,
          color: t.vars.palette.text.primary,
          backgroundImage: 'none',
          border: 'none',
          borderBottom: `1px solid ${t.vars.palette.divider}`,
        }),
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: ({ theme: t }) => ({ border: 'none', borderRight: `1px solid ${t.vars.palette.divider}`, backgroundImage: 'none' }),
      },
    },
    MuiDialog: {
      styleOverrides: { paper: { borderRadius: 16, border: 'none' } },
    },
    MuiMenu: { styleOverrides: { paper: { borderRadius: 8 } } },
    MuiPopover: { styleOverrides: { paper: { borderRadius: 8 } } },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 20, paddingInline: 16, minHeight: 36 },
        sizeSmall: { minHeight: 30, paddingInline: 12 },
        outlined: ({ theme: t }) => ({ borderColor: t.vars.palette.divider }),
      },
    },
    MuiIconButton: { styleOverrides: { root: { borderRadius: 8 } } },
    MuiChip: {
      styleOverrides: {
        root: { borderRadius: 8, fontWeight: 500 },
        sizeSmall: { height: 22, fontSize: '0.75rem' },
      },
    },
    MuiTextField: { defaultProps: { size: 'small', variant: 'outlined' } },
    MuiFormControl: { defaultProps: { size: 'small' } },
    MuiSelect: { defaultProps: { size: 'small' } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: ({ theme: t }) => ({
          borderRadius: 8,
          '& .MuiOutlinedInput-notchedOutline': { borderColor: t.vars.palette.divider },
        }),
      },
    },
    MuiTooltip: { defaultProps: { arrow: true, enterDelay: 400 }, styleOverrides: { tooltip: { fontSize: '0.75rem' } } },
    MuiListItemButton: {
      styleOverrides: {
        root: ({ theme: t }) => ({
          borderRadius: 20,
          marginInline: 8,
          paddingBlock: 6,
          '&.Mui-selected': {
            backgroundColor: t.vars.palette.action.selected,
            color: light.onPrimaryContainer,
            '& .MuiListItemIcon-root': { color: 'inherit' },
            '&:hover': { backgroundColor: t.vars.palette.action.selected },
            ...t.applyStyles('dark', { color: dark.onPrimaryContainer }),
          },
        }),
      },
    },
    MuiListItemIcon: { styleOverrides: { root: { minWidth: 40 } } },
    MuiTableCell: { styleOverrides: { root: ({ theme: t }) => ({ borderBottomColor: t.vars.palette.divider }) } },
    MuiDivider: { styleOverrides: { root: ({ theme: t }) => ({ borderColor: t.vars.palette.divider }) } },
    MuiAlert: { styleOverrides: { root: { borderRadius: 8 } } },
    MuiTabs: { styleOverrides: { root: { minHeight: 40 }, indicator: { height: 3, borderRadius: '3px 3px 0 0' } } },
    MuiTab: { styleOverrides: { root: { minHeight: 40, paddingBlock: 8 } } },
    MuiSnackbarContent: { styleOverrides: { root: { borderRadius: 8 } } },
  },
});
