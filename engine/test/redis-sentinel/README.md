# Disposable Redis Sentinel runtime checks

This fixture starts a master, two replicas, three Sentinels and two Convertigo
instances. All credentials are **test-only**. Only the HTTP ports are published,
on loopback. Do not reuse it for production or point the probe at an existing
Redis server. The test uses an `ISSUE1233SESSION` cookie to avoid replacing the
`JSESSIONID` cookie of another local Studio/server in the same browser.

From the develop checkout (no Studio build):

```sh
./gradlew :engine:classes :engine:testClasses :engine:redisSentinelConfigurationTest
export C8O_ENGINE_ROOT="$PWD"
docker pull convertigo/convertigo-ci:develop
docker compose -p c8o-1233 -f engine/test/redis-sentinel/compose.yaml up -d
```

Wait for both engines to start and the Sentinels to discover the two replicas and
each other. Admins: <http://localhost:29133/convertigo/admin/> and
<http://localhost:29134/convertigo/admin/> (`admin` / `issue1233-test-only`).
Override `C8O_SENTINEL_PORT_A` / `C8O_SENTINEL_PORT_B` if those ports are occupied.

Authenticate on A and send the same cookie to B. `engine.ListInstances` should
report `RedisSessionProvider`, topology `sentinel`, three seeds, a current master
address and two live instances. No additional login should be needed on B.

Keep an opt-in standalone probe running inside A:

```sh
docker compose -p c8o-1233 -f engine/test/redis-sentinel/compose.yaml exec -T engine-a \
  sh -c 'java $JAVA_OPTS -cp "/test-classes:/usr/local/tomcat/webapps/convertigo/WEB-INF/classes:/usr/local/tomcat/webapps/convertigo/WEB-INF/lib/*" com.twinsoft.convertigo.engine.sessions.RedisSentinelRuntimeProbe --failover'
```

When it prints `READY_FOR_FAILOVER`, use another terminal to write a replication
barrier and wait for both replicas **on the same Redis connection**, then stop
only the test master:

```sh
printf 'SET issue1233:replication-barrier ok EX 60\nWAIT 2 5000\n' | \
  docker compose -p c8o-1233 -f engine/test/redis-sentinel/compose.yaml exec -T redis-master \
    redis-cli --no-auth-warning -a data-test-password
# Continue only if SET returns OK and WAIT returns 2.
docker compose -p c8o-1233 -f engine/test/redis-sentinel/compose.yaml stop redis-master
```

The existing clients must follow the promotion without restarting Convertigo.
The probe checks the production session/context stores, previously replicated
inflight data, read-after-write, cross-client lock exclusion and an existing
pub/sub subscription, before and after promotion. It prints `PASS` on success.
The administrative cookie must still work on A and B, and `engine.ListInstances`
must show the promoted master. This is not a test of an in-flight business
sequence, exactly-once execution or non-replicated write survival.

The barrier is a **test precondition**, not a production durability guarantee.
Redis replication is asynchronous and Sentinel can lose unreplicated acknowledged
writes, including locks and request markers. Retry-sensitive operations still
need application-level protection.

Clean up only this disposable stack, including its own test workspace volumes:

```sh
docker compose -p c8o-1233 -f engine/test/redis-sentinel/compose.yaml down --volumes
```

## Engine properties (8.4.7+)

Enable `session.store.mode=redis`. The default `session.redis.topology=single`
keeps using `session.redis.host` and `session.redis.port`. For Sentinel, set:

| Property | Meaning |
| --- | --- |
| `session.redis.topology` | `sentinel` |
| `session.redis.sentinel.master` | Master name declared in Sentinel |
| `session.redis.sentinel.nodes` | Comma/whitespace-separated seeds, `host:port` or `[IPv6]:port` |
| `session.redis.sentinel.username` | Optional Sentinel ACL username, independent of data nodes |
| `session.redis.sentinel.password` | Optional Sentinel password; empty uses the data password when Sentinel requires AUTH |

Use the `convertigo.engine.` JVM prefix, for example
`-Dconvertigo.engine.session.redis.topology=sentinel`. Never put credentials in
seed addresses. Redis data credentials, database, command timeout, pool limits
and prefixes retain their existing `session.redis.*` properties. Reads and
subscriptions use the master. Single-server host/port are ignored in Sentinel mode.

Existing `session.redis.ssl.*` properties apply to **all** Sentinel/data nodes.
Mount the truststore and, for mTLS, the client keystore in the engine container.
Every advertised address must be reachable and match its certificate when using
STRICT verification. Configure Sentinel-to-Redis TLS/authentication separately
on the Redis deployment. The Convertigo Helm chart exposes client settings under
`sessionStore.redis.sentinel.*`; Sentinel servers remain in a dedicated chart.

### Current client limitation

Redisson 4.8.0 does not send Sentinel AUTH when the Redis data password is absent,
even when a separate Sentinel password is configured. This combination is
explicitly rejected with an actionable error rather than ignoring credentials.
Use password-protected data nodes (recommended for production), with the same or
a different Sentinel password, or use both endpoints without password
authentication on an appropriately isolated test network. This is a client
library limitation, not a Redis Sentinel requirement. See
[Redisson's AUTH probe](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/connection/SentinelConnectionManager.java).
