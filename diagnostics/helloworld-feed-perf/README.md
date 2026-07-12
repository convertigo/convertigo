# sample_HelloWorld feed performance diagnosis

Date: 2026-07-12

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
