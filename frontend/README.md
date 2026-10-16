# TW Notification — Admin dashboard

React 19 + TypeScript + Vite + MUI v7 single-page app for the TW Notification backend.
It is served by Spring Boot from `classpath:/static/admin` at **`/admin`** (SPEC §3) and talks to
`/api/v1/admin/**` (SPEC §7.5).

## Development

Requires Node 20+ (Node 22 recommended) and the backend running on port 8089.

```bash
npm i          # or: npm ci
npm run dev    # http://localhost:5173/admin/  (proxies /api → http://localhost:8089)
```

Sign in with an `admin_user` account (seed: `admin` / `ADMIN_INITIAL_PASSWORD`).

Other scripts:

| Script           | What it does                                  |
|------------------|-----------------------------------------------|
| `npm run build`  | `tsc -b && vite build` → `dist/` (zero errors) |
| `npm run lint`   | ESLint flat config (TS + react-hooks)         |
| `npm run preview`| Serves `dist/` at `/admin/` with the same proxy |

## Production

Gradle builds this folder (`npm ci && npm run build`) and copies `dist/` into
`src/main/resources/static/admin`. The backend must serve `/admin/**` with a history
fallback to `/admin/index.html`. All asset URLs are prefixed with `/admin/`
(`vite.config.ts` → `base: '/admin/'`; the router uses `basename="/admin"`).

## Project layout

```
src/
  api/          axios client (envelope unwrap, bearer, refresh-on-401), typed endpoints, SPEC types
  auth/         AuthProvider (access token in memory, refresh token in localStorage), route guards
  components/   DataGridPage + FilterBuilder, StatusChip, KpiTile, DetailDrawer, dialogs, layout
  entities/     one typed config per grid: columns, filterable fields, default sort
  pages/        Login, Overview, Alerts, Notifications, Deliveries, Discarded, Users, Devices,
                Sources, Alert types, Admins, SQL console, Settings, 404
  schema/       SPEC §5 tables as a reference list for the SQL console
  theme/        MUI theme (SPEC §8.4 light + dark palettes), severity/status colour map
  utils/        date formatting (dayjs), localStorage helpers, download/copy
```

## Conventions that depend on the backend

* Every response is `{ success, message, timestamp, data }`. The axios layer unwraps `data`
  and throws `ApiError(code)` when `success=false` or on HTTP errors; `friendlyError()` maps codes
  to text.
* `FilterRequest.page` is sent **0-based** (`src/api/client.ts` → `PAGE_BASE`). Change it to `1` if
  the backend is 1-based.
* `GET /alerts/{id}` is expected as `{ alert, recipients: [{ id|notificationId, phoneE164, status,
  sentAt, deliveredAt, readAt, deliveries: [...] }], discards }`; `GET /users/{id}` as
  `{ user, devices, notifications }`. Both normalisers in `src/api/endpoints.ts` tolerate a
  flattened shape too.
* `POST /sources` and `POST /sources/{id}/rotate-key` should return the plaintext key as `apiKey`
  (also accepts `key`). It is shown once in the UI.
* `GET /dispatch/status` is rendered as a key → number list, whatever the keys are.
* `GET/PUT /settings` accepts a flat object `{ retention_days, max_devices_per_user,
  dispatch_max_attempts }` (numbers or numeric strings) or a `[{key,value}]` array on GET.

## Browser state

| Key                       | Purpose                                  |
|---------------------------|------------------------------------------|
| `tw.admin.refreshToken`   | refresh token                            |
| `mui-mode`                | light / dark (MUI colour scheme)         |
| `tw.grid.density`         | DataGrid density                         |
| `tw.grid.cols.<entity>`   | column visibility per grid               |
| `tw.filters.<entity>`     | saved filters per grid                   |
| `tw.sql.history` / `tw.sql.draft` | SQL console history (last 20) and draft |

Grid state (page, size, sort, filters, search, open drawer) lives in the URL, so links are shareable.
