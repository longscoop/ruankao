# Service Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make server, admin, and miniapp verification reproducible in Java 17 / Node 22 and leave all three existing pipelines green without adding unfinished V1 features.

**Architecture:** Treat the declared runtimes and lockfiles as the build boundary. First make generated files, Node runtime selection, dependency resolution, and CI installation deterministic; then run the unchanged product suites in their supported environments. The server suite is verified in a Java 17 container against PostgreSQL Testcontainers, so the local Java 22 `localhost` resolver failure is not patched into application or test code.

**Tech Stack:** Java 17, Maven 3.9, Spring Boot 3.3, Testcontainers PostgreSQL 16, Node.js 22, npm 10, Vue 3, Vite, uni-app, GitHub Actions

**Spec:** `docs/superpowers/specs/2026-09-22-service-stability-design.md`

## Global Constraints

- Work only on `feat/soft-exam-v1`; never implement directly on `main`.
- Server verification uses Java 17 and `mvn -B verify`.
- Admin and miniapp verification use Node 22 and lockfile-backed `npm ci`.
- Do not change server production code or the shared PostgreSQL fixture: Java 17 already passes all 126 tests and the JaCoCo gate.
- Do not implement unfinished Phase 6 or Phase 8 product scope.
- Do not add real credentials, API keys, WeChat secrets, payment keys, or production database settings.
- Commit each task independently.

## Review Focus

- Node 20 or another unsupported major must fail at dependency installation with a clear engine error instead of reaching `--experimental-strip-types`.
- A package manifest / lockfile mismatch must make `npm ci` fail instead of silently resolving a different dependency graph.
- Generated `target`, `node_modules`, `dist`, coverage, and local environment files must stay out of Git status after a full run.
- `.env.example` files must remain trackable even though real `.env` variants are ignored.
- Java 17 verification must exercise the real PostgreSQL Testcontainer and all 126 tests while leaving the host working tree unchanged.

---

## File Structure

- `.gitignore`: repository-wide generated-output and secret-local-file exclusions.
- `.nvmrc`: canonical Node major for local development.
- `.npmrc`: enforce the declared Node engine during npm operations.
- `admin/package.json`: admin Node engine contract.
- `admin/package-lock.json`: deterministic admin dependency graph.
- `admin/README.md`: admin setup with Node 22 and `npm ci`.
- `miniapp/package.json`: miniapp Node engine contract.
- `miniapp/package-lock.json`: deterministic miniapp dependency graph.
- `miniapp/README.md`: miniapp setup with lockfile-backed installation.
- `.github/workflows/admin-ci.yml`: admin CI cache and `npm ci` execution.
- `.github/workflows/miniapp-ci.yml`: miniapp CI cache and `npm ci` execution.
- `docs/superpowers/specs/2026-09-22-service-stability-design.md`: final evidence and completion record.

### Task 1: Reproducible runtime, dependency, and workspace contract

**Files:**
- Create: `.gitignore`
- Create: `.nvmrc`
- Create: `.npmrc`
- Modify: `admin/package.json`
- Create: `admin/package-lock.json`
- Modify: `admin/README.md`
- Modify: `miniapp/package.json`
- Create: `miniapp/package-lock.json`
- Modify: `miniapp/README.md`
- Modify: `.github/workflows/admin-ci.yml`
- Modify: `.github/workflows/miniapp-ci.yml`

**Interfaces:**
- Consumes: Node 22 installed at `/Users/qilong/.nvm/versions/node/v22.23.2/bin`, npm package manifests, and the existing `check` scripts.
- Produces: `.nvmrc` value `22`, package engine range `>=22 <23`, npm engine enforcement, deterministic lockfiles, and CI workflows that install exclusively with `npm ci`.

- [ ] **Step 1: Verify the missing contract is RED**

Run from the repository root:

```bash
test -f .nvmrc
git check-ignore -q server/target
(cd admin && npm ci --ignore-scripts)
(cd miniapp && npm ci --ignore-scripts)
```

Expected before implementation:

- `test -f .nvmrc` exits 1.
- `git check-ignore -q server/target` exits 1.
- Both `npm ci` commands fail with npm `EUSAGE` because no lockfile exists.

- [ ] **Step 2: Add the repository runtime and ignore contract**

Create `.nvmrc`:

```text
22
```

Create `.npmrc`:

```ini
engine-strict=true
```

Create `.gitignore`:

```gitignore
.DS_Store
.idea/
.vscode/

**/target/
**/node_modules/
**/dist/
**/coverage/

**/.env
**/.env.*
!**/.env.example
```

- [ ] **Step 3: Declare Node 22 in both package manifests**

Add the same block immediately after `"type": "module"` in `admin/package.json` and after `"private": true` in `miniapp/package.json`:

```json
"engines": {
  "node": ">=22 <23",
  "npm": ">=10"
},
```

- [ ] **Step 4: Generate deterministic lockfiles with the explicit Node 22 installation**

Run:

```bash
export PATH="/Users/qilong/.nvm/versions/node/v22.23.2/bin:$PATH"
node --version
npm --version
(cd admin && npm install --package-lock-only --ignore-scripts --registry=https://registry.npmjs.org)
(cd miniapp && npm install --package-lock-only --ignore-scripts --registry=https://registry.npmjs.org)
```

Expected: Node reports `v22.23.2`; both commands exit 0 and create lockfile version 3 without creating business data.

- [ ] **Step 5: Update local setup documentation**

Change the Admin `Run` section to:

````markdown
Requires Node.js 22.

```bash
nvm use
npm ci
cp .env.example .env
npm run dev
```
````

Change the miniapp development commands to:

````markdown
要求 Node.js 22。

```bash
nvm use
npm ci
cp .env.example .env.local
npm run dev:mp-weixin
```
````

