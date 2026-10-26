# Contributing to example-billing

Thank you for your interest in contributing to **example-billing**, a Java 21 /
Spring Boot 4.1.1 modular monolith for billing, package plans and payment
integration. This guide explains how to contribute and the conventions your
pull request has to follow.

By participating in this project you agree to follow our
[Code of Conduct](CODE_OF_CONDUCT.md).

---

## 1. Contribution policy: fork first, no direct branches

**Every contribution must come from your own fork of this repository.**

- Do **not** create branches in `awinishere/example-billing`. Direct branches
  and direct pushes to the upstream repository are not accepted and may be
  deleted without notice.
- Only repository maintainers may push to the base branches (`master`) or merge
  a pull request.
- A pull request whose head branch lives in the upstream repository will be
  closed and the author asked to resubmit it from a fork.

The only supported flow is: fork the repository → create a branch **in your
fork** → open a pull request from `<your-github-username>:<branch>` to
`awinishere:master`.

Maintainers enforce this on GitHub with branch protection on `master`: include
administrators, require a pull request before merging, require linear history,
and grant no write access to outside contributors so that their only route in
is a fork.

---

## 2. Requirements

| Tool   | Version / notes                                        |
| ------ | ------------------------------------------------------ |
| JDK    | 21 (LTS)                                               |
| Build  | Maven wrapper (`./mvnw`) - no local Maven required     |
| Git    | 2.30 or newer                                          |
| Docker | required for `docker-compose.yaml` (local PostgreSQL)  |
| IDE    | IntelliJ IDEA recommended (`.editorconfig` is honored) |

---

## 3. Setting up your workspace

```sh
git clone https://github.com/<your-username>/example-billing.git
cd example-billing
git remote add upstream https://github.com/awinishere/example-billing.git
git fetch upstream
cp .env.example .env
docker compose up -d
make build
make test
```

`.env` is git-ignored and never committed. Fill it with your local values; the
keys are `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`,
`DATABASE_NAME`, `AWS_REGION`, `AWS_ENDPOINT_URL_S3`, `AWS_ACCESS_KEY_ID` and
`AWS_SECRET_ACCESS_KEY`.

Start a single service:

```sh
make run NAME=wonderwrite-authentication
```

---

## 4. Keeping your fork in sync

Never branch from a stale `master`; it produces conflicts and noisy diffs.

```sh
git fetch upstream
git checkout -b feat-plan-catalogue upstream/master
```

Before opening the pull request, rebase your work on the latest base branch:

```sh
git fetch upstream
git rebase upstream/master
```

---

## 5. Branch naming

Create one branch per logical change, named `<type>-<short-description>` in
kebab-case, ASCII only:

| Type    | Use for                                  |
| ------- | ---------------------------------------- |
| `feat`  | new feature or endpoint                  |
| `fix`   | bug repair                               |
| `refac` | refactor without behavior change         |
| `docs`  | documentation only                       |
| `build` | pom.xml, Maven or Docker changes         |
| `test`  | tests only                               |

Examples: `feat-otp-verification`, `fix-plan-update-immutable-fields`,
`docs-contributing-fork-policy`.

---

## 6. Repository layout

```
wonderwrite-common/         shared library: crypto (Argon2) and emails (Thymeleaf + JavaMail)
wonderwrite-authentication/ credentials, OTP, register/login/reset
wonderwrite-plans/          plan catalogue and pricing
wonderwrite-profile/        user profile
wonderwrite-billing/        invoices and payment gateway
docker/Dockerfile           placeholder, still empty
scripts/new-module.sh       module generator (used by `make module`)
```

Notes for library modules such as `wonderwrite-common`:

- They are plain JARs. Never add `spring-boot-maven-plugin` to a library module,
  otherwise the build fails with `Unable to find a single main class`.
- Do not add Spring Boot plugins or third-party starters that pull a different
  Spring Boot major version.

### Generating a new module

```sh
make module NAME=wonderwrite-invoicing
```

The generator creates the module folder, the `pom.xml`, the application class, a
`@SpringBootTest` skeleton, `application.yml`, and registers the module in the
root `<modules>` block. Reload the Maven project in IntelliJ afterwards
(Maven tool window → Reload All Maven Projects).

Known generator gap: `scripts/new-module.sh` still writes `com.example:root` as
parent and a `com.example:shared-kernel` dependency, while this reactor uses
`applications:root` and the library module `wonderwrite-common`. After
generating a module, fix those two entries in the new `pom.xml` (and in the
script when you touch it) before committing.

