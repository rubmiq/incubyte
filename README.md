# Claude Code salary assessment starter

This is a prompt and instruction kit, not the implemented application.

1. Copy CLAUDE.md, AGENTS.md, frontend/AGENTS.md and backend/AGENTS.md into your repository, preserving paths. Merge existing instructions rather than overwriting them.
2. Open Claude Code in that repository.
3. Paste CLAUDE_CODE_PROMPT.md as your initial task, or ask Claude Code to read it and execute it.
4. Review and commit the generated one-page requirements before allowing application scaffolding. The prompt requests this sequencing explicitly.
5. Review real incremental diffs, domain rules and test results throughout development. Provide hosting credentials only through secure environment configuration when deployment is ready.

CLAUDE.md explicitly imports the AGENTS.md guides so Claude Code is instructed to read them. Other agent tools can also use the scoped AGENTS.md files.

Stack: modern Angular, Java/Spring Boot, PostgreSQL. The brief says “AngularJS for Java”; confirm whether they mean modern Angular. The kit records that assumption rather than implying the wording is unambiguous.

The implementation prompt includes product framing, realistic exclusions, cross-currency rules, auditability, concurrency, deterministic seeding, meaningful tests, deployment and video requirements. It requires honest reporting of any missing deployment or recording access.
