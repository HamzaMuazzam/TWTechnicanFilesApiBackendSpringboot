import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  Badge,
  Box,
  Button,
  Divider,
  IconButton,
  InputAdornment,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Paper,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import {
  DataGrid,
  GridPreferencePanelsValue,
  useGridApiRef,
  type GridColDef,
  type GridColumnVisibilityModel,
  type GridDensity,
  type GridPaginationModel,
  type GridRowId,
  type GridSortModel,
  type GridValidRowModel,
} from '@mui/x-data-grid';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';
import FilterListIcon from '@mui/icons-material/FilterList';
import ViewColumnOutlinedIcon from '@mui/icons-material/ViewColumnOutlined';
import DensitySmallIcon from '@mui/icons-material/DensitySmall';
import DensityMediumIcon from '@mui/icons-material/DensityMedium';
import DensityLargeIcon from '@mui/icons-material/DensityLarge';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckIcon from '@mui/icons-material/Check';
import { queryApi } from '@/api/endpoints';
import { PAGE_BASE } from '@/api/client';
import type { FilterRequest, PageResponse } from '@/api/types';
import { apiField, type EntityConfig } from '@/entities/types';
import { queryKeys } from '@/api/hooks';
import { readJson, writeJson } from '@/utils/storage';
import { downloadBlob } from '@/utils/download';
import { dayjs } from '@/utils/format';
import { useSnackbar } from '@/components/SnackbarProvider';
import { ErrorState } from '@/components/ErrorState';
import { EmptyState } from '@/components/EmptyState';
import { PageHeader } from '@/components/PageHeader';
import { FilterBuilder } from './FilterBuilder';
import { FilterChips } from './FilterChips';
import { SavedFiltersMenu } from './SavedFiltersMenu';
import { columnFromMeta } from './columns';
import { PAGE_SIZES, useGridQueryState } from './useGridQueryState';

const DENSITY_KEY = 'tw.grid.density';
const colsKey = (entity: string) => `tw.grid.cols.${entity}`;

export interface DataGridPageProps<R extends GridValidRowModel> {
  entity: EntityConfig;
  /** page title override */
  title?: string;
  subtitle?: ReactNode;
  /** buttons rendered in the page header (e.g. Create) */
  headerActions?: ReactNode;
  /** extra columns appended after the metadata columns (e.g. actions) */
  extraColumns?: GridColDef<R>[];
  /** per-field overrides merged into the generated columns */
  columnOverrides?: Partial<Record<string, Partial<GridColDef<R>>>>;
  onRowClick?: (row: R) => void;
  selectedId?: GridRowId | null;
  /** rendered below the grid (drawers, dialogs) */
  children?: ReactNode;
}

function useDebounced<T>(value: T, ms: number): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

