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

package com.twinsoft.convertigo.engine.studio;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.convertigo.engine.util.SimpleCipher;

/**
 * The Personal Studio Configuration of a desktop Studio: the file studio/psc.txt of the workspace, given at the
 * registration of the Studio, with its owner (owner.email, studio.secret) and the servers it deploys to
 * (deploy.N.*). The Eclipse Studio and the Electron Studio read and write it here, and the Convertigo agent of the
 * Assistant exchanges its owner for its key.
 */
public final class StudioPsc {
	public static final String OWNER_EMAIL = "owner.email";
	public static final String STUDIO_SECRET = "studio.secret";
	private static final String PASSPHRASE = "registration";
	private static final String HEADER = "# PSC file";
	private static final String ANONYMOUS_DOMAIN = "@anonym.ous";

	public static class PscException extends Exception {
		private static final long serialVersionUID = 4617931538208513771L;

		public PscException(String cause) {
			super(cause);
		}
	}

	/** A server of the PSC, deploy.N.&lt;key&gt; for its N, from 1 */
	public enum DeploymentKey {
		adminUser("admin.user"),
		adminPassword("admin.password"),
		server("server", "trial.convertigo.net/cems"),
		sslHttps("ssl.https", "true"),
		sslTrustCert("ssl.trustCert", "false"),
		assembleXsl("assembleXsl", "false");

		private final String key;
		private final String defaultValue;

		DeploymentKey(String key) {
			this(key, null);
		}

		DeploymentKey(String key, String defaultValue) {
			this.defaultValue = defaultValue;
			this.key = key;
		}

		public String key(int i) {
			return "deploy." + i + "." + key;
		}

		public String value(Properties properties, int i) {
			return properties.getProperty(key(i));
		}

		public boolean hasDefault() {
			return defaultValue != null;
		}

		public void setValue(Properties properties, int i) {
			if (hasDefault()) {
				setValue(properties, i, defaultValue);
			}
		}

		public void setValue(Properties properties, int i, String value) {
			properties.setProperty(key(i), value);
		}
	}

	private StudioPsc() {
	}

	/** @return the PSC file of the workspace, which may not exist */
	public static File file() {
		return new File(Engine.USER_WORKSPACE_PATH, "studio/psc.txt");
	}

	/** @return the PSC of the workspace */
	public static Properties decode() throws PscException {
		File pscFile = file();
		if (!pscFile.exists()) {
			throw new PscException("Invalid PSC (the file '" + pscFile.getAbsolutePath() + "' doesn't exist)!");
		}
		try {
			return decode(FileUtils.readFileToString(pscFile, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new PscException("Invalid PSC (failed to read the file '" + pscFile.getAbsolutePath() + "' because of a '"
					+ e.getClass().getSimpleName() + " : " + e.getMessage() + "')!");
		}
	}

	/** @return the properties of a PSC, completed with the defaults of its servers */
	public static Properties decode(String psc) throws PscException {
		if (psc == null || psc.length() == 0) {
			throw new PscException("Invalid PSC (empty)!");
		}
		Properties properties = new Properties();
		try {
			String decipheredPSC = Crypto2.decodeFromHexString(PASSPHRASE, psc);
			if (decipheredPSC == null) {
				throw new PscException("Invalid PSC (unable to decipher)!");
			} else if (!decipheredPSC.startsWith(HEADER)) {
				throw new PscException("Invalid PSC (wrong format)!");
			}
			try {
				properties.load(new StringReader(decipheredPSC));
			} catch (IOException e) {
				throw new PscException("Invalid PSC (cannot load properties)!");
			}
		} catch (PscException e) {
			decodeLegacy(psc, properties, e);
		}

		int i = 0;
		while (++i > 0) {
			if (i > 1 && !properties.containsKey(DeploymentKey.adminUser.key(i))) {
				i = -1;
			} else {
				for (DeploymentKey key : DeploymentKey.values()) {
					if (!properties.containsKey(key.key(i))) {
						if (!key.hasDefault()) {
							throw new PscException("Invalid PSC (altered data)");
						}
						key.setValue(properties, i);
					}
				}
			}
		}
		return properties;
	}

	/** the PSC of the first versions: a server, its user and its password */
	private static void decodeLegacy(String psc, Properties properties, PscException failure) throws PscException {
		try {
			properties.load(new StringReader(SimpleCipher.decode(psc)));

			String server = properties.getProperty("server");
			String user = properties.getProperty("admin.user");
			String password = properties.getProperty("admin.password");

			if (server == null && user == null && password == null) {
				throw failure;
			}
			if (server == null || user == null || password == null) {
				throw new PscException("Invalid PSC (incomplete data)!");
			}
			if (!user.equals(SimpleCipher.decode(password))) {
				throw new PscException("Invalid PSC (altered data)");
			}

			boolean bHttps = Boolean.parseBoolean(properties.getProperty("https"));
			properties.clear();

			DeploymentKey.adminUser.setValue(properties, 1, user);
			DeploymentKey.adminPassword.setValue(properties, 1, password);
			DeploymentKey.server.setValue(properties, 1, server);
			DeploymentKey.sslHttps.setValue(properties, 1, Boolean.toString(bHttps));
		} catch (PscException e) {
			throw e;
		} catch (Exception e) {
			throw failure;
		}
	}

	/**
	 * Writes a PSC as the PSC of the workspace, once decoded.
	 * @return its properties
	 */
	public static Properties save(String psc) throws PscException, IOException {
		String value = psc == null ? "" : psc.trim();
		Properties properties = decode(value);
		FileUtils.writeStringToFile(file(), value, StandardCharsets.UTF_8);
		return properties;
	}

	/** @return the owner of a PSC: its owner.email, else the user of its first server */
	public static String owner(Properties properties) {
		return properties.getProperty(OWNER_EMAIL, DeploymentKey.adminUser.value(properties, 1));
	}

	/** @return whether a PSC has no owner, as the PSC the Eclipse Studio makes without a registration */
	public static boolean isAnonymous(Properties properties) {
		String owner = owner(properties);
		return owner == null || owner.isEmpty() || owner.endsWith(ANONYMOUS_DOMAIN);
	}

	/** @return a PSC without owner nor server */
	public static String makeAnonymous() throws IOException {
		Properties properties = new Properties();
		String anonEmail = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX)
				+ Long.toString(Math.round(Math.random()) % Character.MAX_RADIX, Character.MAX_RADIX) + ANONYMOUS_DOMAIN;

		DeploymentKey.adminUser.setValue(properties, 1, anonEmail);
		DeploymentKey.adminPassword.setValue(properties, 1, "");
		DeploymentKey.server.setValue(properties, 1, "");
		try (StringWriter sw = new StringWriter()) {
			PropertiesUtils.store(properties, sw, " PSC file");
			return Crypto2.encodeToHexString(PASSPHRASE, sw.toString());
		}
	}
}
