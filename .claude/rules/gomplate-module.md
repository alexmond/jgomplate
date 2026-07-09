# gomplate module rules

jgomplate is the gomplate-specific layer on top of the gotmpl4j engine. Keep the boundary
clean:

- **Consume gotmpl4j from Maven Central** (`gotmpl4j-core`, `gotmpl4j-sprig`). Never fork,
  vendor, or copy engine sources into this repo. Engine bugs/features are fixed upstream in
  https://github.com/alexmond/gotmpl4j, not worked around here.
- **Functions register via ServiceLoader.** `GomplateFunctionProvider` (priority 200,
  above Sprig's 100) is listed in
  `META-INF/services/org.alexmond.gotmpl4j.FunctionProvider`.
- **Namespaces are POJOs with PascalCase methods** matching gomplate's Go API, in the
  `functions.ns` package (the naming-convention suppression is scoped to that package).
- **Variadic namespace methods do not work yet** (gotmpl4j matches methods by exact param
  count; no varargs unpacking). Prefer non-variadic signatures, or a native variadic
  function, until upstream varargs-method support lands.
- **No secrets or lab-internal details** in committed sources (this repo is public).
