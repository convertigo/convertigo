/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sessions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SslVerificationMode;
import org.redisson.config.SubscriptionMode;

import com.twinsoft.convertigo.engine.Engine;

/**
 * Shared Redis client helpers (admin tools, diagnostics).
 */
public final class RedisClients {
	private static final Object MUTEX = new Object();
	private static volatile RedisSessionConfiguration configuration;
	private static volatile RedissonClient client;
	private static volatile boolean shutdownHookRegistered;

	private RedisClients() {
	}

	public static RedisSessionConfiguration getConfiguration() {
		ensureInitialized(null);
		return configuration;
	}

	public static RedissonClient getClient() {
		ensureInitialized(null);
		return client;
	}

	static RedissonClient getClient(RedisSessionConfiguration initialConfiguration) {
		ensureInitialized(initialConfiguration);
		return client;
	}

	public static void reload() {
		synchronized (MUTEX) {
			var newConfiguration = RedisSessionConfiguration.fromProperties();
			if (newConfiguration.equals(configuration) && client != null) {
				return;
			}
			// Keep the effective configuration/client if the replacement cannot initialize.
			var newClient = createClient(newConfiguration);
			var previousClient = client;
			configuration = newConfiguration;
			client = newClient;
			registerShutdownHook();
			shutdownQuietly(previousClient);
		}
	}

	private static void ensureInitialized(RedisSessionConfiguration initialConfiguration) {
		if (client != null && configuration != null) {
			return;
		}
		synchronized (MUTEX) {
			if (client != null && configuration != null) {
				return;
			}
			configuration = initialConfiguration != null ? initialConfiguration : RedisSessionConfiguration.fromProperties();
			client = createClient(configuration);
			registerShutdownHook();
		}
	}

	private static RedissonClient createClient(RedisSessionConfiguration cfg) {
		return Redisson.create(createConfig(cfg));
	}

	static Config createConfig(RedisSessionConfiguration cfg) {
		var config = new Config();
		if (cfg.getUsername() != null) {
			config.setUsername(cfg.getUsername());
		}
		if (cfg.getPassword() != null) {
			config.setPassword(cfg.getPassword());
		}
		if (cfg.getTopology() == RedisTopology.sentinel) {
			// Redisson 4.3.1 skips Sentinel AUTH probing when no data password is set,
			// even if sentinelPassword is configured (SentinelConnectionManager.checkAuth).
			// Fail explicitly instead of silently ignoring the supplied Sentinel credentials.
			if (cfg.getSentinelPassword() != null && cfg.getPassword() == null) {
				throw new IllegalArgumentException("Redis Sentinel authentication with no Redis data password is not supported by Redisson 4.3.1. Configure session.redis.password on the data nodes, or use Sentinel without password authentication.");
			}
			config.useSentinelServers()
					.setMasterName(cfg.getSentinelMaster())
					.addSentinelAddress(cfg.getSentinelAddresses())
					.setSentinelUsername(cfg.getSentinelUsername())
					.setSentinelPassword(cfg.getSentinelPassword())
					.setReadMode(ReadMode.MASTER)
					.setSubscriptionMode(SubscriptionMode.MASTER)
					.setDatabase(cfg.getDatabase())
					.setTimeout(cfg.getTimeoutMillis())
					.setMasterConnectionPoolSize(cfg.getConnectionPoolSize())
					.setMasterConnectionMinimumIdleSize(cfg.getConnectionMinimumIdleSize());
		} else {
			config.useSingleServer()
					.setAddress(cfg.getAddress())
					.setDatabase(cfg.getDatabase())
					.setTimeout(cfg.getTimeoutMillis())
					.setConnectionPoolSize(cfg.getConnectionPoolSize())
					.setConnectionMinimumIdleSize(cfg.getConnectionMinimumIdleSize());
		}
		configureSsl(cfg, config);
		return config;
	}

	static Map<String, Object> diagnostics() {
		try {
			var cfg = configuration;
			var map = (cfg != null ? cfg : RedisSessionConfiguration.fromProperties()).diagnostics();
			var currentClient = client;
			if (cfg != null && cfg.getTopology() == RedisTopology.sentinel && currentClient != null) {
				try {
					// In-memory Redisson topology: no Redis command or DNS lookup during an outage.
					var master = currentClient.getRedisNodes(RedisNodes.SENTINEL_MASTER_SLAVE).getMaster();
					if (master != null) {
						var address = master.getAddr();
						var host = address.getAddress().getHostAddress();
						map.put("masterAddress", (host.contains(":") ? "[" + host + "]" : host) + ":" + address.getPort());
					}
				} catch (RuntimeException e) {
					// The client may be shutting down/reloading; keep configured topology and health.
				}
			}
			return map;
		} catch (IllegalArgumentException e) {
			return Map.of("configurationError", e.getMessage());
		}
	}

	private static void configureSsl(RedisSessionConfiguration cfg, Config config) {
		if (!cfg.isSsl()) {
			return;
		}
		try {
			if (cfg.getSslTruststore() != null) {
				config.setSslTruststore(toUrl(cfg.getSslTruststore()));
			}
			if (cfg.getSslTruststorePassword() != null) {
				config.setSslTruststorePassword(cfg.getSslTruststorePassword());
			}
			if (cfg.getSslKeystore() != null) {
				config.setSslKeystore(toUrl(cfg.getSslKeystore()));
			}
			if (cfg.getSslKeystorePassword() != null) {
				config.setSslKeystorePassword(cfg.getSslKeystorePassword());
			}
			if (cfg.getSslKeystoreType() != null) {
				config.setSslKeystoreType(cfg.getSslKeystoreType());
			}
			var verificationMode = parseSslVerificationMode(cfg.getSslVerificationMode());
			if (verificationMode != null) {
				config.setSslVerificationMode(verificationMode);
			}
			var protocols = cfg.getSslProtocols();
			if (protocols.length > 0) {
				config.setSslProtocols(protocols);
			}
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid Redis SSL configuration", e);
		}
	}

	private static URL toUrl(String location) throws MalformedURLException {
		try {
			var uri = URI.create(location);
			if (uri.getScheme() != null) {
				return uri.toURL();
			}
		} catch (IllegalArgumentException e) {
			// Fall back to a local filesystem path.
		}
		return new File(location).toURI().toURL();
	}

	private static SslVerificationMode parseSslVerificationMode(String value) {
		if (value == null) {
			return null;
		}
		try {
			return SslVerificationMode.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid Redis SSL verification mode: " + value, e);
		}
	}

	private static void registerShutdownHook() {
		if (shutdownHookRegistered) {
			return;
		}
		try {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					if (client != null) {
						client.shutdown();
					}
				} catch (Exception e) {
					Engine.logRedis.debug("(RedisClients) Failed to shutdown shared Redis client", e);
				}
			}, "convertigo-redis-clients-shutdown"));
			shutdownHookRegistered = true;
		} catch (Exception ignore) {
			// ignore hook failures
		}
	}

	private static void shutdownQuietly(RedissonClient clientToShutdown) {
		if (clientToShutdown == null || clientToShutdown == client) {
			return;
		}
		try {
			clientToShutdown.shutdown();
		} catch (Exception e) {
			Engine.logRedis.debug("(RedisClients) Failed to shutdown previous shared Redis client", e);
		}
	}
}
