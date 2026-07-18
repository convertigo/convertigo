# HelloWorld Flow Run13 fresh-agent prompt

Create a complete Convertigo Flow backend and Svelte frontend in the target
project `sample_HelloWorldFlowRun13`.

The application must read the NASA Image of the Day RSS feed from
`https://www.nasa.gov/feeds/iotd-feed/`. Keep this service URL in project-level
configuration. The backend must return all current feed entries as `news`, with
`title`, `description`, `imageUrl`, `link`, and `pubDate`, plus a matching
`count`.

The frontend must be responsive and dark, show a clear application title and a
button that loads the backend result, then render every news item with its
image, title, and description. Alternate two visibly different card treatments
across the list.

Work autonomously until the backend and built application are validated. Use
only the installed Convertigo Flow skill, the `convertigo-flow` MCP server, and
Playwright for application validation. If the project does not exist, use the
supported Flow project bootstrap with Svelte UI enabled. The externally
reachable browser origin is `http://127.0.0.1:19080`; preserve the application
path returned by the frontend tools when opening the built application.

This is a fresh-context authoring benchmark:

- Do not inspect, list, read, copy, or call any other application project,
  including previous HelloWorld projects.
- Do not inspect local repositories, session history, CIR memory, runtime
  workspace files, generated files, or Convertigo DBO YAML.
- Do not use shell or legacy Convertigo MCP as an authoring fallback.
- Do not modify `lib_flow_engine`, `lib_flow_mcp`, the Svelte frontbuilder, the
  Convertigo Java engine, or their runtime files. Report a tooling gap instead.
- Do not hard-code feed data or replace the external NASA source with a fixture.
- Keep any unavoidable project-local primitive small and generic. Finish with
  no mock blocks.

Validate the backend result and schema, frontend bindings, generated production
build, and the real application in Playwright. In the final report, state the
first successful backend milestone, the first useful browser rendering, the
final validation, remaining tooling gaps, and whether schema-backed structured
bindings were used without manual string-path workarounds.