- [ ] **Step 6: Make CI consume the lockfiles**

In Admin CI, configure npm caching on `actions/setup-node@v4`:

```yaml
with:
  node-version: '22'
  cache: npm
  cache-dependency-path: 'admin/package-lock.json'
```

In Miniapp CI, use the corresponding configuration:

```yaml
with:
  node-version: '22'
  cache: npm
  cache-dependency-path: 'miniapp/package-lock.json'
```

Replace each workflow's install/test/typecheck/build step sequence with:

```yaml
- run: npm ci
- run: npm run check
```

- [ ] **Step 7: Verify engine enforcement and ignore behavior**

Run from the repository root with the current system Node 20 first:

```bash
(cd admin && /usr/local/bin/npm install --package-lock-only --ignore-scripts)
```

Expected: FAIL with `EBADENGINE` because `.npmrc` enables `engine-strict` and the package requires Node 22.

Then verify ignore rules:

```bash
git check-ignore server/target admin/node_modules admin/dist miniapp/node_modules miniapp/dist admin/.env miniapp/.env.local
if git check-ignore admin/.env.example miniapp/.env.example; then exit 1; fi
```

Expected: all generated/private paths print as ignored; both example files remain unignored.

- [ ] **Step 8: Verify clean installs and complete frontend pipelines under Node 22**

Run:

```bash
export PATH="/Users/qilong/.nvm/versions/node/v22.23.2/bin:$PATH"
(cd admin && npm ci --registry=https://registry.npmjs.org && npm run check)
(cd miniapp && npm ci --registry=https://registry.npmjs.org && npm run check)
```

Expected:

- Admin: all Node tests pass, `vue-tsc --noEmit` passes, and Vite production build passes.
- Miniapp: all Node tests pass, `vue-tsc --noEmit` passes, and `uni build -p mp-weixin` passes.

If a behavior test fails after installation succeeds, stop Task 1, preserve the exact failure output, and create a defect-specific RED -> GREEN task before changing production code.

- [ ] **Step 9: Review and commit Task 1**

Run:

```bash
git diff --check
git diff -- .gitignore .nvmrc .npmrc admin/package.json admin/README.md miniapp/package.json miniapp/README.md .github/workflows/admin-ci.yml .github/workflows/miniapp-ci.yml
git status --short
```

Confirm lockfiles are the only large generated diffs and no dependency ranges changed. Commit:

```bash
git add .gitignore .nvmrc .npmrc admin/package.json admin/package-lock.json admin/README.md miniapp/package.json miniapp/package-lock.json miniapp/README.md .github/workflows/admin-ci.yml .github/workflows/miniapp-ci.yml
git commit -m "build: make frontend verification reproducible"
```

### Task 2: CI-equivalent full-service verification and evidence

**Files:**
- Modify: `docs/superpowers/specs/2026-09-22-service-stability-design.md`

**Interfaces:**
- Consumes: Task 1's Node 22 lockfile contract, existing server Maven suite, Docker/OrbStack, and PostgreSQL Testcontainers.
- Produces: fresh evidence for all three pipelines and a committed completion record; no server source or test-fixture change.

- [ ] **Step 1: Verify the server in Java 17 with a real PostgreSQL Testcontainer**

Run from the repository root:

```bash
docker run --rm --name ruankao-java17-verify \
  -v /Users/qilong/rotbot/ruankao/server:/workspace:ro \
  --tmpfs /workspace/target:rw,exec \
  -v ruankao-m2-cache:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn -B verify
```

Expected: 126 tests, 0 failures, 0 errors; JaCoCo reports `All coverage checks have been met`; build exits 0. The read-only source mount and tmpfs target ensure this verification cannot add Maven output to the repository.

- [ ] **Step 2: Re-run frontend checks from lockfile-backed installs**

Run:

```bash
export PATH="/Users/qilong/.nvm/versions/node/v22.23.2/bin:$PATH"
(cd admin && npm ci --registry=https://registry.npmjs.org && npm run check)
(cd miniapp && npm ci --registry=https://registry.npmjs.org && npm run check)
```

Expected: both commands exit 0 with all unit tests, type checks, and production builds passing.

- [ ] **Step 3: Verify dependency drift and workspace hygiene**

Run:

```bash
git diff --exit-code -- admin/package-lock.json miniapp/package-lock.json
git status --short
```

Expected: `npm ci` does not change either lockfile. Git status shows no generated `target`, `node_modules`, `dist`, coverage, or environment files.

- [ ] **Step 4: Record exact completion evidence**

Append this section to the design document, replacing counts only with the fresh outputs observed in Steps 1-3:

```markdown
## Completion Evidence

- Java 17 server verification: 126 tests, 0 failures, 0 errors; JaCoCo Learning Engine coverage gate passed.
- Node 22 admin verification: unit tests, TypeScript typecheck, and Vite production build passed from `npm ci`.
- Node 22 miniapp verification: unit tests, TypeScript typecheck, and mp-weixin production build passed from `npm ci`.
- Repository hygiene: full verification left generated outputs ignored and both lockfiles unchanged.
- The Java 22 `UnknownHostException: localhost` result was environment-only; no server production or test-fixture workaround was introduced.
```

- [ ] **Step 5: Self-review and commit Task 2**

Run:

```bash
git diff --check
git diff -- docs/superpowers/specs/2026-09-22-service-stability-design.md
git status --short
```

Commit only the evidence update:

```bash
git add docs/superpowers/specs/2026-09-22-service-stability-design.md
git commit -m "docs: record service stability verification"
```

- [ ] **Step 6: Final fresh verification before reporting completion**

Run Task 2 Steps 1-3 again after the evidence commit. Report exact counts and exit codes. Do not claim completion if any command is red; name every failing command and preserve its failure output for the next defect-specific task.
