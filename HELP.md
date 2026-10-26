# Help

Quick reference for building, running and debugging this repository. If you are
about to contribute code, read [CONTRIBUTING.md](CONTRIBUTING.md) first: work
always starts from a fork, never from a branch in the upstream repository.

## Useful project information

- Java 21, Spring Boot 4.1.1, Maven multi-module reactor with parent
  `applications:root`.
- Modules: `wonderwrite-common` (shared library), `wonderwrite-authentication`,
  `wonderwrite-plans`, `wonderwrite-profile`, `wonderwrite-billing`.
- Documentation while a service runs: Scalar UI at `http://localhost:8080/docs`,
  OpenAPI document at `/v3/api-docs`.

## Build commands

Maven is never installed globally; always use the wrapper.

```sh
./mvnw clean verify                 # build and test every module
./mvnw -DskipTests package          # packaging only
./mvnw -pl wonderwrite-authentication -am test    # one module plus its dependencies
./mvnw -pl wonderwrite-authentication -am dependency:tree
./mvnw -pl wonderwrite-authentication -am spring-boot:run
```

The `Makefile` wraps the same commands:

```sh
make help
make build
make test
make run NAME=wonderwrite-authentication
make tree NAME=wonderwrite-authentication
make clean
make module NAME=wonderwrite-invoicing
```

## Reference guides

- [Using the Maven wrapper](https://maven.apache.org/wrapper/)
- [Spring Boot documentation](https://docs.spring.io/spring-boot/index.html)
- [Spring Boot 4.1.1 reference - mail](https://docs.spring.io/spring-boot/reference/io/email.html)
- [Spring Boot 4.1.1 reference - Flyway](https://docs.spring.io/spring-boot/reference/data/sql.html)
- [Testcontainers](https://java.testcontainers.org/)
- [Java 21 language features (records, sealed types)](https://docs.oracle.com/en/java/javase/21/)

## What to do with the `wonderwrite-common` module

`wonderwrite-common` is a plain library JAR, so it has no application class and
must not declare `spring-boot-maven-plugin`. Consuming modules get its beans
because the application package `applications` is component scanned.

Its beans need configuration in the calling service:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}

app:
  mail:
    from: ${MAIL_FROM:no-reply@example.com}
```

`JavaMailSender` is only auto-configured when `spring.mail.host` is present;
without it a service that scans `applications` fails at startup. Email templates
are resolved by Thymeleaf from `src/main/resources/templates` of the running
module.

## Running locally

```sh
docker compose up -d          # PostgreSQL 5432, database/user/password: shop
cp .env.example .env          # then fill in the values
make run NAME=wonderwrite-authentication
```

Environment keys used by the modules: `DATABASE_URL`, `DATABASE_USERNAME`,
`DATABASE_PASSWORD`, `DATABASE_NAME`, `SERVER_PORT`, `AWS_REGION`,
`AWS_ENDPOINT_URL_S3`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`. The `.env`
file is ignored by Git - never commit it.

Persistence uses Flyway (`classpath:migrations`, schema `plans`) with
`spring.jpa.hibernate.ddl-auto=validate`, so a missing migration breaks startup
by design. Add a migration instead of relaxing validation.

## IDE notes (IntelliJ IDEA)

- After `make module`, use Maven tool window → *Reload All Maven Projects*.
- Set the project SDK to JDK 21 and the bytecode target to 21.
- Enable annotation processing for Lombok (Settings → Build, Execution,
  Deployment → Compiler → Annotation Processors) or install the Lombok plugin.
- Reformat with the repository `.editorconfig`: 4 spaces for Java, 2 for
  YAML/JSON/XML, LF endings, max line length 120.

## Troubleshooting

- `./mvnw: Permission denied` → `chmod +x mvnw`.
- `Unable to find a single main class` → a library module declares
  `spring-boot-maven-plugin`; remove it there.
- `Non-resolvable parent POM` for a module → its `<parent>` block must be
  `applications:root:1.0-SNAPSHOT`, matching the root pom.
- Testcontainers artifact not found → Spring Boot 4 / Testcontainers 2.x use
  prefixed artifact ids, e.g. `testcontainers-postgresql`.
- Windows checkouts breaking `./mvnw` through CRLF line endings → keep the
  `.gitattributes` rules (`mvnw text eol=lf`, `mvnw.cmd text eol=crlf`).

## Reporting problems

- Bugs and feature requests: open a GitHub issue, then submit the fix from your
  fork as a pull request against `master`.
- Security issues: read [SECURITY.md](SECURITY.md) and report privately instead
  of opening a public issue.
