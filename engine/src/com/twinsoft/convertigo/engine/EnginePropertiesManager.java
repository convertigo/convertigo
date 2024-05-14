/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;

public class EnginePropertiesManager {
	private enum Visibility { VISIBLE, HIDDEN, HIDDEN_CLOUD, HIDDEN_SERVER };

	private @Retention(RetentionPolicy.RUNTIME)
	@interface CategoryOptions {
		Visibility visibility() default Visibility.VISIBLE;
		Role[] viewRoles() default {};
		Role[] configRoles() default {};
	}

	private @Retention(RetentionPolicy.RUNTIME)
	@interface PropertyOptions {
		Visibility visibility() default Visibility.VISIBLE;
		boolean advance() default false;
		Class<? extends ComboEnum> combo() default EmptyCombo.class;
		PropertyType propertyType() default PropertyType.Text;
		boolean ciphered() default false;
	}

	/**
	 * The propertEnginePropertiesManagerr the Convertigo engine.
	 */
	static final String PROPERTIES_FILE_NAME = "/engine.properties";

	private static final String SYSTEM_PROP_PREFIX = "convertigo.engine.";

	private static String STUDIO_APPLICATION_SERVER_CONVERTIGO_URL = null;

	public interface ComboEnum {
		String getDisplay();
		String getValue();
	}

	private enum EmptyCombo implements ComboEnum {
		;

		public String getDisplay() {
			return null;
		}

		public String getValue() {
			return null;
		}
	}
	
	private enum RootLogLevels implements ComboEnum {
		FATAL,
		ERROR,
		WARN,
		INFO,
		DEBUG,
		TRACE;

		public String getDisplay() {
			return name();
		}

		public String getValue() {
			return name();
		}
	}
	
	private enum LogLevels implements ComboEnum {
		INHERITED ("", "Inherited from root logger"),
		FATAL,
		ERROR,
		WARN,
		INFO,
		DEBUG,
		TRACE;

		final String display;
		final String value;

		LogLevels(String value, String display) {
			this.display = display;
			this.value = value;
		}

		LogLevels() {
			this.display = name();
			this.value = name();
		}

		public String getDisplay() {
			return display;
		}

		public String getValue() {
			return value;
		}
	}

	private enum XsltEngine implements ComboEnum {
		xalan_xslt ("xalan/xslt", "Java Xalan"),
		xalan_xsltc ("xalan/xsltc", "Java Xalan (XSLTC)");

		final String display;
		final String value;

		XsltEngine(String value, String display) {
			this.display = display;
			this.value = value;
		}

		public String getDisplay() {
			return display;
		}

		public String getValue() {
			return value;
		}
	}

	public enum ProxyMode implements ComboEnum {
		off ("disabled"),
		auto ("automatic"),
		manual ("manual");

		final String display;
		final String value;

		ProxyMode(String display) {
			this.display = display;
			this.value = name();
		}

		public String getDisplay() {
			return display;
		}

		public String getValue() {
			return value;
		}
	}

	public enum ProxyMethod implements ComboEnum {
		anonymous ("anonymous"),
		basic ("basic"),
		ntlm ("NTLM");

		final String display;
		final String value;

		ProxyMethod(String display) {
			this.display = display;
			this.value = name();
		}

		public String getDisplay() {
			return display;
		}

		public String getValue() {
			return value;
		}
	}

	public enum PropertyType { Text, PasswordHash, PasswordPlain, Boolean, Combo, Array };

	public enum PropertyCategory {
		Main ("Main Parameters"),
		Account ("Accounts and Security"),
		@CategoryOptions(viewRoles = {Role.LOGS_VIEW, Role.LOGS_CONFIG}, configRoles = {Role.LOGS_CONFIG})
		Logs ("Logs"),
		@CategoryOptions(viewRoles = {Role.LOGS_VIEW, Role.LOGS_CONFIG}, configRoles = {Role.LOGS_CONFIG})
		Context ("Real-time Activity Monitoring"),
		XmlGeneration ("XML Generation"),
		HttpClient ("HTTP Client"),
		Network ("Network"),
		Proxy ("Proxy"),
		@CategoryOptions(viewRoles = {Role.CERTIFICATE_VIEW, Role.CERTIFICATE_CONFIG}, configRoles = {Role.CERTIFICATE_CONFIG})
		Ssl ("SSL"),
		@CategoryOptions(viewRoles = {Role.CACHE_VIEW, Role.CACHE_CONFIG}, configRoles = {Role.CACHE_CONFIG})
		Cache ("Cache"),
		@CategoryOptions(visibility = Visibility.HIDDEN_CLOUD)
		Carioca ("Legacy Carioca Portal"),
		@CategoryOptions(visibility = Visibility.HIDDEN_CLOUD)
		Analytics ("Analytics"),
		Notifications ("Notifications"),
		MobileBuilder ("Mobile Builder"),
		@CategoryOptions(visibility = Visibility.HIDDEN_CLOUD)
		FullSync ("FullSync")
		;

		final String displayName;

		PropertyCategory(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public boolean isVisible() {
			CategoryOptions categoryOptions = GenericUtils.getAnnotation(CategoryOptions.class, this);
			if (categoryOptions != null) {
				Visibility visibility = categoryOptions.visibility();
				switch (visibility) {
				case VISIBLE: return true;
				case HIDDEN_CLOUD: return !Engine.isCloudMode();
				case HIDDEN: return false;
				case HIDDEN_SERVER: return !Engine.isEngineMode();
				}
			}
			return true;
		}

		public Role[] viewRoles() {
			CategoryOptions categoryOptions = GenericUtils.getAnnotation(CategoryOptions.class, this);
			if (categoryOptions != null) {
				return categoryOptions.viewRoles();
			}
			return null;
		}

		public Role[] configRoles() {
			CategoryOptions categoryOptions = GenericUtils.getAnnotation(CategoryOptions.class, this);
			if (categoryOptions != null) {
				return categoryOptions.configRoles();
			}
			return null;
		}

		public static PropertyCategory[] getSortedValues() {
			PropertyCategory[] properties = values();
			Arrays.sort(properties, PropertyByNameComparator.INSTANCE);
			return properties;
		}

		private static class PropertyByNameComparator implements Comparator<PropertyCategory> {

			public static final Comparator<PropertyCategory> INSTANCE = new PropertyByNameComparator();

			public int compare(PropertyCategory enum1, PropertyCategory enum2) {	 
				if (enum1.equals(Main)) {
					return -1;
				}
				if (enum2.equals(Main)) {
					return 1;
				}
				return enum1.getDisplayName().compareTo(enum2.getDisplayName());
			}

		}
	};

