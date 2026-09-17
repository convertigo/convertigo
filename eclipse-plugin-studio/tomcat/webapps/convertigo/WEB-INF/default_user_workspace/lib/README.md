# `lib`: Java libraries added to the server (Docker image only)

When Convertigo runs in its official container image, the content of this directory is copied
into the web application `WEB-INF/lib/` at each start of the container, and native libraries
found here are made available to the JVM. Use it for a library the **server** must load:

- the official JDBC driver used by the **database cache** or the **analytics database**
  (`ojdbc.jar`, `mysql-connector.jar`, `db2jcc.jar` replace the placeholders of the same name);
- the SAP Java Connector: `sapjco3.jar` (keep this exact name) and its native library
  `libsapjco3.so` from the same SAP download.

Restart the container after adding or updating a file. Removing a file here does not remove it
from a container already started: recreate the container.

Outside the Docker image this directory has no effect: replace the placeholder jar directly in
`WEB-INF/lib/` of the web application (`plugins/com.twinsoft.convertigo.studio_*/lib/` in the
Studio). For a library used by the projects only (SQL connectors), see the `libs/` directory.
