# pre-commit-hooks reference

Full list of hooks defined in this repo's `../../../../.pre-commit-config.yaml`, and how to
respond when one fails.

## Full hook list

| Hook | What it does | Kind |
| --- | --- | --- |
| `trailing-whitespace` | Strips trailing whitespace | Auto-fix |
| `end-of-file-fixer` | Ensures exactly one trailing newline | Auto-fix |
| `check-merge-conflict` | Detects leftover merge markers (`<<<<<<<`, `=======`) | Blocking |
| `check-added-large-files` | Blocks new files over 5MB (`--maxkb=5000`) | Blocking |
| `conventional-pre-commit` | Enforces Conventional Commits format (`commit-msg` stage) | Blocking |
| `gitleaks` | Scans for hardcoded API keys, tokens, passwords (misses short/low-entropy values like `password: 1234`) | Blocking |
| `spotless-apply` | Formats Java code to Google Java Format (`./gradlew spotlessApply`) | Auto-fix |
| `checkstyle` | Java lint check incl. missing Javadoc (`./gradlew checkstyleMain checkstyleTest`) | Blocking |

## Auto-fix vs blocking

- **Auto-fix** (`trailing-whitespace`, `end-of-file-fixer`, `spotless-apply`): the
  hook rewrites the file itself and the first commit attempt fails with the file
  now changed. Check what changed with `git status`/`git diff`, `git add` to
  restage, and recommit as-is — it passes.
- **Blocking** (`check-merge-conflict`, `check-added-large-files`, `gitleaks`,
  `checkstyle`, `conventional-pre-commit`): nothing gets fixed automatically, so
  you have to fix the underlying cause yourself (merge markers, a large file, a
  hardcoded key, a lint violation, message format). For `conventional-pre-commit`
  the rejection output names the problem plainly.

## Validating before you commit

There's no non-interactive "dry run" flag on the hooks — actually attempting a
commit is the real check.

If there are Java changes, Spotless/Checkstyle run against the whole build at
commit time and can be slow, so running the following ahead of time avoids any
rework at commit time (optional, not required):

```
./gradlew spotlessApply checkstyleMain checkstyleTest --quiet --build-cache
```

Unlike Spotless, Checkstyle doesn't auto-fix — if there are violations, read the
report and fix them by hand. The HTML/XML reports are under
`build/reports/checkstyle/`. The most common violation is missing or malformed
Javadoc (see section 4 of `../SKILL.md`).

If the build itself fails to evaluate (e.g. `Could not find method spotless()`),
the hook isn't reporting a code problem — a plugin in `build.gradle` is missing or
misconfigured. Fix the build script rather than the Java code.

If you're unsure the commit message itself is compliant, manually check it
against the type list and format before running `git commit`.