	@PropertyOptions
	public enum PropertyName {
		/** MAIN */
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		APPLICATION_SERVER_CONVERTIGO_URL ("application_server.convertigo.url", "http://localhost:" + (Engine.isStudioMode() ? "1" : "2")+ "8080/convertigo", "Convertigo Server local URL", PropertyCategory.Main),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		APPLICATION_SERVER_CONVERTIGO_ENDPOINT ("application_server.convertigo.endpoint", "", "Convertigo Server endpoint URL", PropertyCategory.Main),
		@PropertyOptions(visibility = Visibility.HIDDEN)
		APPLICATION_SERVER_MASHUP_URL ("application_server.mashup.url", "http://localhost:18080/convertigo", "Mashup composer server base URL", PropertyCategory.Main),
		DOCUMENT_THREADING_MAX_WORKER_THREADS ("document.threading.max_worker_threads", "100", "Maximum number of worker threads", PropertyCategory.Main),
		CONVERTIGO_MAX_CONTEXTS ("convertigo.max_context", "750", "Maximum number of contexts", PropertyCategory.Main),
		GIT_CONTAINER ("convertigo.git.container", new File(Engine.USER_WORKSPACE_PATH, "git").getAbsolutePath(), "Git container (autoimport)", PropertyCategory.Main),
		@PropertyOptions(propertyType = PropertyType.Boolean)
		XSRF_ADMIN ("convertigo.xsrf.admin", "true", "Enable XSRF protection for Administration Console", PropertyCategory.Main),
		@PropertyOptions(propertyType = PropertyType.Boolean)
		XSRF_API ("convertigo.xsrf.projects", "false", "Enable XSRF protection for projects", PropertyCategory.Main),

