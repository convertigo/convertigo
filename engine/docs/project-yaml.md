# Project YAML formats

A project keeps the format it was opened with. New projects, including copies created
from downloaded legacy templates by either Studio, use standard YAML. Importing,
opening or building an existing project does not convert it.

## Identification and compatibility

Legacy project headers start with `↑convertigo:` and retain their original reader,
writer and minimum-version calculation. Standard files start with
`convertigo: 8.5.0.m006` (or a later required version), without the leading arrow.
A standard YAML editor may prepend `---` or comments, quote keys or use flow style.
The root `convertigo` field remains mandatory; a malformed standard file never falls
back to the legacy parser. Source fragments inherit the root format and have no header.

The standard format requires engine 8.5.0 or later. Its `convertigo` field is the
maximum of 8.5.0 and the minimum required by the beans, including the migration
suffix. It is not the version of the engine that last saved the project.

## Mapping

The legacy tree and key order are retained. Only scalar syntax, file references
and invalid XML indentation change. For example:

```yaml
convertigo: 8.5.0.m006
↓Example [core.Project]:
  comment: A new project
  ↓Hello [sequences.GenericSequence]: sequences/Hello.yaml
```

`_c8oProject/sequences/Hello.yaml` contains the bean's properties and children:

```yaml
comment: |-
  First line
  Second line
↓Script [steps.SimpleStep-123]:
  expression: |-
    const value = 'hello';
    log.message(value);
↓name [variables.RequestableVariable-124]:
  value: ''
```

- `↓Name [type-priority]` stores the existing bean identity (`yaml_key`). Plain
  keys store properties. An empty bean or property is `''`; an empty source is `{}`.
- `↑name` stores an XML attribute, `→` a CDATA node and `→→` a text node. A simple
  property with a single text node uses its scalar directly. A property containing
  only CDATA uses `{→: value}`. Other nested XML uses ordered sequences of single-key
  mappings, allowing repeated element names and mixed content without duplicate keys:

  ```yaml
  value:
    - xmlizable:
      - ↑classname: java.lang.String
      - entry:
        - ↑name: first
        - →→: text
      - entry: second
  ```

- Extensions for uncommon DOM nodes are `→#` (XML comment), `→?target` (processing
  instruction) and `↑` (element namespace URI). A namespaced attribute uses
  `↑name: {↑: URI, →→: value}`. YAML presentation comments are not project data.
- A nonempty scalar on a bean key is a file reference relative to `_c8oProject`:
  `↓Hello [sequences.GenericSequence]: sequences/Hello.yaml`. It must be a local
  `.yaml` path with no control characters; it cannot escape that directory, including
  through symbolic links. Cycles fail. A bean otherwise contains a property mapping
  or the explicit empty scalar `''` (an empty mapping is also accepted). A scalar on
  an ordinary property remains text, even when it ends in `.yaml`.
  Metadata-only reads validate paths but deliberately do not open references.
- Duplicate mapping keys are rejected. Repeated XML names belong in separate list
  items. Aliases, custom tags, multiple documents and invalid node shapes are rejected.
  Limits are 32 Mi code points per source and 256 nesting levels.

Scalar lexemes become DOM strings. The writer leaves canonical booleans and integers
plain, but quotes ambiguous strings (including `yes`, dates, `null`, leading-zero
numbers and floating-point spellings) so ordinary Engine/Jackson rewrites preserve
values. Prefer quoting any manually authored value whose spelling matters. An empty
value is explicit; `null` is text, not a missing DOM node. The reader does not silently
convert numeric spellings or boolean case.

Every multiline value that contains only printable characters, tabs and LF uses
an indented literal block: one physical source line per value line. `|-` strips the
final LF, `|` preserves one, and `|+` preserves multiple final LFs (also a value made
only of LFs). An explicit indentation indicator (`|2`, `|2-`, `|2+`) protects leading
spaces or empty lines. Unlike legacy blocks, their contents have no extra pair of
single quotes. Tabs and trailing spaces are **data**, preserved literally inside
blocks. Do not configure an editor to trim them automatically.

