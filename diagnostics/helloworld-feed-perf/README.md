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

- Public GitHub search under `convertigo` found no `sample_HelloWorld_flow`
  repository.
- Public beta admin project export and database object services require
  authentication.
- The public project XSD is accessible, but only describes the JSON schema and
  does not expose Flow internals.

So the beta test isolates the difference to server-side Flow execution time,
but does not yet split the exact beta Flow requestable into `http.get`, XML
parsing, `list.map`, serialization, and requestable envelope phases. Doing that
requires the exact `sample_HelloWorld_flow` project source or authenticated
access to inspect and instrument it.

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