---

## 7. Code conventions

These are hard requirements; a pull request that breaks them will be asked to
change before review.

- **Language**: all identifiers, messages, logs, commit messages, documentation
  and generated templates are written in English.
- **No comments** unless a reviewer explicitly asks for one.
- **Layering**: layered architecture, no CQRS, no event bus. Services are
  organized as feature folders: `features/<feature-name>/<Name>Controller`,
  `<Name>Request`, `<Name>Command`, `<Name>Service`, `<Name>Result`,
  `<Name>Response`.
- **Domain first**: pure records in `domain/model` (zero framework imports);
  contracts (ports) in `domain/port`; JPA entities and adapters in
  `persistence/` with implementations suffixed `Adapter`; shared enums and
  value types in `extensions/types`.
- **Commands and results**: `Request` maps to a `Command` record; the service
  returns a `Result` record; `Response` is the HTTP representation. Reuse
  domain records for reads; add a composite record only when several aggregates
  are joined.
- **Validation**: invariants live in record compact constructors and throw
  `IllegalArgumentException`. Do not create an exception class per failure:
  module-specific exceptions belong in `extensions/exception` and are reserved
  for what built-ins cannot express.
- **Folders**: prefer singular names, especially for the layered roles
  (`domain/model`, `domain/port`, `persistence/entity`, `persistence/adapter`,
  `persistence/mapper`). Keep the folder set of a feature free of empty
  directories: delete a folder that ends up holding no class.
- **Versions**: dependency versions are always inherited from the root pom
  Spring Boot BOM. Never hard-code a version that the BOM already manages.
- **Audit fields**: `LocalDateTime`, not `Instant`.
- **Formatting**: 4 spaces for Java, 2 spaces for YAML and JSON, LF line
  endings, max line length 120, no trailing whitespace. XML files keep the
  indentation already used in the pom you edit.

---

## 8. Database changes

- Schema evolution goes through Flyway migrations in
  `src/main/resources/migrations`, named `V<n>__<description>.sql`.
- `spring.jpa.hibernate.ddl-auto` stays at `validate`; never switch it to
  `update` or `create-drop`.
- Comments inside SQL migrations are in English.

---

## 9. Tests

- Every feature pull request ships with tests. Run the whole reactor or a single
  module:

```sh
make test
./mvnw -pl wonderwrite-common -am test
```

- Unit tests use plain constructors and in-memory fakes; container-based tests
  use Testcontainers 2.x artifact names (`testcontainers-postgresql`, not
  `postgresql`) with a shared, statically configured container so the Spring
  context cache does not reuse a dead connection.
- A pull request is not mergeable while `make build` or `make test` fails.

---

## 10. Commit messages

Follow Conventional Commits, imperative mood, no trailing period:

```
feat: add Argon2 password hashing contract
fix: keep plan code immutable on update
docs: describe the fork based contribution flow
```

Keep each commit small and self-contained. Never commit `.env`, credentials,
generated keys, or anything under `target/`.

---

## 11. Opening a pull request

1. Push the branch to **your fork**.
2. Open the PR against `awinishere/example-billing` `master`.
3. Use the title format `<type>: <short summary>` (max ~70 characters).
4. In the description cover: what changes, why it changes, how to verify it, and
   the issue reference when there is one.
5. Mark breaking API or schema changes explicitly and update `CHANGELOG.md`.
6. Prefer `ready for review` over `draft` once `make build` and `make test` are
   green.

### Pull request checklist

- [ ] Branch was created from a fork, not from an upstream branch
- [ ] `make build` succeeds
- [ ] `make test` succeeds
- [ ] New behavior is covered by tests
- [ ] No comments, no Indonesian text, English-only artifacts
- [ ] No `target/`, `.env` or IDE files in the diff
- [ ] Migrations are additive and reversible in meaning
- [ ] `CHANGELOG.md` updated for user visible changes

---

## 12. Review process

- A maintainer reviews within a few days; feel free to ping after a week of
  silence.
- Address feedback in new commits and keep the history readable; the branch is
  squashed on merge.
- Review approval plus green build is required before merge.
- Do not merge your own pull request.

---

## 13. Reporting vulnerabilities

Do not open a public issue for security problems. Follow
[SECURITY.md](SECURITY.md).

---

## 14. License of your contribution

This project is licensed under the MIT License. By opening a pull request you
declare that you wrote the change, that you are allowed to submit it, and that
you contribute it under the same MIT license.
