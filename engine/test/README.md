# Session value serialization regression tests

Run the standalone suite (also included in `:engine:check`):

```sh
./gradlew :engine:sessionSerializationTest
```

No external service or test-framework dependency is required. The suite tests
the production codec and `RedisHttpSession.flush()` / a newly loaded session
against an in-memory `SessionStore`. It covers typed `Properties`, legacy JSON,
W3C DOM interfaces, namespaces, mixed content, XML signatures and XXE rejection.

`SessionSerializationTest --redis` runs the same session checks using
`RedisSessionStore`. Run this main class on the `sessionTest` runtime classpath
with `-Dconvertigo.engine.session.redis.host`,
`-Dconvertigo.engine.session.redis.port` and
`-Dconvertigo.engine.session.redis.prefix` pointing to an **isolated test Redis**.
It creates a uniquely named session and removes it afterwards. Never point this
test at production.

## Stored-value contract

- `Properties` values use the existing typed JSON envelope. Effective string
  defaults are flattened into the snapshot; the defaults chain is not retained.
- Ordinary untyped maps/lists and existing typed values remain readable. Old
  untyped JSON cannot reveal whether it originally came from `Properties`:
  recreate the session or write the attribute again after upgrading.
- DOM payloads store stable W3C interface names and XML, not parser-specific Java
  implementation classes. `Document` and `Element` take precedence over `Node`
  and `NodeList` when implementations expose multiple interfaces.
- `Node` covers attributes, text, CDATA, comments, processing instructions and
  document fragments. Node lists retain item order and each item's DOM type.
- Restored DOM values are detached snapshots, not references into the original
  tree. Node lists are not live queries; parent/document identity across list
  entries, user data, schema/ID annotations and parser implementation identity
  are not retained.
- XML is not pretty-printed. Namespace bindings (including inherited bindings
  of standalone elements) and significant whitespace are retained. The suite
  verifies an enveloped XML signature after a document round-trip.
- DTD/entity nodes and external entity resolution are deliberately rejected.
  Malformed/unsafe DOM values fail decoding rather than silently falling back
  to another Java type.
