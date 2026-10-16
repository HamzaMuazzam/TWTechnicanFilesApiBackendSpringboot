import { useState } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import BookmarkBorderIcon from '@mui/icons-material/BookmarkBorder';
import BookmarkAddOutlinedIcon from '@mui/icons-material/BookmarkAddOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import type { FilterClause, FilterLogic } from '@/api/types';
import { readJson, writeJson } from '@/utils/storage';

export interface SavedFilter {
  name: string;
  filters: FilterClause[];
  logic: FilterLogic;
  search: string;
  savedAt: string;
}

const keyFor = (entity: string) => `tw.filters.${entity}`;

function loadSavedFilters(entity: string): SavedFilter[] {
  return readJson<SavedFilter[]>(keyFor(entity), []);
}

interface Props {
  entity: string;
  current: { filters: FilterClause[]; logic: FilterLogic; search: string };
  onLoad: (f: SavedFilter) => void;
}

/** Saved filters live in localStorage per entity (SPEC §9). */
export function SavedFiltersMenu({ entity, current, onLoad }: Props) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const [saved, setSaved] = useState<SavedFilter[]>(() => loadSavedFilters(entity));
  const [saveOpen, setSaveOpen] = useState(false);
  const [name, setName] = useState('');

  const persist = (list: SavedFilter[]) => {
    setSaved(list);
    writeJson(keyFor(entity), list);
  };

  const canSave = current.filters.length > 0 || !!current.search;

  const save = () => {
    const trimmed = name.trim();
    if (!trimmed) return;
    const next = saved.filter((s) => s.name !== trimmed);
    next.unshift({ name: trimmed, ...current, savedAt: new Date().toISOString() });
    persist(next.slice(0, 30));
    setSaveOpen(false);
    setName('');
  };

  return (
    <>
      <Tooltip title="Saved filters">
        <Button
          size="small"
          color="inherit"
          startIcon={<BookmarkBorderIcon fontSize="small" />}
          onClick={(e) => setAnchor(e.currentTarget)}
          sx={{ color: 'text.secondary' }}
        >
          Saved
        </Button>
      </Tooltip>
      <Menu open={!!anchor} anchorEl={anchor} onClose={() => setAnchor(null)} slotProps={{ paper: { sx: { minWidth: 280 } } }}>
        <MenuItem
          disabled={!canSave}
          onClick={() => {
            setAnchor(null);
            setSaveOpen(true);
          }}
        >
          <ListItemIcon>
            <BookmarkAddOutlinedIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Save current filters…" secondary={canSave ? undefined : 'Apply a filter or search first'} />
        </MenuItem>
        <Divider />
        {saved.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ px: 2, py: 1 }}>
            No saved filters for this page yet.
          </Typography>
        )}
        {saved.map((s) => (
          <MenuItem
            key={s.name}
            onClick={() => {
              setAnchor(null);
              onLoad(s);
            }}
          >
            <ListItemText
              primary={s.name}
              secondary={`${s.filters.length} condition${s.filters.length === 1 ? '' : 's'}${s.search ? ` · search “${s.search}”` : ''}`}
            />
            <IconButton
              size="small"
              edge="end"
              aria-label={`Delete saved filter ${s.name}`}
              onClick={(e) => {
                e.stopPropagation();
                persist(saved.filter((x) => x.name !== s.name));
              }}
            >
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </MenuItem>
        ))}
      </Menu>

      <Dialog open={saveOpen} onClose={() => setSaveOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Save filter</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            label="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && save()}
            sx={{ mt: 1 }}
            helperText="Saved in this browser only."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSaveOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={save} disabled={!name.trim()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