export function DataGridPage<R extends GridValidRowModel>({
  entity,
  title,
  subtitle,
  headerActions,
  extraColumns,
  columnOverrides,
  onRowClick,
  selectedId,
  children,
}: DataGridPageProps<R>) {
  const { state, update } = useGridQueryState(entity);
  const apiRef = useGridApiRef();
  const queryClient = useQueryClient();
  const snackbar = useSnackbar();

  // ---- local UI state ----
  const [filtersOpen, setFiltersOpen] = useState(state.filters.length > 0);
  const [searchText, setSearchText] = useState(state.search);
  const debouncedSearch = useDebounced(searchText, 400);
  const [density, setDensity] = useState<GridDensity>(() => readJson<GridDensity>(DENSITY_KEY, 'compact'));
  const [densityAnchor, setDensityAnchor] = useState<HTMLElement | null>(null);
  const [visibility, setVisibility] = useState<GridColumnVisibilityModel>(() =>
    readJson<GridColumnVisibilityModel>(
      colsKey(entity.name),
      Object.fromEntries(entity.fields.filter((f) => f.hidden).map((f) => [f.field, false])),
    ),
  );

  // Keep the quick-search box in sync when the URL changes externally (search-to-navigate, saved filter).
  const [syncedSearch, setSyncedSearch] = useState(state.search);
  if (syncedSearch !== state.search) {
    setSyncedSearch(state.search);
    setSearchText(state.search);
  }
  // Push debounced text into the URL.
  useEffect(() => {
    if (debouncedSearch !== state.search) update({ search: debouncedSearch }, { resetPage: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch]);

  // ---- request body ----
  const toApiField = useCallback(
    (field: string) => {
      const meta = entity.fields.find((f) => f.field === field);
      return meta ? apiField(meta) : field;
    },
    [entity.fields],
  );

  const request = useMemo<FilterRequest>(
    () => ({
      filters: state.filters.map((f) => ({ ...f, field: toApiField(f.field) })),
      logic: state.logic,
      sort: state.sort ? [{ ...state.sort, field: toApiField(state.sort.field) }] : [],
      page: state.page + PAGE_BASE,
      size: state.size,
      ...(state.search ? { search: state.search } : {}),
    }),
    [state, toApiField],
  );

  const query = useQuery<PageResponse<R>>({
    queryKey: [...queryKeys.entity(entity.name), request],
    queryFn: () => queryApi.query<R>(entity.name, request),
    placeholderData: keepPreviousData,
  });

  const exportMutation = useMutation({
    // SPEC §7.5: export takes the same FilterRequest body (server caps at 50k rows).
    mutationFn: () => queryApi.exportCsv(entity.name, request),
    onSuccess: (blob) => {
      downloadBlob(blob, `${entity.name}-${dayjs().format('YYYYMMDD-HHmm')}.csv`);
      snackbar.success('Export downloaded');
    },
    onError: (e) => snackbar.error(e, 'Export failed'),
  });

  // ---- columns ----
  const columns = useMemo<GridColDef<R>[]>(() => {
    const base = entity.fields.map((f) => ({ ...columnFromMeta<R>(f), ...(columnOverrides?.[f.field] ?? {}) }));
    return extraColumns ? [...base, ...extraColumns] : base;
  }, [entity, extraColumns, columnOverrides]);

  // ---- handlers ----
  const onPaginationChange = useCallback(
    (m: GridPaginationModel) => update({ page: m.page, size: m.pageSize }, { resetPage: m.pageSize !== state.size }),
    [update, state.size],
  );
  const onSortChange = useCallback(
    (m: GridSortModel) => {
      const s = m[0];
      update({ sort: s ? { field: s.field, dir: s.sort === 'asc' ? 'asc' : 'desc' } : null }, { resetPage: true });
    },
    [update],
  );
  const changeDensity = (d: GridDensity) => {
    setDensity(d);
    writeJson(DENSITY_KEY, d);
    setDensityAnchor(null);
  };
  const refresh = () => queryClient.invalidateQueries({ queryKey: queryKeys.entity(entity.name) });

  const rows = query.data?.items ?? [];
  const total = query.data?.total ?? 0;
  const activeFilterCount = state.filters.length;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      <PageHeader
        title={title ?? entity.title}
        subtitle={subtitle ?? (query.data ? `${total.toLocaleString()} ${total === 1 ? entity.singular : entity.title.toLowerCase()}` : undefined)}
        actions={headerActions}
      />

      {/* Toolbar */}
      <Paper sx={{ display: 'flex', alignItems: 'center', gap: 0.5, px: 1, py: 0.75, mb: 1.5, flexWrap: 'wrap' }}>
        <TextField
          size="small"
          placeholder={entity.searchPlaceholder}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') update({ search: searchText }, { resetPage: true });
            if (e.key === 'Escape') setSearchText('');
          }}
          sx={{ width: { xs: '100%', sm: 300 }, '& .MuiOutlinedInput-root': { bgcolor: 'background.default' } }}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
              endAdornment: searchText ? (
                <InputAdornment position="end">
                  <IconButton size="small" edge="end" onClick={() => setSearchText('')} aria-label="Clear search">
                    <ClearIcon fontSize="small" />
                  </IconButton>
                </InputAdornment>
              ) : undefined,
            },
          }}
        />
        <Button
          size="small"
          color={filtersOpen || activeFilterCount ? 'primary' : 'inherit'}
          startIcon={
            <Badge badgeContent={activeFilterCount} color="primary" overlap="circular" sx={{ '& .MuiBadge-badge': { fontSize: 10, height: 16, minWidth: 16 } }}>
              <FilterListIcon fontSize="small" />
            </Badge>
          }
          onClick={() => setFiltersOpen((o) => !o)}
          sx={{ color: filtersOpen || activeFilterCount ? undefined : 'text.secondary' }}
          aria-expanded={filtersOpen}
        >
          Filters
        </Button>
        <SavedFiltersMenu
          entity={entity.name}
          current={{ filters: state.filters, logic: state.logic, search: state.search }}
          onLoad={(s) => {
            setSearchText(s.search);
            update({ filters: s.filters, logic: s.logic, search: s.search }, { resetPage: true });
            if (s.filters.length) setFiltersOpen(true);
          }}
        />
        <Box sx={{ flex: 1 }} />
        <Tooltip title="Choose columns">
          <IconButton size="small" onClick={() => apiRef.current?.showPreferences(GridPreferencePanelsValue.columns)} aria-label="Choose columns">
            <ViewColumnOutlinedIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title="Row density">
          <IconButton size="small" onClick={(e) => setDensityAnchor(e.currentTarget)} aria-label="Row density">
            {density === 'compact' ? <DensitySmallIcon fontSize="small" /> : density === 'standard' ? <DensityMediumIcon fontSize="small" /> : <DensityLargeIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
        <Menu open={!!densityAnchor} anchorEl={densityAnchor} onClose={() => setDensityAnchor(null)}>
          {(['compact', 'standard', 'comfortable'] as GridDensity[]).map((d) => (
            <MenuItem key={d} dense selected={density === d} onClick={() => changeDensity(d)}>
              <ListItemIcon>{density === d ? <CheckIcon fontSize="small" /> : null}</ListItemIcon>
              <ListItemText primary={d[0].toUpperCase() + d.slice(1)} />
            </MenuItem>
          ))}
        </Menu>
        <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
        <Tooltip title="Export the current filter as CSV (max 50k rows)">
          <span>
            <Button
              size="small"
              color="inherit"
              startIcon={<FileDownloadOutlinedIcon fontSize="small" />}
              onClick={() => exportMutation.mutate()}
              loading={exportMutation.isPending}
              sx={{ color: 'text.secondary' }}
            >
              Export
            </Button>
          </span>
        </Tooltip>
        <Tooltip title="Refresh">
          <IconButton size="small" onClick={refresh} aria-label="Refresh">
            <RefreshIcon fontSize="small" sx={query.isFetching ? { animation: 'spin 1s linear infinite', '@keyframes spin': { to: { transform: 'rotate(360deg)' } } } : undefined} />
          </IconButton>
        </Tooltip>
      </Paper>

      <FilterBuilder
        open={filtersOpen}
        entity={entity}
        filters={state.filters}
        logic={state.logic}
        onApply={(filters, logic) => update({ filters, logic }, { resetPage: true })}
        onClear={() => update({ filters: [], logic: 'AND' }, { resetPage: true })}
        onClose={() => setFiltersOpen(false)}
      />

      <FilterChips
        entity={entity}
        filters={state.filters}
        logic={state.logic}
        search={state.search}
        onRemove={(i) => update({ filters: state.filters.filter((_, idx) => idx !== i) }, { resetPage: true })}
        onClearSearch={() => {
          setSearchText('');
          update({ search: '' }, { resetPage: true });
        }}
        onClearAll={() => {
          setSearchText('');
          update({ filters: [], search: '', logic: 'AND' }, { resetPage: true });
        }}
      />

      {query.isError && <ErrorState error={query.error} onRetry={() => query.refetch()} compact />}

      <Paper sx={{ flex: 1, minHeight: 420, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <DataGrid<R>
          apiRef={apiRef}
          rows={rows}
          columns={columns}
          getRowId={(row) => row[entity.idField] as GridRowId}
          loading={query.isPending || (query.isFetching && !query.data)}
          rowCount={total}
          paginationMode="server"
          sortingMode="server"
          filterMode="server"
          disableColumnFilter
          disableColumnMenu={false}
          paginationModel={{ page: state.page, pageSize: state.size }}
          onPaginationModelChange={onPaginationChange}
          pageSizeOptions={PAGE_SIZES as unknown as number[]}
          sortModel={state.sort ? [{ field: state.sort.field, sort: state.sort.dir }] : []}
          onSortModelChange={onSortChange}
          sortingOrder={['desc', 'asc']}
          density={density}
          onDensityChange={changeDensity}
          columnVisibilityModel={visibility}
          onColumnVisibilityModelChange={(m) => {
            setVisibility(m);
            writeJson(colsKey(entity.name), m);
          }}
          disableRowSelectionOnClick
          hideFooterSelectedRowCount
          onRowClick={onRowClick ? (p) => onRowClick(p.row as R) : undefined}
          rowSelectionModel={selectedId !== null && selectedId !== undefined ? { type: 'include', ids: new Set([selectedId]) } : { type: 'include', ids: new Set() }}
          keepNonExistentRowsSelected
          slots={{
            noRowsOverlay: () => (
              <EmptyState
                compact
                title={state.filters.length || state.search ? 'No rows match the current filters' : `No ${entity.title.toLowerCase()} yet`}
                description={state.filters.length || state.search ? 'Try removing a condition or clearing the search.' : undefined}
              />
            ),
          }}
          sx={{
            border: 'none',
            '--DataGrid-rowBorderColor': 'var(--mui-palette-divider)',
            '& .MuiDataGrid-columnHeaders': { bgcolor: 'background.default' },
            '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 500, fontSize: '0.75rem', letterSpacing: '0.02em', color: 'text.secondary' },
            '& .MuiDataGrid-row': { cursor: onRowClick ? 'pointer' : 'default' },
            '& .MuiDataGrid-cell:focus, & .MuiDataGrid-cell:focus-within, & .MuiDataGrid-columnHeader:focus, & .MuiDataGrid-columnHeader:focus-within': { outline: 'none' },
            '& .MuiDataGrid-cell': { display: 'flex', alignItems: 'center' },
            '& .MuiDataGrid-footerContainer': { borderTop: '1px solid', borderColor: 'divider' },
          }}
        />
      </Paper>
      {!query.isError && query.data && total > 0 && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
          Page {state.page + 1} of {Math.max(1, Math.ceil(total / state.size))}
          {query.isFetching ? ' · refreshing…' : ''}
        </Typography>
      )}
      {children}
    </Box>
  );
}
