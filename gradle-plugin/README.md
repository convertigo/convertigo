# Project bundles

The `projectBundle` task builds a ZIP containing project CAR files and a
`bundle.json` manifest. It uses the same engine exporter as the administration
console. The existing `car`, `deploy` and `exportDependencies` tasks keep their
behavior.

```groovy
projectBundle {
    // Defaults to the current project when this list is empty.
    projects = ['Application', 'OtherApplication']
    includeDependencies = true
    destinationFile = file('build/applications-bundle.zip')

    // The same archive options as the car task are available.
    includeTestCases = false
    includeStubs = true
}
```

Run `./gradlew projectBundle`. The task saves the current project first, as the
`car` task does. Selected projects and included references must be available to
the engine used by the build. Dependency selection follows declared project
references; unrelated loaded projects are omitted. Without
`includeDependencies`, only the selected projects are exported. Missing
references must already be available on the destination server.

The manifest lists project names, versions, archive names, declared references
and dependency order. Project names are unique across platforms. Cycles and
conflicting selections are refused before the destination bundle is replaced.
Archive creation is serialized because preparing an archive without test cases
uses temporary files within the source project. Identical sources and options
produce the same projects and manifest; ZIP byte identity is not guaranteed.
