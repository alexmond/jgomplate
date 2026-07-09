# CLAUDE.md — jgomplate

## What this is

A pure-Java implementation of [gomplate](https://gomplate.ca) (the Go `text/template`
renderer), built on [gotmpl4j](https://github.com/alexmond/gotmpl4j). jgomplate is the
gomplate-specific layer — namespaced functions, datasources, CLI — on top of the shared
gotmpl4j engine, the same way [jhelm](https://github.com/alexmond/jhelm) layers Helm on
gotmpl4j.

**Tech stack:** Java 21, Spring Boot (CLI only), Picocli, Jackson (JSON/YAML datasources),
Lombok, Maven. Published to Maven Central.

## Modules

```
jgomplate-parent (pom)
├── jgomplate-functions  — gomplate namespaced functions (ServiceLoader, priority 200)
├── jgomplate-core       — GomplateEngine + datasource layer
├── jgomplate-cli        — `jgomplate` CLI (Spring Boot + Picocli), not published
└── jgomplate-sample     — embedding example, not published
```

`jgomplate-functions`/`jgomplate-core` depend on the published `gotmpl4j-core` and
`gotmpl4j-sprig` artifacts — **never** vendor or fork the engine; consume it from Central.

## Build & test

```bash
./mvnw clean install               # build + test + spring-javaformat + PMD + checkstyle
./mvnw test                        # tests only
./mvnw spring-javaformat:apply     # auto-format (tabs) — run before committing
```

JDK 21. Build/format/lint gates are identical to the sibling alexmond libraries.

## Coding standards

- Tabs (enforced by spring-javaformat). Run `spring-javaformat:apply` before committing.
- Imports, never inline FQNs (PMD `UnnecessaryFullyQualifiedName`).
- `Locale.ROOT` on case conversions; `.append('c')` for single chars.
- Checkstyle's Spring extensions apply: lambda args take parens (`SpringLambda`), ternaries
  use the `(a != b) ? y : n` form (`SpringTernary`).
- File ≤800 lines, method ≤80 (checkstyle).

## The namespace pattern (important)

gomplate functions are **namespaced** — `{{ strings.ToUpper "x" }}`. This is implemented by
registering each namespace (`strings`, `conv`, `coll`, …) as a nullary function returning a
stateless POJO; the template invokes its methods via gotmpl4j's Go-template method-call
support. Consequences to remember when adding functions:

- **Method names mirror gomplate's Go API verbatim (PascalCase).** The camelCase naming
  rules are suppressed for the `functions.ns` package (checkstyle `MethodName`, PMD
  `MethodNamingConventions` via `@SuppressWarnings`). Keep new namespace classes in that
  package so the suppression applies.
- **Variadic namespace methods are not yet callable from templates.** gotmpl4j's executor
  matches methods by exact parameter count and does not unpack Java varargs, so a variadic
  method (e.g. `coll.Slice`) resolves to nothing. Until varargs-method support lands in
  gotmpl4j, build lists with Sprig's variadic `list` function. This is the main known gap.
- Namespace args arrive loosely typed (`Object`); coerce via `functions.Values`.

## Adding a gomplate function

1. Find or create the namespace class in `jgomplate-functions/.../functions/ns/`.
2. Add a `public` method named exactly as gomplate names it (PascalCase), taking `Object`
   params, coercing through `Values`.
3. If it is a **new** namespace, register it in `GomplateFunctionProvider#getFunctions`.
4. Add a rendering test in `GomplateNamespaceTest` (assert against the real engine output).

## Releasing

`.github/workflows/maven_release.yml` (manual dispatch) sets the version, builds, deploys
to Maven Central via the `release` profile (GPG sign + central-publishing-plugin), tags,
and opens a GitHub release. The CLI + sample are excluded from the Central bundle
(`excludeArtifacts`). Release versions are numeric-only (`0.2.0`, `1.0.0`).
