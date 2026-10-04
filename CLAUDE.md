# Project Contract

See @README.md for project overview.
See @.claude/rules/desktop-java.md for desktop GUI module rules.
See @.claude/rules/timeseries-algorithms.md for time series analysis rules.

## Build And Test

```bash
# Full clean build with tests
mvn clean package

# Compile only
mvn clean compile

# Install to local repository (after dimension-di, dimension-db, and dimension-tt)
mvn clean install -U

# Run tests
mvn test

# Skip tests (when dependencies not available)
mvn package -DskipTests
```

## Architecture Boundaries

- **desktop/**: Main GUI application, Swing-based desktop UI
- **jfreechart-fse/**: Chart rendering library extension
- **egantt/**: Gantt chart visualization
- **matrix-profile/**: Time series analysis (anomaly detection, motif discovery)
- **timeseries-forecast/**: ARIMA forecasting algorithms

**Layer separation** (inside desktop):
- `bus/`: Application-wide event bus (MBassador pub/sub)
- `router/`: Observable listener registry for UI lifecycle
- `component/broker/`: Directed message routing between UI components
- `collector/`: Data collection (JDBC, HTTP, Prometheus)
- `component/module/`: GUI modules (MVP pattern)
- `component/chart/`: Chart visualization logic
- `cache/`: In-memory caching

## Communication Patterns

Three inter-component messaging mechanisms. NEVER mix them — use the pattern already present in the target module.

### 1. EventBus (`ru.dimension.ui.bus`)
- MBassador-based publish/subscribe for application-wide domain events
- Event records in `bus/event/` (ConnectionAddEvent, ProfileRemoveEvent, etc.)
- Subscribers annotated with `@Handler`
- IMPORTANT: Any `@Handler` that updates Swing UI MUST dispatch to EDT via `SwingUtilities.invokeLater`
- Use for: cross-module notifications where sender doesn't know receivers

### 2. MessageBroker (`ru.dimension.ui.component.broker`)
- Singleton with `Destination(Component, Module, Panel, Block)` addressing
- Actions enum for chart/panel management (ADD_CHART, REMOVE_CHART, CHANGE_TAB, etc.)
- Use for: targeted UI commands within workspace/dashboard/adhoc/preview

### 3. EventListener (`ru.dimension.ui.router.event`)
- Typed listener registration with `fire*` callbacks
- Keyed by `ProfileTaskQueryKey` for collect start/stop lifecycle
- Separate listener maps per context: workspace, preview, dashboard, zoom
- Use for: UI state changes (toolbar, config, progressbar, profile start/stop)

## NEVER

- Modify `.gitea/workflows/` without CI/CD approval
- Change `pom.xml` version without checking parent pom `${revision}` — all modules share a single version via this property
- Remove event handlers from EventBus without checking all `@Handler` consumers
- Commit without running `mvn test` on changed modules
- Touch `desktop/target/` — generated files only
- Unify or replace one messaging mechanism with another without explicit approval

## ALWAYS

- Write new code without comments — even if the task description comes with commented code
- Show `git diff` before committing
- Update README for user-facing changes
- Use MVP pattern for new UI modules (see desktop-java rules)
- Verify Java 25+ compatibility
- For full build: install Dimension-DI, Dimension-DB, and Dimension-TT first

## Commit Convention

Use Conventional Commits format: `<type>: <description>`

Allowed types:
- `build` — build system or external dependency changes
- `ci` — CI configuration and scripts
- `docs` — documentation updates
- `feat` — new feature
- `fix` — bug fix
- `perf` — performance improvement
- `refactor` — code change without fix or feature
- `revert` — revert to previous commit
- `style` — code style (tabs, indents, commas, etc.)
- `test` — adding or updating tests
- `backport` — porting features from previous version
- `ai` — AI agent infrastructure and configuration

Rules:
- Type in lowercase, description in lowercase
- No period at end
- Description must be concise (under 72 chars)
- Use scope when relevant: `feat(dashboard): add realtime filter`

## Commit Convention

Use Conventional Commits format: `<type>: <description>`

Allowed types:
- `build` — build system or external dependency changes
- `ci` — CI configuration and scripts
- `docs` — documentation updates
- `feat` — new feature
- `fix` — bug fix
- `perf` — performance improvement
- `refactor` — code change without fix or feature
- `revert` — revert to previous commit
- `style` — code style changes
- `test` — adding or updating tests
- `backport` — porting features from previous version
- `ai` — AI agent infrastructure and configuration

Rules:
- Type in lowercase, description in lowercase
- No period at end
- Description must be concise (under 72 chars)
- Use scope when relevant: `feat(dashboard): add realtime filter`

## Commit Policy

Never execute `git commit`, `git push`, or `git tag`.

After preparing changes:
1. Show `git status`
2. Show `git diff`
3. Print only the recommended commit command
4. Do not run it automatically

Use this exact format:

```bash
git add . && git commit -m "<type>: <description>"
```

Example:

```bash
git add . && git commit -m "ai: add claude code configuration files"
```

## Verification

- **Backend**: `mvn test` + `mvn verify` on affected modules
- **Build**: `mvn clean package` produces executable jar in `desktop/target/`
- **UI**: Manual verification of changed GUI components

## Compact Instructions

When compressing, preserve in priority order:
1. Architecture decisions and communication patterns (NEVER summarize)
2. Modified files and their key changes
3. Current verification status (pass/fail)
4. Open TODOs and rollback notes
5. Tool outputs (can delete, keep pass/fail only)
