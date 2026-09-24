# Execution ledger — 2026-09-24-knowledge-agent.md

## Rulings
- Work only on `feat/knowledge-agent-20260924`, as explicitly requested. Do not merge back into `feat/soft-exam-v1`; this supersedes the old plan's last sentence.
- Local container has no GitHub DNS access, Maven or Docker. Use the existing GitHub Actions Java 17/PostgreSQL Testcontainers suite for real validation; do not claim local execution.
- Task 1 RED was observed at commit `516f56a`, server-ci run 35995735629: the three newly added test classes fail compilation because their production classes do not exist. The untouched baseline had passed server-ci run 35995558514.

## Progress
- Task 1 implementation submitted for CI: Chinese lexical terms, page-preserving chunking, PDF/TXT/MD extraction, bounded private local original storage.
- Task 2 and Task 3 not started. No completion claim until their tests and builds are verified.
