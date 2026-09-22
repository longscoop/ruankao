# Service Stability Test and Repair Design

**Date:** 2026-09-22  
**Branch:** `feat/soft-exam-v1`  
**Source of truth:** `docs/specs/soft-exam-v1.md`, `docs/implementation-plan.md`, and the completed PDF import plan

## Goal

Make the existing server, admin, and miniapp verification pipelines reproducible and green in their declared runtime environments, then repair only regressions demonstrated by those pipelines. This stabilization pass does not implement unfinished Phase 6 or Phase 8 product features.

## Baseline Evidence

- The repository is on the required feature branch and was clean before the first test run.
- Server `mvn -B verify` discovered 126 tests. Pure Java learning-engine, storage, and PDF parser tests passed, while 84 integration-test errors shared one infrastructure symptom: Flyway could not connect to the Testcontainers PostgreSQL URL because the Java 22 process reported `UnknownHostException: localhost`.
- Admin and miniapp tests exited before collection because the active Codex process used Node 20, which does not support the configured `--experimental-strip-types` option. Both CI workflows explicitly use Node 22.
- Admin and miniapp have no committed lockfiles, while CI uses `npm install`; dependency resolution is therefore not reproducible.
- The repository does not ignore `server/target/`, so a normal Maven run dirties the working tree.

## Chosen Approach

Treat environment and build reproducibility as the first test boundary, then distinguish environment-only failures from product defects before changing production code.

1. Declare the supported runtimes: Java 17 and Node 22.
2. Add repository-level ignore rules for generated Maven, npm, and build output.
3. Generate and commit npm lockfiles with Node 22, and change both frontend workflows to use `npm ci`.
4. Run the server suite with Java 17 and PostgreSQL Testcontainers. If the Java 17 run is green, record the Java 22 failure as an unsupported local-runtime mismatch and do not change the test harness. If it still fails, reduce it to one integration test, add the smallest regression test or diagnostic assertion that demonstrates the host-resolution defect, and repair the shared test fixture.
5. Run admin and miniapp unit tests, type checks, and production builds under Node 22.
6. For each real behavior failure found after the environment is aligned, follow one independent RED -> GREEN cycle and commit it separately.

This approach avoids masking product failures with environment workarounds and avoids changing production behavior merely to accommodate an unsupported toolchain.

## Files and Boundaries

Expected build-reproducibility changes:

- root `.gitignore`
- root runtime declaration such as `.nvmrc`
- `admin/package.json` and `admin/package-lock.json`
- `miniapp/package.json` and `miniapp/package-lock.json`
- `.github/workflows/admin-ci.yml`
- `.github/workflows/miniapp-ci.yml`
- developer setup documentation where the runtime contract needs to be visible

Server test-fixture or production files will be changed only if a Java 17 reproduction proves an actual defect. No database schema or API contract change is planned.

## Testing Strategy

The stabilization work is split into independently verifiable tasks:

1. **Runtime and dependency reproducibility**
   - Demonstrate the current missing-runtime/missing-lockfile behavior.
   - Add runtime declarations and lockfiles.
   - Verify clean installation with `npm ci`.
2. **Admin verification**
   - `npm test`
   - `npm run typecheck`
   - `npm run build`
3. **Miniapp verification**
   - `npm run test`
   - `npm run typecheck`
   - `npm run build:mp-weixin`
4. **Server verification**
   - focused reproduction for any failing integration test
   - `mvn -B verify` under Java 17
   - confirm the Learning Engine JaCoCo gate remains satisfied
5. **Repository hygiene**
   - run all three pipelines and confirm generated output does not appear in `git status`

## Commit Strategy

Each repair is committed independently. A likely sequence is:

1. `docs: define service stability verification`
2. `build: make frontend verification reproducible`
3. One scoped `test:` / `fix:` pair for every genuine regression discovered

No unrelated refactoring or unfinished roadmap feature is included.

## Completion Criteria

- Server verify passes under Java 17 with PostgreSQL Testcontainers.
- Admin check passes under Node 22 from a lockfile-backed install.
- Miniapp check passes under Node 22 from a lockfile-backed install.
- The critical integration coverage already present in the server suite remains green.
- A full verification run leaves no generated files in Git status.
- Every production-code repair has a test that was observed failing for the intended reason before the repair.