CR/CRLF, NEL, U+2028/U+2029 and other YAML-incompatible control characters require
double-quoted escapes, even in a multiline value. These are the exceptions to block
presentation. Ordinary TypeScript, SCSS, HTML and multiline JSON stay in blocks.
Because Engine's emitter refuses some valid whitespace in literal blocks, the writer
emits unique placeholders and replaces the scalar ranges identified by syntax events
with correctly indented/chomped blocks. No value text is interpreted as YAML syntax.

XML list indentation is kept close to legacy, but a CDATA mapping under a list item
needs the extra YAML indentation that the legacy reader did not require. Files without
a final literal line break keep the legacy convention of no final newline.

The event reader uses SnakeYAML 2.6 without its object constructors or YAML 1.1 type
resolver; scalar validation uses the YAML 1.2 JSON schema. The writer uses SnakeYAML
Engine 3.2. Both libraries are Apache-2.0 licensed. Engine 3.2's reader was not retained:
it rejects the standard `\L` and `\P` escapes emitted by Jackson YAML. This combination
is tested with rewrites by both Engine and Jackson; it does not claim a complete
YAML 1.2 parser (the underlying syntax reader retains YAML 1.1 line-break rules).

## Explicit conversion

Both Studios offer **Convert to standard YAML…** in a legacy project's context
menu. The confirmation explains that current changes are saved, the minimum
engine version becomes 8.5, and older engines can no longer open the project.
The web Studio also shows the format in the project label's tooltip. After
conversion, the web action disappears and the Eclipse action is disabled.

An authenticated administrator with `WEB_ADMIN` or `PROJECTS_CONFIG` may POST
`projectName=<name>` to the existing administration service endpoint
`services/projects.ConvertYaml`. It saves the loaded project and its current edits
in standard YAML and reports the new minimum version. GET is rejected. No downgrade
service is provided. Java callers use
`Engine.theApp.databaseObjectsManager.convertProjectToStandardYaml(project)`: as a
save, it then refreshes the engine caches of the project and asks the other instances
of a shared workspace to reload it. `CarUtils.convertToStandardYaml(project)` only
writes the files.

Conversion uses the loaded beans and the regular shrink calculation, not a direct
replacement of the old header: default values must retain their meaning. A failure before committing the sources restores the in-memory format; a failure
while subsequently saving sidecars retains the committed standard format. The
standard writer renders and checks file paths before replacement, compares UTF-8
bytes with existing sources, and only replaces changed files. Unchanged sources retain
their modification dates. It restores earlier files on an I/O failure. No timestamp-only
shortcut is used: external edits are detected even if their dates are restored. A multi-file project save is not a filesystem-wide transaction in
the event of process termination. Keep project sources under version control.

The project DOM carries format metadata through shrink, unshrink and cloning, so
Git diff/merge and NGX conversion also retain it. Search snippets still use the
legacy display notation; they are not standalone project documents.

## Templates and CLI

Template URLs are declared in `convertigo-studio-web/src/lib/studio/projectTemplates.js`
and Eclipse wizard definitions. The archives live in external `c8oprj-template-*`
repositories, not in this repository. `studio.project.Create` and
`NewProjectWizard.createFromBlankProject` select standard YAML when saving their new
local copy. Existing downloaded template releases remain usable. Creating a Java
`Project` also defaults to standard YAML. The CLI loads existing projects and retains
their format; it has no separate template-creation command.

Run `:engine:standardYamlTest` for the focused suite and `:engine:test :engine:check`
for all engine checks. The regression corpus includes every project descriptor in
the repository outside generated directories, exact legacy byte compatibility,
DOM node/scalar preservation, included files, malformed inputs, conversion and
creation from a legacy template.
