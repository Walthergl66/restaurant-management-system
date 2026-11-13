# Repository Guidelines

## Project Structure & Modules

This repository is a monorepo. `backend/` contains the Java/Spring Boot modular API, migrations, and tests; `frontend/` contains the Next.js web app; `mobile/` contains the Expo app; `database/` contains schema references and backup/restore scripts; and `docs/` contains product, architecture, API, and operations documentation. Backend modules live under `backend/src/main/java/com/restaurante/<module>` and generally separate `domain`, `application`, `web`, and `infrastructure` responsibilities. Tests are under `backend/src/test/`.

## Build, Test, and Development

- `cd backend && ./mvnw clean package` builds the API.
- `cd backend && ./mvnw test` runs JUnit tests, including Testcontainers tests that require Docker.
- `cd frontend && npm ci && npm run dev` installs and starts the web app.
- `cd frontend && npm run lint && npm run build` checks and builds the web app.
- `cd mobile && npm install && npx expo start` starts the mobile development server.
- `docker compose up --build -d` starts the API and web containers; configure the external PostgreSQL connection in `.env` or `backend/.env` first.

## Coding Style & Naming

Follow the existing conventions in each module. Java packages and classes use descriptive lowercase package names and `UpperCamelCase` types; frontend and mobile use TypeScript, `UpperCamelCase` components, and `camelCase` functions and variables. Keep backend module boundaries intact and expose cross-feature frontend imports through each feature's `index.ts` barrel. Use the existing ESLint configuration for frontend style; no repository-wide formatter is configured.

## Testing

Backend tests use JUnit 5, Spring test support, Testcontainers, Spring Modulith, and ArchUnit. Name tests descriptively with `Test` suffix, following nearby examples. Run `./mvnw test` before submitting backend changes. For web changes, run `npm run lint` and `npm run build`; add or update focused tests where the affected area has test coverage. CI runs backend tests and web lint/build.

## Commits & Pull Requests

Recent commits use concise, imperative subjects, often with conventional prefixes such as `feat`, `fix`, `docs`, and `chore`, optionally scoped (for example, `docs(backend): ...`). Keep commits focused. Pull requests should explain the user-visible change, list validation commands and results, link related issues when available, and include screenshots for visual web or mobile changes.

## Security & Configuration

Never commit real `.env` files, passwords, API keys, or JWT secrets. Use the checked-in `.env.example` templates. The database is external to Compose; Flyway applies migrations on backend startup, so use an appropriately scoped database account and verify connectivity before running the stack.

## Documentation Maintenance

For every change, assess whether behavior, interfaces, data, configuration, operations, or architecture affect the documentation. Update the relevant `docs/` topic and component README in the same change whenever needed; keep `docs/README.md` links current. Document user-visible capabilities and changed behavior from code or verified requirements, and mark unknown policy or product decisions as pending instead of guessing. Keep substantive documentation grouped by product, module, architecture, API, or operations. Include documentation updates in the change's review and commit history.

## Required Project Context Before Code Changes

Before making the first code change in a task, read `docs/README.md` and review the project documentation relevant to the affected component and behavior, including its architecture, module/function, API, and operational guidance as applicable. Also inspect the component README and nearby implementation/tests. Use this context to preserve existing contracts and module boundaries; do not start coding from an isolated file or assumption. If the documentation conflicts with the implementation, verify the source and update the relevant docs as part of the task.
