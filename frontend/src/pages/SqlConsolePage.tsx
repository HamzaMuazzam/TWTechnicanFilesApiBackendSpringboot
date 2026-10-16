import { useMemo, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
  Collapse,
  Divider,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Paper,
  Tab,
  Tabs,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import HistoryIcon from '@mui/icons-material/History';
import DeleteSweepOutlinedIcon from '@mui/icons-material/DeleteSweepOutlined';
import { sqlApi } from '@/api/endpoints';
import type { SqlResult } from '@/api/types';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import { friendlyError } from '@/api/errors';
import { readJson, writeJson } from '@/utils/storage';
import { fmtDateTime } from '@/utils/format';
import { SAMPLE_QUERIES, SCHEMA } from '@/schema/tables';

const HISTORY_KEY = 'tw.sql.history';
const DRAFT_KEY = 'tw.sql.draft';
interface HistoryItem {
  sql: string;
  at: string;
  ok: boolean;
  rows?: number;
  elapsedMs?: number;
}

function SchemaPanel({ onInsert }: { onInsert: (text: string) => void }) {
  const [open, setOpen] = useState<Record<string, boolean>>({});
  return (
    <List dense disablePadding sx={{ overflowY: 'auto', flex: 1 }}>
      {SCHEMA.map((t) => (
        <Box key={t.name}>
          <ListItemButton sx={{ borderRadius: 0, mx: 0, py: 0.5 }} onClick={() => onInsert(t.name)}>
            <ListItemText primary={t.name} slotProps={{ primary: { sx: { fontFamily: 'monospace', fontSize: 13 } } }} secondary={t.note} />
            <IconButton
              size="small"
              aria-label={open[t.name] ? 'Hide columns' : 'Show columns'}
              onClick={(e) => {
                e.stopPropagation();
                setOpen((o) => ({ ...o, [t.name]: !o[t.name] }));
              }}
              sx={{ transform: open[t.name] ? 'rotate(180deg)' : 'none', transition: 'transform .15s' }}
            >
              <ExpandMoreIcon fontSize="small" />
            </IconButton>
          </ListItemButton>
          <Collapse in={!!open[t.name]} unmountOnExit>
            <Box sx={{ pl: 3, pb: 1, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
              {t.columns.map((c) => (
                <Chip key={c} label={c} size="small" variant="outlined" onClick={() => onInsert(c)} sx={{ fontFamily: 'monospace', fontSize: 11, height: 20 }} />
              ))}
            </Box>
          </Collapse>
        </Box>
      ))}
    </List>
  );
}

export function SqlConsolePage() {
  const [sql, setSql] = useState<string>(() => readJson<string>(DRAFT_KEY, SAMPLE_QUERIES[0].sql));
  const [history, setHistory] = useState<HistoryItem[]>(() => readJson<HistoryItem[]>(HISTORY_KEY, []));
  const [sideTab, setSideTab] = useState<'schema' | 'history'>('schema');
  const [result, setResult] = useState<SqlResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);

  const pushHistory = (item: HistoryItem) => {
    const next = [item, ...history.filter((h) => h.sql !== item.sql)].slice(0, 20);
    setHistory(next);
    writeJson(HISTORY_KEY, next);
  };

  const run = useMutation({
    mutationFn: (q: string) => sqlApi.run(q),
    onSuccess: (res, q) => {
      setResult(res);
      setError(null);
      pushHistory({ sql: q, at: new Date().toISOString(), ok: true, rows: res.rows.length, elapsedMs: res.elapsedMs });
    },
    onError: (e, q) => {
      setError(friendlyError(e));
      setResult(null);
      pushHistory({ sql: q, at: new Date().toISOString(), ok: false });
    },
  });

  const execute = () => {
    const q = sql.trim().replace(/;\s*$/, '');
    if (!q) return;
    if (!/^(select|with)\b/i.test(q)) {
      setError('Only a single read-only statement starting with SELECT or WITH is allowed.');
      return;
    }
    run.mutate(q);
  };

  const insertAtCursor = (text: string) => {
    const el = inputRef.current;
    if (!el) {
      setSql((s) => s + text);
      return;
    }
    const start = el.selectionStart ?? sql.length;
    const end = el.selectionEnd ?? sql.length;
    const next = sql.slice(0, start) + text + sql.slice(end);
    setSql(next);
    writeJson(DRAFT_KEY, next);
    requestAnimationFrame(() => {
      el.focus();
      el.setSelectionRange(start + text.length, start + text.length);
    });
  };

  const columns = useMemo<GridColDef[]>(
    () =>
      (result?.columns ?? []).map((c, i) => ({
        field: `c${i}`,
        headerName: c,
        minWidth: 120,
        flex: 1,
        sortable: true,
        valueGetter: (_v, row: Record<string, unknown>) => row[`c${i}`],
        renderCell: (p) => {
          const v = p.value;
          if (v === null || v === undefined) return <Typography component="span" variant="body2" color="text.disabled">NULL</Typography>;
          const s = typeof v === 'object' ? JSON.stringify(v) : String(v);
          return (
            <Typography component="span" variant="body2" noWrap title={s} sx={{ fontFamily: 'monospace', fontSize: 12.5 }}>
              {s}
            </Typography>
          );
        },
      })),
    [result],
  );
  const rows = useMemo(
    () => (result?.rows ?? []).map((r, idx) => ({ __id: idx, ...Object.fromEntries(r.map((v, i) => [`c${i}`, v])) })),
    [result],
  );

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      <PageHeader title="SQL console" subtitle="Read-only. One SELECT / WITH statement, 10 s timeout, max 1000 rows. Every query is audit-logged." />
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 300px' }, gap: 1.5, flex: 1, minHeight: 0 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, minWidth: 0 }}>
          <Paper sx={{ p: 1.5 }}>
            <TextField
              inputRef={inputRef}
              multiline
              minRows={5}
              maxRows={16}
              fullWidth
              value={sql}
              onChange={(e) => {
                setSql(e.target.value);
                writeJson(DRAFT_KEY, e.target.value);
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                  e.preventDefault();
                  execute();
                }
              }}
              placeholder="SELECT …"
              spellCheck={false}
              slotProps={{ input: { sx: { fontFamily: '"Roboto Mono", ui-monospace, Menlo, monospace', fontSize: 13, lineHeight: 1.5 } }, htmlInput: { 'aria-label': 'SQL query' } }}
            />
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1, flexWrap: 'wrap' }}>
              <Tooltip title="Ctrl/⌘ + Enter">
                <span>
                  <Button variant="contained" startIcon={<PlayArrowIcon />} onClick={execute} loading={run.isPending} disabled={!sql.trim()}>
                    Run
                  </Button>
                </span>
              </Tooltip>
              {SAMPLE_QUERIES.map((s) => (
                <Chip key={s.label} label={s.label} size="small" variant="outlined" onClick={() => setSql(s.sql)} />
              ))}
              <Box sx={{ flex: 1 }} />
              {result && (
                <Typography variant="caption" color="text.secondary">
                  {result.rows.length.toLocaleString()} row{result.rows.length === 1 ? '' : 's'} · {result.elapsedMs} ms
                </Typography>
              )}
            </Box>
          </Paper>

          {error && (
            <Alert severity="error" variant="outlined" onClose={() => setError(null)} sx={{ fontFamily: 'monospace', fontSize: 12.5 }}>
              {error}
            </Alert>
          )}
          {result?.truncated && (
            <Alert severity="warning" variant="outlined">
              Result truncated by the server (hard limit 1000 rows). Add a <code>WHERE</code> or <code>LIMIT</code> to narrow it.
            </Alert>
          )}

          <Paper sx={{ flex: 1, minHeight: 320, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            {!result && !run.isPending ? (
              <EmptyState title="No results yet" description="Write a query and press Run (Ctrl/⌘ + Enter). Click a table or column on the right to insert its name." />
            ) : (
              <DataGrid
                rows={rows}
                columns={columns}
                getRowId={(r) => r.__id as number}
                loading={run.isPending}
                density="compact"
                disableRowSelectionOnClick
                disableColumnFilter
                pageSizeOptions={[50, 100, 500, 1000]}
                initialState={{ pagination: { paginationModel: { pageSize: 100 } } }}
                slots={{ noRowsOverlay: () => <EmptyState compact title="Query returned no rows" /> }}
                sx={{
                  border: 'none',
                  '& .MuiDataGrid-columnHeaders': { bgcolor: 'background.default' },
                  '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 500, fontSize: '0.75rem', color: 'text.secondary', fontFamily: 'monospace' },
                  '& .MuiDataGrid-cell': { display: 'flex', alignItems: 'center' },
                }}
              />
            )}
          </Paper>
        </Box>

        <Paper sx={{ display: 'flex', flexDirection: 'column', minHeight: 320, maxHeight: { md: 'calc(100vh - 180px)' }, overflow: 'hidden', position: { md: 'sticky' }, top: { md: 88 } }}>
          <Tabs value={sideTab} onChange={(_, v: 'schema' | 'history') => setSideTab(v)} variant="fullWidth">
            <Tab label="Schema" value="schema" />
            <Tab label={`History${history.length ? ` (${history.length})` : ''}`} value="history" icon={<HistoryIcon fontSize="small" />} iconPosition="start" />
          </Tabs>
          <Divider />
          {sideTab === 'schema' ? (
            <SchemaPanel onInsert={insertAtCursor} />
          ) : (
            <>
              {history.length === 0 ? (
                <EmptyState compact title="No history" description="The last 20 queries are kept in this browser." />
              ) : (
                <List dense disablePadding sx={{ overflowY: 'auto', flex: 1 }}>
                  {history.map((h) => (
                    <ListItemButton key={h.at + h.sql} sx={{ borderRadius: 0, mx: 0, alignItems: 'flex-start' }} onClick={() => setSql(h.sql)}>
                      <ListItemText
                        primary={h.sql}
                        secondary={`${fmtDateTime(h.at)}${h.ok ? ` · ${h.rows} rows · ${h.elapsedMs} ms` : ' · failed'}`}
                        slotProps={{
                          primary: { sx: { fontFamily: 'monospace', fontSize: 12, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' } },
                          secondary: { sx: { color: h.ok ? undefined : 'error.main' } },
                        }}
                      />
                    </ListItemButton>
                  ))}
                </List>
              )}
              {history.length > 0 && (
                <>
                  <Divider />
                  <Button
                    size="small"
                    startIcon={<DeleteSweepOutlinedIcon />}
                    onClick={() => {
                      setHistory([]);
                      writeJson(HISTORY_KEY, []);
                    }}
                    sx={{ m: 1, alignSelf: 'flex-end', color: 'text.secondary' }}
                  >
                    Clear history
                  </Button>
                </>
              )}
            </>
          )}
        </Paper>
      </Box>
    </Box>
  );
}
