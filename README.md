# TodoApp — Retrofit + Kotlin Coroutines Hands-on

Android Jetpack Compose client + Node.js/MySQL REST API for the PAM hands-on
**“Mengakses REST API dengan Retrofit (Studi Kasus Todo List)”**.

The app follows the module’s data flow:

```
TodoScreen (View)
  -> TodoViewModel (StateFlow + viewModelScope)
  -> Retrofit ApiService / RetrofitClient / OkHttp
  -> Express REST API
  -> MySQL tododb.todos
  -> Gson -> ViewModel StateFlow -> recomposition
```

## Repository layout

```
.
├── app/                         Android app (Kotlin DSL, Compose)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/id/ac/ub/todoapp/
│       │   ├── MainActivity.kt
│       │   ├── data/model/Todo.kt
│       │   ├── data/remote/ApiService.kt
│       │   ├── data/remote/RetrofitClient.kt
│       │   └── ui/TodoScreen.kt, TodoViewModel.kt, theme/Theme.kt
│       └── res/xml/network_security_config.xml
├── todo-api/                    Node.js REST API (Bagian 1–2)
│   ├── server.js
│   ├── schema.sql
│   ├── smoke-test.js
│   ├── package.json
│   └── .env.example
├── docs/INTEGRATION.md          Merge/integration audit
└── gradle/, gradlew, ...
```

## Prerequisites

| Tool | Version |
|---|---|
| Node.js | 18 LTS or newer |
| MySQL | 8.x or newer |
| Android Studio | current stable |
| JDK | 25 (the Gradle wrapper pins Java 25) |
| Android SDK | compileSdk 37; minSdk 24 |

## 1. Database

```bash
mysql -u root -p < todo-api/schema.sql
```

`schema.sql` is idempotent: it creates `tododb` and `todos` only if missing and
inserts the three seed rows only when the table is empty. It never deletes
existing todos.

Expected verification:

```sql
SELECT * FROM tododb.todos;   -- Eat 0, Sleep 1, Work 0
DESCRIBE tododb.todos;        -- completed is tinyint(1)
```

## 2. REST API

```bash
cd todo-api
npm ci
npm start
```

If the MySQL password is not empty:

```bash
DB_PASSWORD=rahasia npm start
```

The server listens on `0.0.0.0:3000` and logs every request, which is useful
when comparing against Logcat.

Run the contract smoke test in another terminal:

```bash
cd todo-api
npm run smoke
```

It asserts the endpoint contract from Bagian 2:

| Request | Expected |
|---|---|
| `GET /api/todos` | `200` + JSON array |
| `POST /api/todos` (valid) | `201` + todo with `id` |
| `PUT /api/todos/{id}` | `204`, empty body |
| `DELETE /api/todos/{id}` | `204`, empty body |
| `DELETE /api/todos/9999` | `404` + JSON message |
| `POST /api/todos` (blank title) | `400` + JSON message |

## 3. Android app

Open the repository root in Android Studio, or build from the terminal:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Emulator

`RetrofitClient.BASE_URL` is:

```kotlin
http://10.0.2.2:3000/api/
```

`10.0.2.2` is the emulator alias for the host machine. The matching
`res/xml/network_security_config.xml` permits cleartext HTTP only for that
domain (development only).

### Physical device

1. Put the phone and computer on the same Wi-Fi.
2. Allow port `3000` through the computer firewall.
3. Change `BASE_URL` to `http://<IP-LAN-komputer>:3000/api/`.
4. Add the same IP to `network_security_config.xml` and rebuild.

### Android 17 / API 37 note

Android 17 blocks local-network traffic for apps targeting API 37 unless the
`ACCESS_LOCAL_NETWORK` runtime permission is granted. Without it, TCP to
`10.0.2.2` is **silently dropped** (10-second timeout, no server log).

This project declares the permission and requests it before composing
`TodoScreen`:

```xml
<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
```

On API < 37 the request is skipped. On API 37, tap **Allow** on first launch.
If the permission is denied, the ViewModel shows the network error and the
**Coba lagi** button retries.

## API contract

| Method | URL | Body | Success | Retrofit |
|---|---|---|---|---|
| GET | `/api/todos` | — | `200` + array | `suspend getTodos(): Response<List<Todo>>` |
| POST | `/api/todos` | `{title, completed}` | `201` + todo | `suspend addTodo(...): Response<Todo>` |
| PUT | `/api/todos/{id}` | `{title, completed}` | `204` | `suspend updateTodo(...): Response<Unit>` |
| DELETE | `/api/todos/{id}` | — | `204` | `suspend deleteTodo(...): Response<Unit>` |

The model is `Todo(id: Int = 0, title: String, completed: Boolean = false)`.
`id = 0` is ignored by the server; MySQL `AUTO_INCREMENT` assigns the real id.

## Verification performed

- `./gradlew clean :app:assembleDebug` → **BUILD SUCCESSFUL**
- `npm run smoke` → **all 8 checks PASS**
- Emulator Pixel_10 (API 37): list loads (`Eat`, `Sleep`, `Work`), add “Drink”
  → `201` + row in MySQL, toggle “Eat” → `204` + `completed=1`, delete “Work”
  → `204`, reopen app → state persists.
- All seven branches are ancestors of `main` (see `docs/INTEGRATION.md`).

## Branch provenance

| Branch | Contribution | Final state |
|---|---|---|
| `main` | Initial Android skeleton | base |
| `candra` | Bagian 1–2: MySQL/Express API | merged via PR #1 |
| `putra-retrofit` | Bagian 4–5: model + Retrofit | merged via PR #2 ancestry |
| `salma-viewmodel` | Bagian 3 + 6: manifest/config + ViewModel | merged via PR #2 |
| `surya` / `andry-njakatiana` | Bagian 7: View/theme snapshot | merged via PR #3 ancestry |
| `reina` / `surya-coroutines` | Bagian 10: suspend refactor | merged via PR #3 |

## Intentional deviations from the module snippet

| Change | Why |
|---|---|
| API lives in `todo-api/` | Matches Bagian 2.2 and keeps the Android project at the repo root |
| `schema.sql` is idempotent | Safe to re-run; no accidental data loss |
| `DB_HOST` defaults to `127.0.0.1` | Avoids macOS `localhost` → IPv6 (`::1`) resolution issues |
| `ACCESS_LOCAL_NETWORK` permission | Required by Android 17 / targetSdk 37; otherwise `10.0.2.2` times out silently |
| `gradlew` mode `100755` | Fresh clones can run `./gradlew` |
| `material-icons-core` dependency | `Icons.Default.Add/Delete` used by `TodoScreen` |

## Troubleshooting

| Symptom | Check |
|---|---|
| `CLEARTEXT communication ... not permitted` | `networkSecurityConfig` in manifest and `10.0.2.2` in the XML |
| `Failed to connect to /10.0.2.2:3000` (10s timeout) | API running? port 3000? On API 37: `ACCESS_LOCAL_NETWORK` granted? |
| `ECONNREFUSED` in Node | MySQL running? `DB_PASSWORD` correct? |
| `baseUrl must end in /` | `BASE_URL` must end with `/` |
| `UnknownHostException` | `INTERNET` permission missing or no connection |
| `Expected BEGIN_ARRAY but was BEGIN_OBJECT` | Server returned an error object instead of an array |
| `ER_NOT_SUPPORTED_AUTH_MODE` | Use `mysql2`, not `mysql` |

Development only: HTTP cleartext and `Level.BODY` logging must not be used in
production.