		/** MAIN ADVANCE */
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		CONVERTIGO_PRODUCT_VERSION_CHECK ("convertigo.product_version_check", "true", "Product version check", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		DOCUMENT_THREADING_USE_STOP_METHOD ("document.threading.use_stop_method", "false", "Use the Java Thread.stop() method in order to finish threads", PropertyCategory.Main),
		@PropertyOptions(advance = true)
		POOL_MANAGER_TIMEOUT ("pool.manager.timeout", "-1", "Time allowed for pool management task in seconds (-1 for disable)", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		PROJECTS_DATA_COMPATIBILITY_MODE ("projects_data.compatibility_mode", "false", "Enable the compatibility mode for projects data (required for JSP usage); engine restart required", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SEQUENCE_STEPS_USE_SAME_JSESSION ("sequence.steps.use_same_jsession", "true", "Use same JSESSIONID for sequences and steps", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SOAP_REQUEST_ADD_XML_ENCODING_CHARSET ("soap.request.add_xml_encoding_charset", "false", "Add XML encoding charset for SOAP requests", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		THROW_HTTP_500 ("throw_http_500", "false", "Throw HTTP 500 in case of unrecoverable servlet error", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		THROW_HTTP_500_SOAP_FAULT ("throw_http_500.soap_fault", "true", "Throw HTTP 500 in case of SOAP fault", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		HIDING_ERROR_INFORMATION ("hiding_error_information", "false", "Hide all error informations", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_REQUESTABLE_INFORMATION ("show_error_requestable_information", "true", "Show error requestable informations", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_CONTEXT_INFORMATION ("show_error_context_information", "false", "Show error context informations", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_TYPE ("show_error_type", "true", "Show error type", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_CODE ("show_error_code", "true", "Show error code", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_MESSAGE ("show_error_message", "true", "Show error message", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_DETAIL ("show_error_detail", "true", "Show error detail", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_EXCEPTION ("show_error_exception", "false", "Show error exception", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SHOW_ERROR_STACKTRACE ("show_error_stacktrace", "false", "Show error stacktrace", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN)
		UPDATE_STEPS ("update.steps", "false", "Update steps", PropertyCategory.Main),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		CRYPTO_PASSPHRASE ("crypto.passphrase", "A8dkLmsdfkKze0e34FGh", "Cryptographic services passphrase", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		ZIP_BACKUP_OLD_PROJECT ("project.zip_backup_old", "true", "Automatically performs a dated zip backup of replaced projects", PropertyCategory.Main),
		@PropertyOptions(advance = true)
		CORS_POLICY ("cors.policy", "=Origin", "CORS Policy\n• empty: disallow all\n• '=Origin': use client 'Origin' header\n• 'url1#url2#url3': allow if 'Origin' one of 'url'", PropertyCategory.Main),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		DELEGATE_URL ("delegate.url", "", "Delegate URL for extra functionality", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		AUTO_GC ("auto.gc", "false", "Automatically GC on low usage (every 10 min)", PropertyCategory.Main),

		/** ACCOUNTS */
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		ADMIN_USERNAME ("admin.username", "admin", "Admin username", PropertyCategory.Account),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD, propertyType = PropertyType.PasswordHash)
		ADMIN_PASSWORD ("admin.password", encodeValue(PropertyType.PasswordHash, "admin"), "Admin password", PropertyCategory.Account),
		TEST_PLATFORM_USERNAME ("testplatform.username", "", "Test Platform username (leave it blank for anonymous access)", PropertyCategory.Account),
		@PropertyOptions(propertyType = PropertyType.PasswordHash)
		TEST_PLATFORM_PASSWORD ("testplatform.password", encodeValue(PropertyType.PasswordHash, ""), "Test Platform password", PropertyCategory.Account),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SECURITY_FILTER ("security.filter", "false", "Security Filter", PropertyCategory.Account),
		@PropertyOptions(advance = true)
		USER_PASSWORD_REGEX ("user.password.regexp", "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[\\w~@#$%^&*+=`|{}:;!.?\\\"()\\[\\]-]{8,20}$", "RegularExpression used to validate password change for Admin accounts.", PropertyCategory.Account),
		@PropertyOptions(advance = true)
		USER_PASSWORD_INSTRUCTION ("user.password.instruction", "must respect at least 1 lowercase, 1 uppercase, 1 digit and between 8-20 characters.", "Instruction in case of RegularExpression failure for password change.", PropertyCategory.Account),

		/** LOGS */
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		LOG_FILE_ENABLE("log.file.enable", "true", "Log into files", PropertyCategory.Logs),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		LOG_STDOUT_ENABLE("log.stdout.enable", "false", "Log into the standard console output", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = RootLogLevels.class)
		LOG4J_LOGGER_CEMS ("log4j.logger.cems", RootLogLevels.INFO.getValue(), "Log4J root logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_ADMIN ("log4j.logger.cems.Admin", LogLevels.WARN.getValue(), "Log4J admin logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = RootLogLevels.class)
		LOG4J_LOGGER_CEMS_CONTEXT_AUDIT ("log4j.logger.cems.Context.Audit", RootLogLevels.INFO.getValue(), "Log4J audit context logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_BEANS ("log4j.logger.cems.Beans", LogLevels.INHERITED.getValue(), "Log4J beans logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_BILLERS ("log4j.logger.cems.Billers", LogLevels.WARN.getValue(), "Log4J billers logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_CACHEMANAGER ("log4j.logger.cems.CacheManager", LogLevels.WARN.getValue(), "Log4J cache manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_CERTIFICATEMANAGER ("log4j.logger.cems.CertificateManager", LogLevels.WARN.getValue(), "Log4J certificate manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_CONTEXT ("log4j.logger.cems.Context", LogLevels.INHERITED.getValue(), "Log4J context logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_CONTEXTMANAGER ("log4j.logger.cems.ContextManager", LogLevels.WARN.getValue(), "Log4J context manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_CDBMANAGER ("log4j.logger.cems.CouchDbManager", LogLevels.WARN.getValue(), "Log4J couch DB manager output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_DATABASEOBJECTMANAGER ("log4j.logger.cems.DatabaseObjectManager", LogLevels.WARN.getValue(), "Log4J database objects manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_DEVICES ("log4j.logger.cems.Devices", LogLevels.INFO.getValue(), "Log4J devices output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_EMULATORS ("log4j.logger.cems.Emulators", LogLevels.INFO.getValue(), "Log4J emulators output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_ENGINE ("log4j.logger.cems.Engine", LogLevels.INHERITED.getValue(), "Log4J engine logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_JOBMANAGER ("log4j.logger.cems.JobManager", LogLevels.WARN.getValue(), "Log4J job manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_PROXYMANAGER ("log4j.logger.cems.ProxyManager", LogLevels.INFO.getValue(), "Log4J proxy manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_SCHEDULER ("log4j.logger.cems.Scheduler", LogLevels.INFO.getValue(), "Log4J scheduler output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_SECURITYFILTER ("log4j.logger.cems.SecurityFilter", LogLevels.WARN.getValue(), "Log4J security filter output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_SECURITYTOKENMANAGER ("log4j.logger.cems.SecurityTokenManager", LogLevels.INFO.getValue(), "Log4J security token manager output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_SITECLIPPER ("log4j.logger.cems.SiteClipper", LogLevels.INFO.getValue(), "Log4J site clipper output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_STATISTICS ("log4j.logger.cems.Statistics", LogLevels.INFO.getValue(), "Log4J statistics logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class, visibility = Visibility.HIDDEN_SERVER)
		LOG4J_LOGGER_CEMS_STUDIO ("log4j.logger.cems.Studio", LogLevels.WARN.getValue(), "Log4J studio logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_TRACEPLAYERMANAGER ("log4j.logger.cems.TracePlayerManager", LogLevels.WARN.getValue(), "Log4J trace player manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_USAGEMONITOR ("log4j.logger.cems.UsageMonitor", LogLevels.WARN.getValue(), "Log4J usage monitor logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_CONTEXT_USER ("log4j.logger.cems.Context.User", LogLevels.INHERITED.getValue(), "Log4J user context logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_USER ("log4j.logger.cems.User", LogLevels.INFO.getValue(), "Log4J user output logger", PropertyCategory.Logs),

		/** LOGS ADVANCE */
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG_EXPLICIT_VARIABLES ("log.explicit_variables", "contextid,project,sequence,connector,transaction,user,clientip,clienthostname", "Explicit variables", PropertyCategory.Logs),
		// New appender for the Audit logger.
		LOG4J_APPENDER_AUDITAPPENDER ("log4j.appender.AuditAppender", "org.apache.log4j.RollingFileAppender", "Log4J audit appender", PropertyCategory.Logs),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_AUDITAPPENDER_FILE ("log4j.appender.AuditAppender.File","${log.directory}/audit.log", "Log4J audit appender file", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_AUDITAPPENDER_LAYOUT ("log4j.appender.AuditAppender.layout","org.apache.log4j.PatternLayout", "Log4J audit appender layout", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG4J_APPENDER_AUDITAPPENDER_LAYOUT_CONVERSIONPATTERN ("log4j.appender.AuditAppender.layout.ConversionPattern", "!%c{1} | %d | %-5p | %m%n", "Log4J audit appender layout conversion pattern", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_AUDITAPPENDER_MAXBACKUPINDEX ("log4j.appender.AuditAppender.MaxBackupIndex", "25", "Log4J audit appender max backup index", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_AUDITAPPENDER_MAXFILESIZE ("log4j.appender.AuditAppender.MaxFileSize", "10MB", "Log4J audit appender max file size", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_CEMSAPPENDER ("log4j.appender.CemsAppender", "org.apache.log4j.RollingFileAppender", "Log4J default appender", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_CEMSAPPENDER_ENCODING ("log4j.appender.CemsAppender.Encoding","UTF-8", "Log4J default appender encoding (requires JVM restart)", PropertyCategory.Logs),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_CEMSAPPENDER_FILE ("log4j.appender.CemsAppender.File","${log.directory}/engine.log", "Log4J default appender file", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_CEMSAPPENDER_LAYOUT ("log4j.appender.CemsAppender.layout","org.apache.log4j.PatternLayout", "Log4J default appender layout", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG4J_APPENDER_CEMSAPPENDER_LAYOUT_CONVERSIONPATTERN ("log4j.appender.CemsAppender.layout.ConversionPattern", "!%-28c{1} | %d | %-5p | %-32t | %X{ContextualParameters}%m%n", "Log4J default appender layout conversion pattern", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_CEMSAPPENDER_MAXBACKUPINDEX ("log4j.appender.CemsAppender.MaxBackupIndex", "25", "Log4J default appender max backup index", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_CEMSAPPENDER_MAXFILESIZE ("log4j.appender.CemsAppender.MaxFileSize", "10MB", "Log4J default appender max file size", PropertyCategory.Logs),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		LOG4J_ADDITIVITY_CEMS ("log4j.additivity.cems", "false", "Log4J root logger additivity", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG_START_OF_LINE_CHARACTER ("log.start_of_line_character", "!", "Start-of-line character", PropertyCategory.Logs),

		/** NETWORK */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		NET_GZIP ("net.gzip", "true", "Enable GZip response for most text responses (need the header Accept-Encoding: gzip)", PropertyCategory.Network),
		NET_MAX_AGE ("net.max-age", "10", "Set the Cache-Control: max-age value in seconds, for static resources", PropertyCategory.Network),
		@PropertyOptions(propertyType = PropertyType.Boolean)
		NET_REVERSE_DNS ("net.reverse_dns", "false", "Use DNS reverse search for finding host names", PropertyCategory.Network),
		FILE_UPLOAD_MAX_REQUEST_SIZE ("net.upload.max_request_size", "-1", "Maximum allowed size of a complete multipart request (in bytes). Value -1 indicates no limit.", PropertyCategory.Network),
		FILE_UPLOAD_MAX_FILE_SIZE ("net.upload.max_request_size", "10485760", "Maximum allowed size of a single uploaded file (in bytes).", PropertyCategory.Network),

		/** HTTPCLIENT */
		HTTP_CLIENT_MAX_TOTAL_CONNECTIONS ("http_client.max_total_connections", "100", "Maximal number of HTTP connections (from 1 to 65535)", PropertyCategory.HttpClient),
		HTTP_CLIENT_MAX_CONNECTIONS_PER_HOST ("http_client.max_connections_per_host", "50", "Maximal number of HTTP connections per host (from 1 to 255)", PropertyCategory.HttpClient),

		/** CONNECTORS MONITORING */
		@PropertyOptions(propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		CONNECTORS_MONITORING ("connectors.monitoring", "false", "Display running connectors in monitor of Legacy connectors", PropertyCategory.Context),
		@PropertyOptions(propertyType = PropertyType.Boolean)
		DOCUMENT_LOG_SCREEN_DUMPS ("document.log.screen_dumps", "false", "Trace in logs the screen dumps of the running Legacy connectors", PropertyCategory.Context),

		/** XML GENERATION */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		DOCUMENT_INCLUDE_STATISTICS ("document.include_statistics", "false", "Insert statistics in the generated document", PropertyCategory.XmlGeneration),

		/** XML GENERATION ADVANCE */
		@PropertyOptions(advance = true, propertyType = PropertyType.Combo, combo = XsltEngine.class)
		DOCUMENT_XSLT_ENGINE ("document.xslt_engine", XsltEngine.xalan_xsltc.getValue(), "XSLT engine", PropertyCategory.XmlGeneration),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		DOCUMENT_NAMESPACE_AWARE("document.namespace.aware", "false", "Set namespace aware", PropertyCategory.XmlGeneration),
		@PropertyOptions(advance = true)
		DOCUMENT_FROMSCHEMA_DEPTH("document.fromschema.depth","100","Maximum number of elements for XML sample generation based on schema",PropertyCategory.XmlGeneration),

		/** PROXY */
		@PropertyOptions(propertyType = PropertyType.Combo, combo = ProxyMode.class)
		PROXY_SETTINGS_MODE ("htmlProxy.mode", ProxyMode.off.getValue(), "Proxy mode", PropertyCategory.Proxy),
		PROXY_SETTINGS_PORT ("htmlProxy.port", "8080", "Proxy port", PropertyCategory.Proxy),
		PROXY_SETTINGS_HOST ("htmlProxy.host", "localhost", "Proxy host", PropertyCategory.Proxy),
		PROXY_SETTINGS_BY_PASS_DOMAINS ("htmlProxy.bpdomains", "localhost,127.0.0.1", "Do not apply proxy settings on", PropertyCategory.Proxy),
		PROXY_SETTINGS_AUTO ("htmlProxy.auto", "", "Autoconfiguration proxy url", PropertyCategory.Proxy),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = ProxyMethod.class)
		PROXY_SETTINGS_METHOD ("htmlProxy.method", ProxyMethod.anonymous.getValue(), "Proxy authentication method", PropertyCategory.Proxy),
		PROXY_SETTINGS_USER ("htmlProxy.user", "", "Username", PropertyCategory.Proxy),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		PROXY_SETTINGS_PASSWORD ("htmlProxy.password", "", "Password", PropertyCategory.Proxy),

		/** SSL */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		SSL_DEBUG ("ssl.debug", "false", "SSL debug output (requires JVM restart); only available for HTTP connectors", PropertyCategory.Ssl),
		@PropertyOptions(propertyType = PropertyType.Array)
		SSL_ISSUERS ("ssl.issuers", "", "SSL issuers", PropertyCategory.Ssl),

		/** CACHE */
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		CACHE_MANAGER_CLASS ("cache_manager.class", "com.twinsoft.convertigo.engine.cache.FileCacheManager", "Cache manager class", PropertyCategory.Cache),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		CACHE_MANAGER_FILECACHE_DIRECTORY ("cache_manager.filecache.directory", "${user.workspace}/cache", "File cache directory", PropertyCategory.Cache),
		CACHE_MANAGER_SCAN_DELAY ("cache_manager.scan_delay", "60", "Cache scan delay (in seconds)", PropertyCategory.Cache),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		CACHE_MANAGER_USE_WEAK ("cache_manager.weak", "false", "Allow to cache responses in memory until the next GC", PropertyCategory.Cache),
		@PropertyOptions(advance = false, propertyType = PropertyType.Boolean)
		DISABLE_CACHE ("disable.cache", "false", "Disable Cache", PropertyCategory.Cache),

		/** CARIOCA */
		CARIOCA_DEFAULT_USER_NAME ("carioca.default.user.name", "admin", "Default user name", PropertyCategory.Carioca),
		CARIOCA_DEFAULT_USER_PASSWORD ("carioca.default.user.password", "admin", "Default user password", PropertyCategory.Carioca),
		CARIOCA_SESSION_KEY_LIFE_TIME ("carioca.session_key.life_time", "60", "Default session key life time (in seconds)", PropertyCategory.Carioca),
		CARIOCA_URL ("carioca.url", "${user.workspace}/minime", "Carioca access URL", PropertyCategory.Carioca),

		/** ANALYTICS */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		ANALYTICS_PERSISTENCE_ENABLED ("billing.enabled", "false", "Enable persistence analytics (JDBC)", PropertyCategory.Analytics),
		@PropertyOptions(propertyType = PropertyType.Boolean)
		ANALYTICS_GOOGLE_ENABLED ("billing.google.enabled", "false", "Enable google analytics", PropertyCategory.Analytics),

		/** ANALYTICS ADVANCE */
		@PropertyOptions(advance = true)
		ANALYTICS_PERSISTENCE_DIALECT ("billing.persistence.dialect", "org.hibernate.dialect.MySQLDialect", "Persistence SQL Dialect", PropertyCategory.Analytics),
		@PropertyOptions(advance = true)
		ANALYTICS_PERSISTENCE_JDBC_DRIVER ("billing.persistence.jdbc.driver", "org.mariadb.jdbc.Driver", "Persistence JDBC driver", PropertyCategory.Analytics),
		@PropertyOptions(advance = true, propertyType = PropertyType.PasswordPlain, ciphered = true)
		ANALYTICS_PERSISTENCE_JDBC_PASSWORD ("billing.persistence.jdbc.password", "", "Persistence JDBC password", PropertyCategory.Analytics),
		@PropertyOptions(advance = true)
		ANALYTICS_PERSISTENCE_JDBC_URL ("billing.persistence.jdbc.url", "jdbc:mariadb://localhost:3306/c8oAnalytics", "Persistence JDBC URL", PropertyCategory.Analytics),
		@PropertyOptions(advance = true)
		ANALYTICS_PERSISTENCE_JDBC_USERNAME ("billing.persistence.jdbc.username", "", "Persistence JDBC username", PropertyCategory.Analytics),
		@PropertyOptions(advance = true)
		ANALYTICS_PERSISTENCE_MAX_RETRY ("billing.persistence.jdbc.maxretry", "2", "JDBC max retry on connection failed", PropertyCategory.Analytics),
		@PropertyOptions(advance = true)
		ANALYTICS_GOOGLE_ID ("billing.google.analytics.measurement_id", "", "Google Analytics Measurement ID", PropertyCategory.Analytics),
		@PropertyOptions(advance = true)
		ANALYTICS_GOOGLE_SECRET ("billing.google.analytics.api_secret", "", "Google Analytics API Secret", PropertyCategory.Analytics),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		ANALYTICS_OLD_GOOGLE_ID ("billing.google.analytics.id", "", "Google Old Analytics ID", PropertyCategory.Analytics),


		/** NOTIFICATIONS */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		NOTIFICATIONS_NOTIFY_PROJECT_DEPLOYMENT ("notifications.notify.project_deployment", "false", "Notify project deployment", PropertyCategory.Notifications),
		@PropertyOptions(advance = true)
		NOTIFICATIONS_TARGET_EMAIL ("notifications.target_email", "", "Target email", PropertyCategory.Notifications),
		@PropertyOptions(advance = true)
		NOTIFICATIONS_SMTP_HOST ("notifications.smtp.host", "", "STMP host", PropertyCategory.Notifications),
		@PropertyOptions(advance = true)
		NOTIFICATIONS_SMTP_PORT ("notifications.smtp.port", "465", "STMP port", PropertyCategory.Notifications),
		@PropertyOptions(advance = true)
		NOTIFICATIONS_SMTP_USER ("notifications.smtp.user", "", "STMP user", PropertyCategory.Notifications),
		@PropertyOptions(advance = true, propertyType = PropertyType.PasswordPlain, ciphered = true)
		NOTIFICATIONS_SMTP_PASSWORD ("notifications.smtp.password", "", "STMP password", PropertyCategory.Notifications),

		/** MOBILE BUILDER */
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_AUTHENTICATION_TOKEN ("mobile.builder.auth_token", "", "Mobile builder authentication token", PropertyCategory.MobileBuilder),

		//ANDROID
		MOBILE_BUILDER_ANDROID_CERTIFICATE_TITLE ("mobile.builder.android_certificate_title", "", "Android certificate title", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_ANDROID_CERTIFICATE_PW ("mobile.builder.android_certificate_pw", "", "Android certificate password", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_ANDROID_KEYSTORE_PW ("mobile.builder.android_keystore_pw", "", "Android keyStore password", PropertyCategory.MobileBuilder),

		//iOS
		MOBILE_BUILDER_IOS_CERTIFICATE_TITLE ("mobile.builder.ios_certificate_title", "", "iOS certificate title", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_IOS_CERTIFICATE_PW ("mobile.builder.ios_certificate_pw", "", "iOS certificate password", PropertyCategory.MobileBuilder),

		//WINDOWSPHONE
		MOBILE_BUILDER_WINDOWSPHONE_PUBLISHER_ID_TITLE ("mobile.builder.windows_phone_publisher_id_title", "", "Windows Phone publisher ID title", PropertyCategory.MobileBuilder),

		@PropertyOptions(advance = true)
		MOBILE_BUILDER_PLATFORM_URL ("mobile.builder.platform_url", "https://build.convertigo.net/cmb/PhoneGapBuilder", "Mobile builder platform URL", PropertyCategory.MobileBuilder),

		/** FULL SYNC */
		@PropertyOptions(propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		FULLSYNC_USE_POUCHDB ("fullsync.pouchdb", Engine.isStudioMode() ? "true" : " false", "Use PouchDB for FullSync (prefer CouchDB for production)", PropertyCategory.FullSync),
		FULLSYNC_COUCH_URL ("fullsync.couch.url", "http://127.0.0.1:5984", "Couch DB URL for FullSync", PropertyCategory.FullSync),
		FULLSYNC_COUCH_USERNAME ("fullsync.couch.username", "", "Couch DB username for FullSync", PropertyCategory.FullSync),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		FULLSYNC_COUCH_PASSWORD ("fullsync.couch.password", "", "Couch DB password for FullSync", PropertyCategory.FullSync),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		FULLSYNC_COUCH_PREFIX ("fullsync.couch.prefix", "", "Couch DB prefix for all FullSync databases", PropertyCategory.FullSync),

		/* End of configuration keys definition */;

		final String key;
		final String defaultValue;
		final String description;
		final PropertyCategory category;

		PropertyName(String key, String defaultValue, String description, PropertyCategory category) {
			this.key = key;
			this.defaultValue = defaultValue;
			this.description = description;
			this.category = category;
		}

		@Override
		public String toString() {
			return key;
		}

		private PropertyOptions getOptions () {
			PropertyOptions options = GenericUtils.getAnnotation(PropertyOptions.class, this);
			if (options == null) {
				options = getClass().getAnnotation(PropertyOptions.class);
			}
			return options;
		}

		public String getKey() {
			return key;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public String getDescription() {
			return description;
		}

		public PropertyCategory getCategory() {
			return category;
		}

		public boolean isAdvance() {
			return getOptions().advance();
		}

		public boolean isCiphered() {
			return getOptions().ciphered();
		}

		public PropertyType getType() {
			return getOptions().propertyType();
		}

		public ComboEnum[] getCombo() {
			return getOptions().combo().getEnumConstants();
		}

		public boolean isVisible() {
			PropertyOptions propertyOptions = GenericUtils.getAnnotation(PropertyOptions.class, this);
			if (propertyOptions != null) {
				Visibility visibility = propertyOptions.visibility();
				switch (visibility) {
				case VISIBLE: return true;
				case HIDDEN_CLOUD: return !Engine.isCloudMode();
				case HIDDEN: return false;
				case HIDDEN_SERVER: return !Engine.isEngineMode();
				}
			}
			return true;
		}
	};

	private static Properties properties;
	private static Properties system_properties = null;

	public static String getProperty(PropertyName property) {
		return getProperty(property, true);
	}

	public static boolean checkProperty(PropertyName property, String value) {
		String current_value = getProperty(property);
		String val = encodeValue(property.getType(), value);
		if (current_value.equals(val)) {
			return true;
		}
		val = encodeValueOld(property.getType(), value);
		return current_value.equals(val);
	}

	public static long getPropertyAsLong(PropertyName property) {
		try {
			return Long.parseLong(getProperty(property, true));
		} catch (Exception e) {
			return Long.parseLong(property.getDefaultValue());
		}
	}

	static int getPropertyAsInt(PropertyName property) {
		try {
			return Integer.parseInt(getProperty(property, true));
		} catch (Exception e) {
			return Integer.parseInt(property.getDefaultValue());
		}
	}

	public static boolean getPropertyAsBoolean(PropertyName property) {
		return "true".equals(getProperty(property, true));
	}

	public static String getOriginalProperty(PropertyName property) {
		return getProperty(property, false);
	}

	private static String getProperty(PropertyName property, boolean bSubstitute) {
		if (property == null) {
			throw new IllegalArgumentException("Null property key");
		}

		if (properties == null) {
			throw new IllegalStateException("Not initialized EnginePropertiesManager");
		}

		String result = system_properties.getProperty(SYSTEM_PROP_PREFIX + property);
		if (result == null) {
			result = properties.getProperty(property.key);
		}

		if (result == null) {
			result = property.getDefaultValue();
		}

		if (bSubstitute && property == PropertyName.APPLICATION_SERVER_CONVERTIGO_URL && STUDIO_APPLICATION_SERVER_CONVERTIGO_URL != null && !STUDIO_APPLICATION_SERVER_CONVERTIGO_URL.equals(result)) {
			Engine.logStudio.warn("Studio is currently listening on: " + STUDIO_APPLICATION_SERVER_CONVERTIGO_URL
					+ "\n                      instead of: " + result);
			return STUDIO_APPLICATION_SERVER_CONVERTIGO_URL;
		}

		if (result == null) {
			throw new IllegalArgumentException("Unknown property key: " + property);
		}

		// Substitute parameter value if needed
		if (bSubstitute) result = OptionConverter.substVars(result, properties);

		// Migration -> 3.1
		if (property.equals(PropertyName.DOCUMENT_XSLT_ENGINE) && (result != null) && result.equals("xalan")) result = "xalan/xsltc";

		return result;
	}

	public static String[] getOriginalPropertyAsStringArray(PropertyName property) {
		return getPropertyAsStringArray(property, false);
	}

	static String[] getPropertyAsStringArray(PropertyName property) {
		return getPropertyAsStringArray(property, true);
	}

	private static String[] getPropertyAsStringArray(PropertyName property, boolean bSubstitute) {
		if (property.getType() != PropertyType.Array) {
			throw new IllegalArgumentException("The requested property is not of type Array: " + property);
		}

		String array = getProperty(property, bSubstitute);
		StringTokenizer st = new StringTokenizer(array, ";", false);
		String[] propertyAsStringArray = new String[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens()) {
			String item = st.nextToken();
			item = item.replaceAll("\\[\\[pv\\]\\]", ";");
			propertyAsStringArray[i] = item;
			i++;
		}

		return propertyAsStringArray;
	}

	public static void setPropertyFromStringArray(PropertyName property, String[] values) {
		if (property.getType() != PropertyType.Array) {
			throw new IllegalArgumentException("The requested property is not of type Array: " + property);
		}

		String propertyAsString = "";
		for (String item : values) {
			item = item.replaceAll(";", "[[pv]]");
			propertyAsString += item + ";";
		}

		setProperty(property, propertyAsString);
	}

	public static void setProperty(PropertyName property, String value) {
		String exvalue;

		if (system_properties.containsKey(SYSTEM_PROP_PREFIX + property.getKey())) {
			exvalue = (String) system_properties.put(SYSTEM_PROP_PREFIX + property.getKey(), value);
		} else {
			if (properties == null) {
				throw new IllegalStateException("Not initialized EnginePropertiesManager");
			}

			value = encodeValue(property.getType(), value);
			exvalue = (String) properties.put(property.getKey(), value);
		}
		if (!value.equals(exvalue) && Engine.isStarted) {
			if (property == PropertyName.CONVERTIGO_PRODUCT_VERSION_CHECK) {
				if ("false".equals(value)) {
					Engine.logEngine.warn("The product version check will be ignored!");
				}
				else {
					Engine.logEngine.info("The product version check will be done!");
				}
			}
			Engine.theApp.eventManager.dispatchEvent(new PropertyChangeEvent(property, value), PropertyChangeEventListener.class);
		}
	}

	static <E extends ComboEnum> E getPropertyAsEnum(PropertyName property) {
		if (property.getType() != PropertyType.Combo) {
			throw new IllegalArgumentException("The requested property is not of type Combo: " + property);
		}
		String stringValue = getProperty(property);
		try {
			E value = GenericUtils.cast(property.getOptions().combo().getMethod("valueOf", String.class).invoke(null, stringValue));
			return value;
		} catch (Exception e) {
			throw new RuntimeException("Unable to retrieve the Enum value", e);
		}
	}

	private static void copySystemProperties(){
		if (system_properties == null) {
			Properties sys_prop = System.getProperties();
			system_properties = new Properties();
			for (String name : GenericUtils.<Collection<String>> cast(sys_prop.keySet())) {
				if (name.startsWith("convertigo.")) {
					system_properties.setProperty(name, sys_prop.getProperty(name));
				}
			}
		}
	}

	static {
		copySystemProperties();
	}

	public static synchronized void initProperties() throws EngineException {
		properties = new Properties();
	}

	public static synchronized void loadProperties() throws EngineException {
		loadProperties(true);

		if (Boolean.parseBoolean(EnginePropertiesManager.getProperty(PropertyName.SSL_DEBUG))) {
			System.setProperty("javax.net.debug", "all");
			System.out.println("(EnginePropertyManager) Enabling SSL debug mode");
		}
	}

	public static synchronized void loadProperties(boolean configureLog4J) throws EngineException {
		if (properties != null) return;

		String enginePropertiesFile = Engine.CONFIGURATION_PATH + PROPERTIES_FILE_NAME;
		try {
			EnginePropertiesManager.initProperties();

			System.out.println("Loading Convertigo engine properties from " + enginePropertiesFile);

			try {
				PropertiesUtils.load(properties, enginePropertiesFile);
			}
			catch(FileNotFoundException e) {
				String message = "Unable to find the Convertigo engine configuration file '" + enginePropertiesFile + "'. Creating a new one...";
				if (Engine.logEngine != null) Engine.logEngine.warn(message);
				else System.out.println(message);
			}

			// Decipher the ciphered properties
			for (PropertyName property : PropertyName.values()) {
				String key = property.getKey();

				// Ciphered property?
				if (property.isCiphered() && properties.containsKey(key)) {
					String value = properties.getProperty(key);

					String decipheredValue = Crypto2.decodeFromHexString3(value);
					if (decipheredValue == null) {
						String message = "Unable to decode value for property '" + key + "'";
						if (Engine.logEngine != null) Engine.logEngine.warn(message);
						else System.out.println(message);
						continue;
					}

					properties.setProperty(key, decipheredValue);
				}
			}

			// Add special properties needed for substitution
			properties.put("user.workspace", Engine.USER_WORKSPACE_PATH);
			System.out.println("  Adding user workspace path: " + Engine.USER_WORKSPACE_PATH);

			properties.put("log.directory", Engine.USER_WORKSPACE_PATH + "/logs");

			File logEngine = new File(getProperty(PropertyName.LOG4J_APPENDER_CEMSAPPENDER_FILE));

			Engine.LOG_PATH = logEngine.getParent();
			Engine.LOG_ENGINE_NAME = logEngine.getName();

			String logDirectory = Engine.LOG_PATH;
			System.out.println("  Adding log directory: " + logDirectory);

			String webappPath = Engine.WEBAPP_PATH;
			properties.put("convertigo.webapp_path", webappPath);
			System.out.println("  Adding webapp path: " + webappPath);

			System.out.println("Properties loaded!");

			if (configureLog4J) {
				configureLog4J();
			}
		}
		catch(IOException e) {
			properties = null;
			throw new EngineException("Unable to load the Convertigo engine configuration file '" + PROPERTIES_FILE_NAME + "'.", e);
		}
	}

	public static synchronized void saveProperties() throws IOException, EngineException {
		saveProperties(true);
	}

	public static synchronized void saveProperties(boolean configureLog4J) throws IOException, EngineException {
		OutputStream propsOutputStream = null;
		String enginePropertiesPath = Engine.CONFIGURATION_PATH + PROPERTIES_FILE_NAME;
		File enginePropertiesFile = new File(enginePropertiesPath);
		try {
			if (Engine.logEngine == null)
				System.out.println("Saving Convertigo engine properties to " + enginePropertiesPath);
			else
				Engine.logEngine.debug("Saving Convertigo engine properties to " + enginePropertiesPath);

			try {
				FileUtils.copyFile(enginePropertiesFile, new File(enginePropertiesPath + ".bak"));	
			} catch (Exception e) {}

			enginePropertiesFile.getParentFile().mkdirs();
			propsOutputStream = new FileOutputStream(enginePropertiesFile);

			saveProperties(propsOutputStream, "Convertigo Engine configuration file");

			if (Engine.logEngine == null) {
				System.out.println("Convertigo engine properties saved!");
			}
			else {
				Engine.logEngine.debug("Convertigo engine properties saved!");
			}

			if (configureLog4J) {
				configureLog4J();
			}
		}
		catch(IOException e) {
			//properties = null; // Why ??    Part of fix for Ticket #2072
			throw new EngineException("Unable to save the Convertigo engine configuration file '" + enginePropertiesPath + "'.", e);
		}
		finally {
			if (propsOutputStream != null) {
				propsOutputStream.flush();
				propsOutputStream.close();
			}
		}
	}

	private static void saveProperties(OutputStream outputStream, String comments) throws IOException, EngineException {
		Properties modifiedProperties = new Properties();
		for (PropertyName property : PropertyName.values()) {
			String propertyValue = getOriginalProperty(property);
			if (!property.getDefaultValue().equals(propertyValue)) {
				if (property.isCiphered()) {
					propertyValue = Crypto2.encodeToHexString(propertyValue);
				}
				modifiedProperties.put(property.getKey(), propertyValue);
			} else {
				if (!property.isCiphered() && !property.getKey().contains(".pass")) {
					outputStream.write(("#" + property.getKey() + "=" + property.getDefaultValue() + "\r\n").getBytes("UTF-8"));
				}
			}
		}
		PropertiesUtils.store(modifiedProperties, outputStream, comments);
	}

	static String getPropertiesAsString(String title, Properties propertiesToGet) {
		if (propertiesToGet == null) {
			if (properties == null) {
				throw new IllegalStateException("Not initialized EnginePropertiesManager");
			}
			propertiesToGet = properties;
		}

		List<String> vProperties = new ArrayList<String>(propertiesToGet.size());
		for (Object propKey : propertiesToGet.keySet()) {
			String propValue = propertiesToGet.getProperty((String) propKey);
			vProperties.add(propKey + "=" + propValue);
		}

		Collections.sort(vProperties);

		String msg = title + "\n";
		for (String line : vProperties) {
			msg += line + "\n";
		}

		return msg;
	}

	private static final Filter filterLog4J = new Filter() {

		@Override
		public int decide(LoggingEvent event) {
			return event.getMDC("nolog") == Boolean.TRUE ? Filter.DENY : Filter.NEUTRAL;
		}
	};

	private static void configureLog4J() {
		Properties log4jProperties = new Properties();
		for (PropertyName propertyName : PropertyName.values()) {
			String sPropertyName = propertyName.toString();
			if (sPropertyName.startsWith("log4j.")) {
				String sPropertyValue = getProperty(propertyName);
				if (propertyName == PropertyName.LOG4J_LOGGER_CEMS || propertyName == PropertyName.LOG4J_LOGGER_CEMS_CONTEXT_AUDIT) {
					if (sPropertyValue.isEmpty()) {
						sPropertyValue = LogLevels.INFO.getValue();
					}
					int id = sPropertyValue.indexOf(",");
					if (id > -1) {
						sPropertyValue = sPropertyValue.substring(0, id);
						setProperty(propertyName, sPropertyValue);
					}
					sPropertyValue += propertyName == PropertyName.LOG4J_LOGGER_CEMS ? ", CemsAppender" : ", AuditAppender";
				}
				log4jProperties.setProperty(sPropertyName, sPropertyValue);
			}
		}

		log4jProperties.put("log.directory", Engine.LOG_PATH);

		SortedSet<String> sortedKey = new TreeSet<String>(GenericUtils.<Collection<String>>cast(log4jProperties.keySet()));
		for (String key: sortedKey) {
			if (key.startsWith("log4j.logger.cems.") && "".equals(log4jProperties.get(key))) {
				String v = log4jProperties.getProperty(key.replaceAll("\\.[^.]*$", ""), "INFO");
				v = v.replaceAll(",.*", "");
				log4jProperties.put(key, v);
			}
		}
		
		if (!getPropertyAsBoolean(PropertyName.LOG_FILE_ENABLE)) {
			log4jProperties.put(PropertyName.LOG4J_LOGGER_CEMS.getKey(), getProperty(PropertyName.LOG4J_LOGGER_CEMS));
		}
		
		if (getPropertyAsBoolean(PropertyName.LOG_STDOUT_ENABLE)) {
			log4jProperties.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
			log4jProperties.put("log4j.appender.stdout.target", "System.out");
			log4jProperties.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
			log4jProperties.put("log4j.appender.stdout.layout.ConversionPattern", getProperty(PropertyName.LOG4J_APPENDER_CEMSAPPENDER_LAYOUT_CONVERSIONPATTERN));
			log4jProperties.put(PropertyName.LOG4J_LOGGER_CEMS.getKey(), log4jProperties.get(PropertyName.LOG4J_LOGGER_CEMS.getKey()) + ", stdout");
		}
		
		PropertyConfigurator.configure(log4jProperties);

		Logger cems = Logger.getLogger("cems");
		Enumeration<Appender> appenders = GenericUtils.cast(cems.getAllAppenders());
		while(appenders.hasMoreElements()) {
			Appender appender = appenders.nextElement();
			if (appender.getFilter() == null) {
				appender.addFilter(filterLog4J);
			}
		}

		if (Engine.logEngine != null) {
			Engine.logEngine.debug(getPropertiesAsString("Log4J properties:", log4jProperties));
		}
		else {
			System.out.println(getPropertiesAsString("Log4J properties:", log4jProperties));
		}
	}

	public static void unload() {
		properties = null;
	}

	static void load(String sProperties) throws IOException {
		if (properties == null) {
			throw new IllegalStateException("Not initialized EnginePropertiesManager");
		}
		PropertiesUtils.load(properties, new StringReader(sProperties));
	}

	private static String encodeValue(PropertyType propertyType, String value) {
		switch (propertyType) {
		case PasswordHash:
			value = DigestUtils.sha512Hex(value);
			break;
		default:
			break;
		}
		return value;
	}

	private static String encodeValueOld(PropertyType propertyType, String value) {
		switch (propertyType) {
		case PasswordHash:
			value = "" + value.hashCode();
			break;
		default:
			break;
		}
		return value;
	}

	public static void setStudioApplicationServerConvertigoUrl(String studioApplicationServerConvertigoUrl) {
		String cls = Thread.currentThread().getStackTrace()[2].getClassName();
		if ("com.twinsoft.convertigo.eclipse.EmbeddedTomcat".equals(cls) && STUDIO_APPLICATION_SERVER_CONVERTIGO_URL == null) {
			STUDIO_APPLICATION_SERVER_CONVERTIGO_URL = studioApplicationServerConvertigoUrl;
		}
	}
}
