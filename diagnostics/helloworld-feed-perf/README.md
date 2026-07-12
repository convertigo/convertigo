# sample_HelloWorld feed performance diagnosis

Date: 2026-07-12

## Public beta exact URL comparison

Compared URLs:

- Legacy:
  `https://beta.convertigo.net/convertigo/projects/sample_HelloWorld/.json?__sequence=GetFeed`
- Flow:
  `https://beta.convertigo.net/convertigo/projects/sample_HelloWorld_flow/.json?__sequence=GetFeed`

Benchmark run:

```bash
ITERATIONS=12 WARMUP=2 diagnostics/helloworld-feed-perf/bench_beta_urls.sh
```

Artifacts:

- Raw timings: `results/beta-benchmark-20260712T214008Z.csv`
- Summary: `results/beta-summary-20260712T214008Z.csv`

The script reuses one cookie jar, uses a dedicated `__context`, alternates call
order, records curl timing phases, and removes the context/session at the end.

All measured responses were valid and equivalent:

- 24 measured rows out of 24 returned HTTP 200.
- Every measured body was 7218 bytes.
- Every measured body had SHA-256
  `c2dd0fd76f9b1ab622c372d0b7639404a36baa41b9a4228edb4cc8d2d07eae57`.

Cold calls:

| Target | TTFB | Total |
| --- | ---: | ---: |
| Legacy | 553.0 ms | 553.1 ms |
| Flow | 1010.4 ms | 1010.7 ms |

Hot measured medians:

| Target | Connect | TLS | TTFB | Total | Server after TLS |
| --- | ---: | ---: | ---: | ---: | ---: |
| Legacy | 38.1 ms | 86.6 ms | 277.3 ms | 277.5 ms | 185.7 ms |
| Flow | 44.0 ms | 95.4 ms | 441.5 ms | 441.8 ms | 321.2 ms |

In this beta run, Flow is therefore 164.3 ms slower on median total time, or
1.59x the legacy time. Since `time_total` is effectively equal to
`time_starttransfer`, and the response body is identical, the difference is in
server-side time before the first byte rather than in client network transfer or
download. Subtracting TLS pretransfer time still leaves about 135.5 ms median
extra server-side time for Flow.

This confirms the reported direction on the exact public beta URLs, although
the delta in this run is a little smaller than the previous reported roughly
200 ms.

## Public beta source availability

The exact `sample_HelloWorld_flow` project source was not available from this
workspace or from public project discovery:

- Flow MCP reports `Unknown Convertigo project: sample_HelloWorld_flow` for both
  `flow-list` and `code-get sample_HelloWorld_flow.GetFeed`.
- Local Convertigo MCP `project-list` contains `sample_HelloWorld` and
  `sample_HelloWorldFlowRun4`, but not `sample_HelloWorld_flow`.
- Marketplace searches on 2026-07-13 for `sample_HelloWorld_flow` and
  `HelloWorld flow` returned no entries. Searches for `sample_HelloWorld` and
  `helloworld` returned only the legacy `sample_HelloWorld`
  (`c8oprj-sample-helloworld`). No exact Flow import was attempted because no
  exact marketplace entry exists.
- Public GitHub search under `convertigo` found no `sample_HelloWorld_flow`
  repository.
- Public beta admin project export and database object services require
  authentication.
- The public project XSD is accessible, but only describes the JSON schema and
  does not expose Flow internals.

Current source-availability evidence is recorded in
`results/source-availability-20260712T220609Z.json`.

So the beta test isolates the difference to server-side Flow execution time,
but does not yet split the exact beta Flow requestable into `http.get`, XML
parsing, `list.map`, serialization, and requestable envelope phases. Doing that
requires the exact `sample_HelloWorld_flow` project source or authenticated
access to inspect and instrument it.

## Local Flow primitive isolation

Because the beta Flow source is not available, the local
`sample_HelloWorldFlowRun4.ReadNasaFeed` Flow was used only as a controlled
primitive benchmark. The script `bench_flow_phases.py` writes temporary
FlowScript working copies through Flow MCP, runs them with `code-run`, and
always discards the working copy. A final `code-status` confirmed no dirty
working copy remained.

The benchmark serves the captured RSS fixture
`results/nasa-iotd-20260712T195054Z.rss` to the Convertigo container over the
Docker bridge, so the measured variants avoid external NASA network variance.

Benchmark run:

```bash
ITERATIONS=12 WARMUP=3 diagnostics/helloworld-feed-perf/bench_flow_phases.py
```

Artifacts:

- Variant timings: `results/flow-variant-20260712T220153Z.csv`
- Variant summary: `results/flow-variant-summary-20260712T220153Z.csv`
- Derived deltas: `results/flow-variant-derived-20260712T220153Z.json`
- Local HTTP requestable timings: `results/flow-http-20260712T220153Z.csv`
- Bridge summary: `results/flow-http-summary-20260712T220153Z.json`

FlowScript variant medians under `code-run`:

| Variant | Median |
| --- | ---: |
| Empty Flow | 193.3 ms |
| Local fixture `http.get` | 214.6 ms |
| `http.get` + `xml.parse` | 238.3 ms |
| `http.get` + `xml.parse` + `list.sort` | 266.4 ms |
| `http.get` + `xml.parse` + `list.sort` + `list.map` | 510.0 ms |
| Full `news` result | 466.5 ms |

Median deltas:

| Delta | Approx. cost |
| --- | ---: |
| Local fixture `http.get` over empty | 21.2 ms |
| XML parse over HTTP | 23.7 ms |
| Sort over XML | 28.2 ms |
| Map/expression projection over sort | 243.5 ms |

The `full_result` variant is faster than the count-only `list.map` variant in
this `code-run` path, so the difference between those two should not be used as
a serialization estimate. The MCP `code-run` response compacts large results
and has its own response shaping. The useful signal is the staged delta: in the
real Flow engine, with a local RSS fixture, raw HTTP and XML parsing are not the
dominant costs; the expensive step is the Flow expression/list projection path.

The same run also measured the saved local Flow requestable over HTTP:

| Metric | Value |
| --- | ---: |
| HTTP median TTFB | 163.2 ms |
| HTTP median total | 163.3 ms |
| Estimated `FlowEngineBridge.run` average from cache-info deltas | 173.1 ms |

This supports the beta observation that almost all elapsed time is before the
first byte and inside Flow server execution. On the local controlled Flow, the
largest isolated primitive cost is `list.map`/expression evaluation, not the
network fetch or XML parsing alone.

## Runtime

- Runtime container: `c8o-agent-runtime`
- Image: `convertigo/convertigo-ci:develop`
- Local base URL: `http://127.0.0.1:19080/convertigo`
- Legacy project imported for this run:
  `sample_HelloWorld=https://github.com/convertigo/c8oprj-sample-helloworld/releases/download/v8.3.0/sample_HelloWorld.car`
- Flow project already present in the runtime: `sample_HelloWorldFlowRun4`

## Important equivalence gap

The two requestables available in this runtime are not strictly equivalent:

- Legacy `sample_HelloWorld.GetFeed` calls `sample_HelloWorld.RSSConnector.GetFeed`,
  configured as `https://apod.com/feed.rss`.
- Flow `sample_HelloWorldFlowRun4.ReadNasaFeed` calls
  `https://www.nasa.gov/feeds/iotd-feed/`, parses XML, sorts items by title,
  and maps `title`, `description`, `imageUrl`, `link`, and `pubDate`.

So this run is useful to isolate layers, but not as an exact apples-to-apples
comparison of one identical feed pipeline.

## Results

Client-side medians from `results/feed-benchmark-20260712T195054Z.csv`:

| Target | Median |
| --- | ---: |
| Direct APOD HTTP | 246.0 ms |
| Direct NASA IOTD HTTP | 80.0 ms |
| Legacy `GetFeed` | 254.5 ms |
| Flow `ReadNasaFeed` | 171.1 ms |

Server-side medians from `results/server-summary-20260712T195054Z.csv`:

| Target | Metric | Median |
| --- | --- | ---: |
| Legacy transaction | Host | 218 ms |
| Legacy transaction | Convertigo | 20 ms |
| Legacy transaction | Total | 238 ms |
| Legacy sequence | Total | 251 ms |
| Flow sequence | Total | 172 ms |

XML pipeline microbench from `results/xml-pipeline-20260712T195054Z.csv`:

| Payload | Phase | Median |
| --- | --- | ---: |
| NASA IOTD RSS | JAXP parse | 1.226 ms |
| NASA IOTD RSS | DOM to object | 0.686 ms |
| NASA IOTD RSS | sort/map | 0.132 ms |
| NASA IOTD RSS | total | 2.018 ms |
| APOD RSS | total | 2.184 ms |

## Conclusion

On this runtime, the Flow requestable is faster than the legacy requestable
because it calls a much faster upstream feed. Legacy `GetFeed` is dominated by
the APOD HTTP call: server stats put 218 ms median in Host time out of a
238 ms median transaction total.

For Flow itself, direct NASA HTTP is about 80 ms median while the full Flow
requestable is about 172 ms median. The extra roughly 90 ms is not explained by
XML parsing, DOM conversion, or sort/map: the same XML pipeline is about 2 ms
in the Java microbench. The remaining cost is therefore in the Flow execution
envelope: requestable setup, Rhino/block dispatch, expression evaluation,
scope/result handling, and response serialization.

If the observed "legacy faster than Flow" result came from another Flow version
of `sample_HelloWorld.GetFeed`, that exact Flow project was not present here;
this diagnosis intentionally did not create or invent one.

## Reproduction

```bash
ITERATIONS=30 WARMUP=5 diagnostics/helloworld-feed-perf/bench_feed.sh

javac diagnostics/helloworld-feed-perf/XmlPipelineBench.java
java -cp diagnostics/helloworld-feed-perf XmlPipelineBench \
  diagnostics/helloworld-feed-perf/results/nasa-iotd-20260712T195054Z.rss \
  diagnostics/helloworld-feed-perf/results/apod-20260712T195054Z.rss
```
