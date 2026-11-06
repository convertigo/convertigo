# JDBC connection compatibility

Convertigo keeps MariaDB connections unencrypted by default for compatibility with Connector/J 3.x, including when using Connector/J 4. An effective `jdbc:mariadb:` URL without an explicit TLS choice receives `sslMode=disable`; the URL saved in the project is not rewritten. This also applies to `jdbc:mysql:` URLs handled by the MariaDB driver, the SQL cache and MariaDB analytics persistence.

Enable authenticated TLS explicitly with `sslMode=verify-full` in the JDBC URL, and configure certificate trust as needed. Convertigo preserves an existing `sslMode` key regardless of its case or value. It also skips the compatibility default when the URL contains the legacy `useSsl`/`useSSL`, `trustServerCertificate` or `disableSslHostnameVerification` options, certificate/store options (`serverSslCert`, `trustStore`, `keyStore`), or a per-host `sslMode`. These checks are case-insensitive; option validation and interpretation remain the driver's responsibility. Certificate/store settings are preserved to avoid silently disabling an explicit TLS configuration. The existing MySQL-to-MariaDB URL scheme conversion still applies when explicit options are present.

Connection-string overrides also preserve driver options, including TLS settings; only their `user` and `password` parameters are removed from the effective URL and passed separately as credentials.

The existing PostgreSQL SQL-connector default remains `stringtype=unspecified` when that parameter is absent. Driver compatibility defaults are applied centrally by `JdbcConnectionManager.prepareJdbcUrl`.
