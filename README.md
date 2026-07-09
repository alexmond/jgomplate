# jgomplate

A pure-Java implementation of [gomplate](https://gomplate.ca) — the popular Go
`text/template` renderer — built on top of
[gotmpl4j](https://github.com/alexmond/gotmpl4j) (the Go template engine + Sprig
functions for the JVM).

jgomplate lets a JVM application render gomplate-flavoured templates without shelling out
to the Go `gomplate` binary. It is to gomplate what
[jhelm](https://github.com/alexmond/jhelm) is to Helm: the host-specific layer
(gomplate's namespaced function set, datasources, and CLI) on top of the shared gotmpl4j
engine.

> **Status:** seed / early scaffold. A working subset of the `strings`, `conv`, and
> `coll` namespaces is wired end-to-end (engine + CLI); the full ~200-function surface and
> the remote datasources are tracked follow-up work.

## Modules

```
jgomplate-parent (pom)
├── jgomplate-functions  — gomplate's namespaced functions (ServiceLoader, priority 200)
│                          on top of gotmpl4j-core + Sprig
├── jgomplate-core       — the rendering engine (GomplateEngine) + datasource layer
├── jgomplate-cli        — the `jgomplate` command-line renderer (Spring Boot + Picocli)
└── jgomplate-sample     — a minimal embedding example
```

`jgomplate-functions` and `jgomplate-core` are libraries published to Maven Central; the
CLI and sample are applications and are not published.

## Quick start (embedding)

```java
GomplateEngine engine = new GomplateEngine();
String out = engine.render("Hello, {{ strings.ToUpper .name }}!", Map.of("name", "world"));
// -> "Hello, WORLD!"
```

The Go builtins, Sprig (priority 100), and the gomplate namespaces (priority 200) are all
discovered automatically via `ServiceLoader` when the modules are on the classpath.

## Quick start (CLI)

```bash
java -jar jgomplate-cli/target/jgomplate.jar --in '{{ strings.ToUpper "hello" }}'
# -> HELLO

java -jar jgomplate-cli/target/jgomplate.jar --file template.tmpl --out rendered.txt
```

## Build & test

```bash
./mvnw clean install                 # build + test + spring-javaformat + PMD + checkstyle
./mvnw spring-javaformat:apply       # auto-format (tabs)
```

JDK 21. Build/format/lint gates match the sibling alexmond libraries.

## How namespacing works

gomplate calls functions namespaced — `{{ strings.ToUpper "x" }}`. Each namespace is
registered as a nullary function returning a stateless namespace object; the template then
invokes its methods through gotmpl4j's Go-template method-call support. Namespace method
names deliberately mirror gomplate's Go API (PascalCase) so templates written for gomplate
render unchanged.

## License

Apache License 2.0.
