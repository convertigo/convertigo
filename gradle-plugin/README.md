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

## Bundle import behavior

The administration console has a separate multiple-project mode. A request can
contain several CAR or project ZIP files, a distribution ZIP of project archives,
or a ZIP of project folders. Only one outer bundle level is expanded: ZIP and
JAR files inside a project remain project assets. `bundle.json` is optional;
project descriptors determine names, versions and references. Differences from
the manifest are reported as warnings.

The complete selection is validated in a fresh staging directory before any
project is replaced. Duplicate project names, incompatible portable paths and
dependency cycles are refused. The limits for one request are 64 uploaded files,
1 GiB of uploaded bytes, 200 projects, 200,000 entries, 4 GiB of expanded bytes
(counting outer and inner archives together), and 1 GiB per entry. An entry
larger than 1 MiB must not exceed a 200:1 expansion ratio. ZIP directory metadata
is limited to 64 MiB, project descriptors to 16 MiB and the manifest to 1 MiB.
Private project data is omitted. Known operating-system metadata is ignored and
reported.

Projects are deployed sequentially in dependency order, using the normal
backup/replacement operation. Each project's normal automatic starts and
synchronizations still occur during that project's deployment. A failure stops
the remaining projects and the result lists deployed, failed and unattempted
projects, including migration-save warnings. Already deployed projects are not
rolled back. Normal deployment backups are preserved. There is no batch-wide
visibility barrier between instances sharing a workspace.

The existing single-project service and the Eclipse Studio deployment remain
available with their existing contracts.
