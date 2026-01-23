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

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.twinsoft.convertigo.engine.Engine;

/**
 * Shared Redis client helpers (admin tools, diagnostics).
 */
public final class RedisClients {
	private static final Object MUTEX = new Object();
	private static volatile RedisSessionConfiguration configuration;
	private static volatile RedissonClient client;

	private RedisClients() {
	}

	public static RedisSessionConfiguration getConfiguration() {
		ensureInitialized();
		return configuration;
	}

	public static RedissonClient getClient() {
		ensureInitialized();
		return client;
	}

	private static void ensureInitialized() {
		if (client != null && configuration != null) {
			return;
		}
		synchronized (MUTEX) {
			if (client != null && configuration != null) {
				return;
			}
			configuration = RedisSessionConfiguration.fromProperties();
			client = createClient(configuration);
			registerShutdownHook();
		}
	}

	private static RedissonClient createClient(RedisSessionConfiguration cfg) {
		var config = new Config();
		if (cfg.getUsername() != null) {
			config.setUsername(cfg.getUsername());
		}
		if (cfg.getPassword() != null) {
			config.setPassword(cfg.getPassword());
		}
		config.useSingleServer().setAddress(cfg.getAddress())
				.setDatabase(cfg.getDatabase()).setTimeout(cfg.getTimeoutMillis());
		return Redisson.create(config);
	}

	private static void registerShutdownHook() {
		try {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					if (client != null) {
						client.shutdown();
					}
				} catch (Exception e) {
					if (Engine.logEngine != null) {
						Engine.logEngine.debug("(RedisClients) Failed to shutdown shared Redis client", e);
					}
				}
			}, "convertigo-redis-clients-shutdown"));
		} catch (Exception ignore) {
			// ignore hook failures
		}
	}
}
