# BugTrack Selenium Automation

End-to-end test automation for the **BugTrack** bug-tracking app
([bugtracker-fullstack](https://github.com/Sonalisahu728/bugtracker-fullstack): HTML/CSS/JS frontend, Spring Boot + JWT backend, PostgreSQL).

Built with **Java 17, Selenium WebDriver 4, TestNG, REST Assured, Datafaker, Maven** and run in CI with **GitHub Actions**.

## What is tested

| Area | UI (Selenium) | API (REST Assured) |
|---|---|---|
| Registration & login | valid/invalid login, duplicate email, password boundary (5 vs 6 chars), invalid email, logout clears JWT, protected-page redirect | 201 + JWT, 400 field errors, 401 wrong password |
| Role-based access (Developer / Team Lead / Admin) | which buttons each role sees, who can assign, who can delete | 403 for Developer on create project / assign / delete, project-owner-only member add |
| Projects | create project, invite members, required name | list, create |
| Issues | report issue, default priority P2, required title, filters (priority/type), assign list per role, delete only when CLOSED | defaults, validation, filters, assign, delete rules |
| **Kanban drag-and-drop** | full status-transition matrix: 6 valid + 6 invalid moves, error toast, card stays put | same matrix at API level (200 vs 409 + message) |
| Comments | post, empty state, blank ignored, oldest-first order | 201, blank -> 400 |
| JWT security | redirect without token | no token, tampered signature, garbage token |

About 94 test executions in the regression suite (52 UI + 42 API), plus 2 "known defect" tests (see below).

## Framework design

```
src/main/java/.../automation
  config/    ConfigReader        config.properties + -D / BT_* overrides
  driver/    DriverFactory       Chrome / Firefox / Edge, headless flag, ThreadLocal (parallel-safe)
  pages/     BasePage, LoginPage, RegisterPage, DashboardPage, BoardPage    (Page Object Model)
  api/       ApiClient           REST Assured wrapper used for setup and for API tests
  model/     Role, TestUser
  utils/     TestDataFactory (Datafaker + UUID), BrowserSession (hybrid login), ScreenshotUtil
src/test/java/.../automation
  base/      BaseUiTest, BaseApiTest, TransitionData (shared data provider)
  listeners/ TestListener        screenshot on failure
  ui/        LoginTest, RegistrationTest, ProjectTest, IssueTest, StatusTransitionTest, CommentTest, DeleteIssueTest
  api/       AuthApiTest, IssueApiTest, SecurityApiTest
testng.xml  testng-smoke.xml  testng-api.xml  testng-known-defects.xml
.github/workflows/ci.yml
```

Key decisions:

- **Hybrid tests.** Users, projects and issues are created through the API; the JWT is put into `localStorage`
  (`bt_token` / `bt_user`, exactly like `js/api.js`). UI tests then check only what they are about, and run fast.
- **Independent, parallel-safe data.** Every test creates its own users/project/issue with unique names. No shared state, no cleanup order.
- **No `Thread.sleep`, no implicit wait.** Only explicit waits (`WebDriverWait` + `ExpectedConditions`) and toast/state waits.
- **Drag-and-drop** uses JavaScript `DragEvent` + `DataTransfer`, because Selenium's `Actions` class cannot drive native HTML5 drag-and-drop.
- **Data-driven.** TestNG `@DataProvider` for roles and for the status-transition matrix (shared by UI and API tests so the rules live in one place).
- **Groups:** `smoke`, `regression`, `auth`, `rbac`, `kanban`, `issues`, `comments`, `filters`, `security`, `api`, `known-defect`.

## Run it locally

Prerequisites: JDK 17+, Maven, Chrome (Selenium Manager downloads the matching driver), PostgreSQL with a database `bugtracker_db`.

```bash
# 1. start the application under test
cd bugtracker-fullstack/backend && mvn spring-boot:run                  # http://localhost:8080
cd bugtracker-fullstack/frontend && python3 -m http.server 5500         # http://localhost:5500

# 2. run the tests (from this project)
mvn test                                         # full regression (testng.xml)
mvn test -DsuiteXmlFile=testng-smoke.xml         # smoke only
mvn test -DsuiteXmlFile=testng-api.xml           # API only, no browser
mvn test -Dbrowser=firefox -Dheadless=true       # other browser / headless
mvn test -Dbase.url=http://localhost:5500 -Dapi.url=http://localhost:8080/api
```

Reports: `target/surefire-reports/index.html` (and `emailable-report.html`). Failure screenshots: `target/screenshots/`.

## CI

`.github/workflows/ci.yml` runs on push, pull request, nightly and manually. It starts PostgreSQL as a service,
builds and starts the backend, serves the frontend, runs the suite headless in Chrome, and uploads reports,
screenshots and the backend log as artifacts.

## Known-defect tests

`testng-known-defects.xml` holds tests that assert the **correct** behaviour for suspected defects found while reading the
backend source. They are expected to fail until the defect is fixed, and are excluded from the normal suites.

| Test | Expected | Why it is suspected |
|---|---|---|
| `developerCannotCreateIssueViaApi` | 403 | The UI hides "Report Issue" for Developers, but `SecurityConfig` does not restrict `POST /api/issues`. |
| `unknownPriorityValueReturns400` | 400 | An invalid enum value (e.g. `P9`) is not handled by `GlobalExceptionHandler`, so the catch-all returns 500. |
