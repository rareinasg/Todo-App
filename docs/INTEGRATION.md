# Integration Audit — Todo-App

This document records how the seven branches were reconciled into `main`, the
defects that existed before the merge, and the verification that was performed.
It is the repository-facing summary; the detailed first-person sprint log is
kept locally and is intentionally not committed.

## 1. Pre-merge state

Safety tags created before any write:

| Tag | Commit | Ref |
|---|---|---|
| `pre-merge/main` | `263e0f3` | `main` |
| `pre-merge/candra` | `8cf5268` | `candra` |
| `pre-merge/putra` | `1cbd25e` | `putra-retrofit` |
| `pre-merge/salma` | `3019c7f` | `salma-viewmodel` |
| `pre-merge/surya` | `99561e0` | `surya` |
| `pre-merge/andry` | `99561e0` | `andry-njakatiana` |
| `pre-merge/reina` | `f9eb04e` | `reina` |
| `pre-merge/surya-coroutines` | `f9eb04e` | `surya-coroutines` |

Topology:

- **Lineage A:** `263e0f3` → `candra` / `putra-retrofit` → `salma-viewmodel`
- **Lineage B (unrelated root):** `99561e0` → `reina` / `surya-coroutines`
- `surya` and `andry-njakatiana` pointed to the same commit (`99561e0`).
- `reina` and `surya-coroutines` pointed to the same commit (`f9eb04e`).
- `putra-retrofit` was already an ancestor of `salma-viewmodel`.

So seven refs reduced to **three unique merge operations**: `candra`,
`salma-viewmodel`, `reina`.

## 2. Defects found before merging

| Ref | Defect | Why it mattered |
|---|---|---|
| `main` | `AndroidManifest.xml` invalid: stray `S` after `</manifest>`, `android:networkSecurityConfig` outside `<application>` | `processDebugMainManifest` failed; the project never built |
| `main` | `Theme.kt` missing while `MainActivity` imported `TodoAppTheme` | `Unresolved reference 'theme'` |
| `main` | `network_security_config.xml` missing while the manifest referenced it | Resource linking failure |
| `main` | `.gitignore` did not ignore `node_modules` | 979 dependency files were committed on `candra` |
| `candra` | API at repo root; `package.json` `main: index.js`; no start/smoke script; no schema | Not the layout from Bagian 2.2; not reproducible |
| `salma-viewmodel` | `addTodo`/`updateTodo`/`deleteTodo`/`clearError`/`enqueueThenRefresh` declared inside `onFailure` | Legal Kotlin local functions, but not ViewModel members; `TodoScreen` would not compile |
| `salma-viewmodel` | `Theme.kt` missing | Build failed |
| `surya` / `andry` | Orphan root `99561e0`; no common ancestor with `main` | Naive merge produced add/add conflicts on every shared file |
| `surya` / `andry` | Network config allowed `localhost` + `10.100.134.62`, not `10.0.2.2` | Emulator request would be blocked/refused |
| `surya` / `andry` | Duplicate Retrofit/lifecycle dependencies; icons dependency present | Build noise and risk of version drift |
| `reina` | `BASE_URL = http://localhost:3000/api/` | From the emulator, `localhost` is the emulator itself |
| `reina` | `Response<Void>` for PUT/DELETE | Bagian 10 specifies `Response<Unit>` |
| `reina` | `import okio.IOException` | Compiles on JVM via typealias, but the canonical type is `java.io.IOException` |
| `reina` | Error messages “HTTP Response …”, “sever” typo, English action labels | Did not match the module |
| all | `gradlew` mode `100644` | `./gradlew` → permission denied on a fresh clone |
| all | No README, schema file, smoke test, or CI | No reproducible run book or contract check |

## 3. Merge sequence

### PR #1 — `candra` → `main`

**Merge commit:** `cd2cea1`

Changes made on the branch before merging:

- `git rm -r --cached node_modules` (979 files) and added `node_modules/`,
  `.env`, `*.log` to `.gitignore`.
- Moved `server.js`, `package.json`, `package-lock.json` into `todo-api/`.
- Added `todo-api/schema.sql` (idempotent), `.env.example`, and
  `todo-api/smoke-test.js`.
- Updated `package.json` (`main`, `start`, `smoke`, `engines`).
- Result: 7 changed files in the PR instead of 982.

### PR #2 — `salma-viewmodel` → `main`

**Merge commit:** `0800df7`

Changes made on the branch before merging:

- Added the missing `ui/theme/Theme.kt`.
- Restored the Bagian 6 callback ViewModel with all CRUD functions at class
  scope.
