# `libs`: Java libraries loaded by the projects

Jar files (and a `classes/` directory) placed here are loaded by every project, in front of the
libraries shipped with Convertigo. A single project can also carry its own `libs/` directory.

Typical use: a JDBC driver for **SQL connectors**, or any library used by the code of a project.

This directory is **not** seen by the server itself. For a library the server loads (the JDBC
driver of the database cache or of the analytics database, the SAP Java Connector `sapjco3.jar`
with its native library), see the `lib/` directory next to this one when running the Docker
image, or replace the placeholder jar directly in the web application `WEB-INF/lib/` otherwise
(`plugins/com.twinsoft.convertigo.studio_*/lib/` in the Studio).

Restart Convertigo after adding or updating a library.