- Verified with `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

Because `putra-retrofit` is an ancestor of this branch, it is merged by the
same PR.

### PR #3 — `reina` → `main`

**Merge commit:** `8ddeb98`

Changes made on the branch before merging:

- `BASE_URL` and `networkSecurityConfig` both target `10.0.2.2`.
- PUT/DELETE use `Response<Unit>`.
- `java.io.IOException`; consistent error messages and Indonesian action
  labels.
- Removed duplicate dependencies; kept `material-icons-core`.
- `gradlew` set to `100755`.

`main` was then merged **into `reina`** with
`--allow-unrelated-histories`. The add/add conflicts were resolved once, on
the feature branch, so the PR itself was clean and normal:

| File | Resolution |
|---|---|
| `.gitignore` | `main` (includes `node_modules/`) |
| `.idea/gradle.xml` | `main` |
| `app/build.gradle.kts` | `main`’s deduplicated block + `material-icons-core` |
| `AndroidManifest.xml` | `main`’s valid manifest (INTERNET + network config) |
| `MainActivity.kt` | `reina` (TodoScreen) |
| `Todo.kt` | `reina` |
| `ApiService.kt` | `reina` (suspend + Unit) |
| `RetrofitClient.kt` | `reina` (10.0.2.2) |
| `TodoViewModel.kt` | `reina` (suspend) |
| `network_security_config.xml` | `reina` (10.0.2.2) |
| `Theme.kt`, `TodoScreen.kt` | clean additions from `reina` |

`reina` and `surya-coroutines` were pushed to the same final commit
(`cc8c912`), so both refs stay in sync.

## 4. Android 17 local-network blocker

After the merge, the app built and installed but every request timed out after
10 seconds with **no request in the Node log**:

```
<-- HTTP FAILED: java.net.SocketTimeoutException:
failed to connect to /10.0.2.2 (port 3000) from /10.0.2.15 (port 42052) after 10000ms
```

Diagnosis:

- `adb shell` raw HTTP to `10.0.2.2:3000` worked; the server logged it.
- The app’s `appops` showed `ACCESS_LOCAL_NETWORK: ignore` with a `rejectTime`
  matching each attempt.
- The project targets SDK 37. Android 17 enforces the
  `ACCESS_LOCAL_NETWORK` runtime permission for local-network traffic and
  silently drops packets when it is missing.

Fix:

- Declared `android.permission.ACCESS_LOCAL_NETWORK` in the manifest.
- Request the permission in `MainActivity` **before** `setContent`, so the
  first `fetchTodos()` runs after the grant.
- On API < 37 the request is skipped.

Verified on Pixel_10 (API 37): GET `200`, POST `201`, PUT `204`, DELETE `204`,
and state persisted after an app restart.

## 5. Verification

### Build

```bash
./gradlew clean :app:assembleDebug   # BUILD SUCCESSFUL
```

### API contract

`todo-api/smoke-test.js` output:

| Request | Expected | Actual | Body | Result |
|---|---|---|---|---|
| GET /todos | 200 | 200 | yes | PASS |
| POST /todos (valid) | 201 | 201 | yes | PASS |
| PUT /todos/{id} | 204 | 204 | no | PASS |
| GET /todos (after PUT) | 200 | 200 | yes | PASS |
| DELETE /todos/{id} | 204 | 204 | no | PASS |
| DELETE /todos/{id} (again) | 404 | 404 | yes | PASS |
| POST /todos (blank title) | 400 | 400 | yes | PASS |
| DELETE /todos/9999 | 404 | 404 | yes | PASS |

Note: `DELETE /todos/9999` returns a JSON message, so “Ada body?” is **yes**,
not “Tidak” as written in the draft report. The code matches the module’s
`server.js`.

### Emulator flow

- Seeded list: `Eat`, `Sleep` (checked), `Work`.
- Add `Drink` → POST `201`; MySQL row `id=5`.
- Toggle `Eat` → PUT `204`; MySQL `completed=1`.
- Delete `Work` → DELETE `204`; row removed.
- Force-stop + relaunch → list reflects the new state.

### Branch ancestry

All seven refs are ancestors of `main`:

```
candra: MERGED
putra-retrofit: MERGED
salma-viewmodel: MERGED
surya: MERGED
andry-njakatiana: MERGED
reina: MERGED
surya-coroutines: MERGED
```

## 6. Rollback

- Safety tags `pre-merge/*` are pushed to `origin`.
- To undo a merge commit: `git revert -m 1 <merge-sha>`.
- To restore a branch: reset it to its `pre-merge/*` tag (requires a
  force-push, only with explicit approval).
- No force-push to `main` was performed.

## 7. Remaining pedagogical work

Bagian 8 (Logcat trace + table 6.3) and Bagian 9 (E1–E7 experiments) are not
part of the merge; they must be executed on the callback revision (before the
Bagian 10 suspend refactor) so the `onResponse`/`onFailure` questions match
the module. The final suspend app remains the verified production state.

Bagian 11 (bonus challenge) is not implemented in any branch.
