/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.OptionConverter;

import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EnginePropertiesManager {
	enum Visibility { VISIBLE, HIDDEN, HIDDEN_CLOUD };

	@Retention(RetentionPolicy.RUNTIME)
	@interface CategoryOptions {
    	Visibility visibility() default Visibility.VISIBLE;
	}
    
	@Retention(RetentionPolicy.RUNTIME)
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
    public static final String PROPERTIES_FILE_NAME = "/engine.properties";
    
    public static final String SYSTEM_PROP_PREFIX = "convertigo.engine.";
    
	public interface ComboEnum {
		String getDisplay();
		String getValue();
	}
	
	public enum EmptyCombo implements ComboEnum {
		;

		public String getDisplay() {
			return null;
		}

		public String getValue() {
			return null;
		}
	}
	
	public enum LogLevels implements ComboEnum {
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
	
	public enum XmlEngine implements ComboEnum {
		java ("Java/Default"),
		msxml ("Microsoft XML");

		final String display;
		final String value;
		
		XmlEngine(String display) {
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
	
	public enum XsltEngine implements ComboEnum {
		xalan_xslt ("xalan/xslt", "Java Xalan"),
		xalan_xsltc ("xalan/xsltc", "Java Xalan (XSLTC)"),
		msxml ("Microsoft XML");

		final String display;
		final String value;
		
		XsltEngine(String value, String display) {
			this.display = display;
			this.value = value;
		}
		
		XsltEngine(String display) {
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
	
	public enum SecurityTokenMode implements ComboEnum {
		memory ("memory"),
		database ("database");
		
		final String display;
		final String value;
		
		SecurityTokenMode(String display) {
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
    	Main ("Main parameters"),
    	Account ("Accounts"),
    	Logs ("Logs"),
    	Context ("Real-time activity monitoring"),
    	XmlGeneration ("XML generation"),
    	XulRunner ("HTML parser"),
    	HttpClient ("HTTP client"),
    	Network ("Network"),
    	Proxy ("Proxy"),
    	SecurityToken ("Security token"),
    	Ssl ("SSL"),
    	Cache ("Cache"),
    	@CategoryOptions(visibility = Visibility.HIDDEN_CLOUD)
    	Carioca ("Legacy Carioca portal"),
    	@CategoryOptions(visibility = Visibility.HIDDEN_CLOUD)
    	Billing ("Supervision"),
    	Notifications ("Notifications"),
    	MobileBuilder ("Mobile builder"),
    	@CategoryOptions(visibility = Visibility.HIDDEN)
    	ExternalBrowser ("External browser")
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
				if (Engine.isCloudMode()) {
					return visibility == Visibility.VISIBLE;
				}
				else {
					return visibility == Visibility.VISIBLE || visibility == Visibility.HIDDEN_CLOUD;
				}
			}
			return true;
		}
    };
	
	@PropertyOptions
    public enum PropertyName {
		/** MAIN */
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		APPLICATION_SERVER_CONVERTIGO_URL ("application_server.convertigo.url", "http://localhost:18080/convertigo", "Convertigo Server application URL", PropertyCategory.Main),
		@PropertyOptions(visibility = Visibility.HIDDEN)
		APPLICATION_SERVER_MASHUP_URL ("application_server.mashup.url", "http://localhost:18080/convertigo", "Mashup composer server base URL", PropertyCategory.Main),
		DOCUMENT_THREADING_MAX_WORKER_THREADS ("document.threading.max_worker_threads", "100", "Maximum number of worker threads", PropertyCategory.Main),
		CONVERTIGO_MAX_CONTEXTS ("convertigo.max_context", "750", "Maximum number of contexts", PropertyCategory.Main),

		/** MAIN ADVANCE */
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		CONVERTIGO_PRODUCT_VERSION_CHECK ("convertigo.product_version_check", "true", "Product version check", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		DOCUMENT_THREADING_USE_STOP_METHOD ("document.threading.use_stop_method", "false", "Use the Java Thread.stop() method in order to finish threads", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		LINUX_LAUNCH_XVNC ("linux.xvnc.launch", "true", "(Linux only) Launch Xvnc server using DISPLAY environment variable at startup", PropertyCategory.Main),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LINUX_XVNC_DEPTH ("linux.xvnc.depth", "16", "(Linux only) Depth parameter for the Xvnc, default is 16", PropertyCategory.Main),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LINUX_XVNC_GEOMETRY ("linux.xvnc.geometry", "320x240", "(Linux only) Geometry parameter for Xvnc, default is 320x240", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN)
		MIGRATION_3_0_0 ("migration.3.0.0", "false", "Migration 3.0.0", PropertyCategory.Main),
		@PropertyOptions(advance = true)
		POOL_MANAGER_TIMEOUT ("pool.manager.timeout", "-1", "Time allowed for pool management task in seconds (-1 for disable)", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN_CLOUD)
		PROJECTS_DATA_COMPATIBILITY_MODE ("projects_data.compatibility_mode", "false", "Enable the compatibility mode for projects data (required for JSP usage); engine restart required", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SEQUENCE_STEPS_USE_SAME_JSESSION ("sequence.steps.use_same_jsession", "true", "Use same JSESSIONID for sequences and steps", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		SOAP_REQUEST_ADD_XML_ENCODING_CHARSET ("soap.request.add_xml_encoding_charset", "false", "Add XML encoding charset for SOAP requests", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		THROW_HTTP_500 ("throw_http_500", "true", "Throw HTTP 500 in case of unrecoverable servlet error", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		HIDING_ERROR_INFORMATION ("hiding_error_information", "false", "Hide detailed information in case of unrecoverable servlet error", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		THROW_HTTP_500_SOAP_FAULT ("throw_http_500.soap_fault", "true", "Throw HTTP 500 in case of SOAP fault", PropertyCategory.Main),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean, visibility = Visibility.HIDDEN)
		UPDATE_STEPS ("update.steps", "false", "Update steps", PropertyCategory.Main),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		CRYPTO_PASSPHRASE ("crypto.passphrase", "A8dkLmsdfkKze0e34FGh", "Cryptographic services passphrase", PropertyCategory.Main),
		
		/** ACCOUNTS */
		ADMIN_USERNAME ("admin.username", "admin", "Admin username", PropertyCategory.Account),
		@PropertyOptions(propertyType = PropertyType.PasswordHash)
		ADMIN_PASSWORD ("admin.password", ""+"admin".hashCode(), "Admin password", PropertyCategory.Account),
		TEST_PLATFORM_USERNAME ("testplatform.username", "", "Test Platform username (leave it blank for anonymous access)", PropertyCategory.Account),
		@PropertyOptions(propertyType = PropertyType.PasswordHash)
		TEST_PLATFORM_PASSWORD ("testplatform.password", ""+"".hashCode(), "Test Platform password", PropertyCategory.Account),
		
		/** LOGS */
		LOG4J_LOGGER_CEMS ("log4j.logger.cems", LogLevels.INFO.getValue() + ", CemsAppender", "Log4J root logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_ADMIN ("log4j.logger.cems.Admin", LogLevels.WARN.getValue(), "Log4J admin logger", PropertyCategory.Logs),
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
		LOG4J_LOGGER_CEMS_CONTEXT_USER ("log4j.logger.cems.Context.User", LogLevels.INHERITED.getValue(), "Log4J user context logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_CONTEXTMANAGER ("log4j.logger.cems.ContextManager", LogLevels.WARN.getValue(), "Log4J context manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_DATABASEOBJECTMANAGER ("log4j.logger.cems.DatabaseObjectManager", LogLevels.WARN.getValue(), "Log4J database objects manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_EMULATORS ("log4j.logger.cems.Emulators", LogLevels.INFO.getValue(), "Log4J emulators output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_ENGINE ("log4j.logger.cems.Engine", LogLevels.INHERITED.getValue(), "Log4J engine logger", PropertyCategory.Logs),
		/** #3437 : Disabled ExternalBrowser feature because it's not terminated
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_EXTERNALBROWSER ("log4j.logger.cems.ExternalBrowser", LogLevels.INFO.getValue(), "Log4J external browser output logger", PropertyCategory.Logs),
		*/
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_JOBMANAGER ("log4j.logger.cems.JobManager", LogLevels.WARN.getValue(), "Log4J job manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_PROXYMANAGER ("log4j.logger.cems.ProxyManager", LogLevels.INFO.getValue(), "Log4J proxy manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_SCHEDULER ("log4j.logger.cems.Scheduler", LogLevels.INFO.getValue(), "Log4J scheduler output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_SITECLIPPER ("log4j.logger.cems.SiteClipper", LogLevels.INFO.getValue(), "Log4J site clipper output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_STATISTICS ("log4j.logger.cems.Statistics", LogLevels.WARN.getValue(), "Log4J statistics logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_TRACEPLAYERMANAGER ("log4j.logger.cems.TracePlayerManager", LogLevels.WARN.getValue(), "Log4J trace player manager logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_USAGEMONITOR ("log4j.logger.cems.UsageMonitor", LogLevels.WARN.getValue(), "Log4J usage monitor logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Combo, combo = LogLevels.class)
		LOG4J_LOGGER_CEMS_USER ("log4j.logger.cems.User", LogLevels.INFO.getValue(), "Log4J user output logger", PropertyCategory.Logs),
		@PropertyOptions(propertyType = PropertyType.Text)
		LOG4J_LOGGER_CEMS_CONTEXT_AUDIT ("log4j.logger.cems.Context.Audit", LogLevels.INFO.getValue() + ", AuditAppender", "Log4J audit context logger", PropertyCategory.Logs),
		
		
		/** LOGS ADVANCE */
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG_EXPLICIT_VARIABLES ("log.explicit_variables", "contextid,project,sequence,connector,transaction,user,clientip,clienthostname", "Explicit variables", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG_START_OF_LINE_CHARACTER ("log.start_of_line_character", "!", "Start-of-line character", PropertyCategory.Logs),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		LOG4J_ADDITIVITY_CEMS ("log4j.additivity.cems", "false", "Log4J root logger additivity", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_CEMSAPPENDER ("log4j.appender.CemsAppender", "org.apache.log4j.RollingFileAppender", "Log4J default appender", PropertyCategory.Logs),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_CEMSAPPENDER_FILE ("log4j.appender.CemsAppender.File","${log.directory}/engine.log", "Log4J default appender file", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_CEMSAPPENDER_LAYOUT ("log4j.appender.CemsAppender.layout","org.apache.log4j.PatternLayout", "Log4J default appender layout", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG4J_APPENDER_CEMSAPPENDER_LAYOUT_CONVERSIONPATTERN ("log4j.appender.CemsAppender.layout.ConversionPattern", "!%-28c{1} | %d | %-5p | %-32t | %X{ContextualParameters}%m%n", "Log4J default appender layout conversion pattern", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_CEMSAPPENDER_MAXFILESIZE ("log4j.appender.CemsAppender.MaxFileSize", "10MB", "Log4J default appender max file size", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_CEMSAPPENDER_MAXBACKUPINDEX ("log4j.appender.CemsAppender.MaxBackupIndex", "100", "Log4J default appender max backup index", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_CEMSAPPENDER_ENCODING ("log4j.appender.CemsAppender.Encoding","UTF-8", "Log4J default appender encoding (requires JVM restart)", PropertyCategory.Logs),
		// New appender for the Audit logger.
		LOG4J_APPENDER_AUDITAPPENDER ("log4j.appender.AuditAppender", "org.apache.log4j.RollingFileAppender", "Log4J audit appender", PropertyCategory.Logs),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_AUDITAPPENDER_FILE ("log4j.appender.AuditAppender.File","${log.directory}/audit.log", "Log4J audit appender file", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		LOG4J_APPENDER_AUDITAPPENDER_LAYOUT ("log4j.appender.AuditAppender.layout","org.apache.log4j.PatternLayout", "Log4J audit appender layout", PropertyCategory.Logs),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN)
		LOG4J_APPENDER_AUDITAPPENDER_LAYOUT_CONVERSIONPATTERN ("log4j.appender.AuditAppender.layout.ConversionPattern", "!%c{1} | %d | %-5p | %m%n", "Log4J audit appender layout conversion pattern", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_AUDITAPPENDER_MAXFILESIZE ("log4j.appender.AuditAppender.MaxFileSize", "10MB", "Log4J audit appender max file size", PropertyCategory.Logs),
		@PropertyOptions(advance = true)
		LOG4J_APPENDER_AUDITAPPENDER_MAXBACKUPINDEX ("log4j.appender.AuditAppender.MaxBackupIndex", "100", "Log4J audit appender max backup index", PropertyCategory.Logs),
		

		/** NETWORK */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		NET_REVERSE_DNS ("net.reverse_dns", "false", "Use DNS reverse search for finding host names", PropertyCategory.Network),
		FILE_UPLOAD_MAX_REQUEST_SIZE ("net.upload.max_request_size", "-1", "Maximum allowed size of a complete multipart request (in bytes). Value -1 indicates no limit.", PropertyCategory.Network),
		FILE_UPLOAD_MAX_FILE_SIZE ("net.upload.max_request_size", "10485760", "Maximum allowed size of a single uploaded file (in bytes).", PropertyCategory.Network),
		
		/** HTTPCLIENT */
		HTTP_CLIENT_MAX_TOTAL_CONNECTIONS ("http_client.max_total_connections", "100", "Maximal number of HTTP connections (from 1 to 65535)", PropertyCategory.HttpClient),
		HTTP_CLIENT_MAX_CONNECTIONS_PER_HOST ("http_client.max_connections_per_host", "50", "Maximal number of HTTP connections per host (from 1 to 255)", PropertyCategory.HttpClient),

		/** CONNECTORS MONITORING */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		CONNECTORS_MONITORING ("connectors.monitoring", "false", "Display running connectors in monitor of Legacy connectors", PropertyCategory.Context),
		@PropertyOptions(propertyType = PropertyType.Boolean)
		DOCUMENT_LOG_SCREEN_DUMPS ("document.log.screen_dumps", "false", "Trace in logs the screen dumps of the running Legacy connectors", PropertyCategory.Context),

		/** XML GENERATION */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		DOCUMENT_INCLUDE_STATISTICS ("document.include_statistics", "false", "Insert statistics in the generated document", PropertyCategory.XmlGeneration),

		/** XML GENERATION ADVANCE */
		@PropertyOptions(advance = true, propertyType = PropertyType.Combo, combo = XmlEngine.class)
		DOCUMENT_XML_ENGINE ("document.xml_engine", XmlEngine.java.getValue(), "XML engine", PropertyCategory.XmlGeneration),
		@PropertyOptions(advance = true, propertyType = PropertyType.Combo, combo = XsltEngine.class)
		DOCUMENT_XSLT_ENGINE ("document.xslt_engine", XsltEngine.xalan_xsltc.getValue(), "XSLT engine", PropertyCategory.XmlGeneration),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		DOCUMENT_NAMESPACE_AWARE("document.namespace.aware", "false", "Set namespace aware", PropertyCategory.XmlGeneration),
		
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

		/** XULRUNNER */
		XULRUNNER_MAX_CONNECTIONS ("xulrunner.max-connections", "65535", "Max connections (from 1 to 65535)", PropertyCategory.XulRunner),
		XULRUNNER_MAX_CONNECTIONS_PER_SERVER ("xulrunner.max-connections-per-server", "255", "Max connections per server (from 1 to 255)", PropertyCategory.XulRunner),
		XULRUNNER_MAX_PERSISTENT_CONNECTIONS_PER_SERVER ("xulrunner.max-persistent-connections-per-server", "10", "Max persistent connections per server (from 1 to 10)", PropertyCategory.XulRunner),
		XULRUNNER_USERAGENT ("xulrunner.useragent", "", "Override User-Agent", PropertyCategory.XulRunner),
		
		/** XULRUNNER ADVANCE */
		@PropertyOptions(advance = true)
		XULRUNNER_ACCEPT_LANGUAGES ("xulrunner.accept_languages", "", "Override Accept-Language header", PropertyCategory.XulRunner),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		XULRUNNER_ALLOW_IMAGE ("xulrunner.allow.image", "true", "Allow image", PropertyCategory.XulRunner),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		XULRUNNER_ALLOW_PLUGIN ("xulrunner.allow.plugin", "false", "Allow plugin", PropertyCategory.XulRunner),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		XULRUNNER_CHECK_CACHE ("xulrunner.check-cache", "true", "Check cache validity (false could increase latency)", PropertyCategory.XulRunner),
		@PropertyOptions(advance = true, propertyType = PropertyType.Boolean)
		XULRUNNER_INTERRUPT_PARSING ("xulrunner.interrupt-parsing", "false", "Enable screen rendering during parse (should be disabled)", PropertyCategory.XulRunner),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		XULRUNNER_URL ("xulrunner.url", "${convertigo.webapp_path}/WEB-INF/xulrunner", "XulRunner path", PropertyCategory.XulRunner),
		@PropertyOptions(advance = true, visibility = Visibility.HIDDEN_CLOUD)
		XULRUNNER_WORK ("xulrunner.work", "${user.workspace}/xulrunner-work", "XulRunner work directory", PropertyCategory.XulRunner),

		/** SECURITY TOKEN */
		SECURITY_TOKEN_LIFE_TIME ("security_token.life_time", "20", "Security tokens lifetime (in seconds)", PropertyCategory.SecurityToken),
		@PropertyOptions(propertyType = PropertyType.PasswordHash)
		SECURITY_TOKEN_PASSWORD ("security_token.password", ""+"c8o-password".hashCode(), "Security token generator password", PropertyCategory.SecurityToken),
		@PropertyOptions(advance = true, propertyType = PropertyType.Combo, combo = SecurityTokenMode.class)
		SECURITY_TOKEN_MODE ("security_token.mode", SecurityTokenMode.memory.getValue(), "Storage Mode", PropertyCategory.SecurityToken),
		@PropertyOptions(advance = true)
		SECURITY_TOKEN_PERSISTENCE_DIALECT ("security_token.persistence.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect", "Persistence Dialect", PropertyCategory.SecurityToken),
		@PropertyOptions(advance = true)
		SECURITY_TOKEN_PERSISTENCE_JDBC_DRIVER ("security_token.persistence.jdbc.driver", "org.mariadb.jdbc.Driver", "JDBC Driver", PropertyCategory.SecurityToken),
		@PropertyOptions(advance = true)
		SECURITY_TOKEN_PERSISTENCE_JDBC_URL ("security_token.persistence.jdbc.url", "jdbc:mysql://localhost:3306/c8oSecurityToken", "JDBC URL", PropertyCategory.SecurityToken),
		@PropertyOptions(advance = true)
		SECURITY_TOKEN_PERSISTENCE_JDBC_USERNAME ("security_token.persistence.jdbc.username", "", "JDBC Username", PropertyCategory.SecurityToken),
		@PropertyOptions(advance = true, propertyType = PropertyType.PasswordPlain, ciphered = true)
		SECURITY_TOKEN_PERSISTENCE_JDBC_PASSWORD ("security_token.persistence.jdbc.password", "", "JDBC Password", PropertyCategory.SecurityToken),
		
		/** SSL */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		SSL_DEBUG ("ssl.debug", "false", "SSL debug output (requires JVM restart); only available for HTTP connectors (i.e. NOT useful for HTML connectors)", PropertyCategory.Ssl),
		@PropertyOptions(propertyType = PropertyType.Array)
		SSL_ISSUERS ("ssl.issuers", "", "SSL issuers", PropertyCategory.Ssl),

		/** CACHE */
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		CACHE_MANAGER_CLASS ("cache_manager.class", "com.twinsoft.convertigo.engine.cache.FileCacheManager", "Cache manager class", PropertyCategory.Cache),
		@PropertyOptions(visibility = Visibility.HIDDEN_CLOUD)
		CACHE_MANAGER_FILECACHE_DIRECTORY ("cache_manager.filecache.directory", "${user.workspace}/cache", "File cache directory", PropertyCategory.Cache),
		CACHE_MANAGER_SCAN_DELAY ("cache_manager.scan_delay", "60", "Cache scan delay (in seconds)", PropertyCategory.Cache),
		@PropertyOptions(advance = false, propertyType = PropertyType.Boolean)
		DISABLE_CACHE ("disable.cache", "false", "Disable Cache", PropertyCategory.Cache),
		
		/** CARIOCA */
		CARIOCA_DEFAULT_USER_NAME ("carioca.default.user.name", "admin", "Default user name", PropertyCategory.Carioca),
		CARIOCA_DEFAULT_USER_PASSWORD ("carioca.default.user.password", "admin", "Default user password", PropertyCategory.Carioca),
		CARIOCA_SESSION_KEY_LIFE_TIME ("carioca.session_key.life_time", "60", "Default session key life time (in seconds)", PropertyCategory.Carioca),
		CARIOCA_URL ("carioca.url", "${user.workspace}/minime", "Carioca access URL", PropertyCategory.Carioca),

		/** SUPERVISION */
		@PropertyOptions(propertyType = PropertyType.Boolean)
		BILLING_ENABLED ("billing.enabled", "false", "Enable supervision", PropertyCategory.Billing),

		/** SUPERVISION ADVANCE */
		@PropertyOptions(advance = true)
		BILLING_PERSISTENCE_DIALECT ("billing.persistence.dialect", "org.hibernate.dialect.HSQLDialect", "Persistence Dialect", PropertyCategory.Billing),
		@PropertyOptions(advance = true)
		BILLING_PERSISTENCE_JDBC_DRIVER ("billing.persistence.jdbc.driver", "org.hsqldb.jdbcDriver", "JDBC Driver", PropertyCategory.Billing),
		@PropertyOptions(advance = true, propertyType = PropertyType.PasswordPlain, ciphered = true)
		BILLING_PERSISTENCE_JDBC_PASSWORD ("billing.persistence.jdbc.password", "", "JDBC Password", PropertyCategory.Billing),
		@PropertyOptions(advance = true)
		BILLING_PERSISTENCE_JDBC_URL ("billing.persistence.jdbc.url", "jdbc:hsqldb:file:${user.workspace}/databases/hsqldb_billing", "JDBC URL", PropertyCategory.Billing),
		@PropertyOptions(advance = true)
		BILLING_PERSISTENCE_JDBC_USERNAME ("billing.persistence.jdbc.username", "sa", "JDBC Username", PropertyCategory.Billing),

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
		//@PropertyOptions(propertyType = PropertyType.Boolean)
		//MOBILE_BUILDER_ ("mobile.builder.", "false", "Don't use the default Phonegap account", PropertyCategory.MobileBuilder),
		
		MOBILE_BUILDER_USERNAME ("mobile.builder.username", "", "Mobile builder username", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_PASSWORD ("mobile.builder.password", "", "Mobile builder password", PropertyCategory.MobileBuilder),
		//iOS
		MOBILE_BUILDER_IOS_CERTIFICATE_TITLE ("mobile.builder.ios_certificate_title", "", "iOS certificate title", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_IOS_CERTIFICATE_PW ("mobile.builder.ios_certificate_pw", "", "iOS certificate password", PropertyCategory.MobileBuilder),
		
		//WINDOWSPHONE7
		
		//ANDROID
		MOBILE_BUILDER_ANDROID_CERTIFICATE_TITLE ("mobile.builder.android_certificate_title", "", "Android certificate title", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_ANDROID_CERTIFICATE_PW ("mobile.builder.android_certificate_pw", "", "Android certificate password", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_ANDROID_KEYSTORE_PW ("mobile.builder.android_keystore_pw", "", "Android keyStore password", PropertyCategory.MobileBuilder),
		
		//BLACKBERRY
		MOBILE_BUILDER_BB_KEY_TITLE ("mobile.builder.bb_key_title", "", "BlackBerry key title", PropertyCategory.MobileBuilder),
		@PropertyOptions(propertyType = PropertyType.PasswordPlain, ciphered = true)
		MOBILE_BUILDER_BB_KEY_PW ("mobile.builder.bb_key_pw", "", "BlackBerry key password", PropertyCategory.MobileBuilder),
		
		@PropertyOptions(advance = true)
		MOBILE_BUILDER_PLATFORM_URL ("mobile.builder.platform_url", "https://build.convertigo.net/cmb/PhoneGapBuilder", "Mobile builder platform URL", PropertyCategory.MobileBuilder),
		
		
		/** EXTERNAL BROWSER */
		@PropertyOptions(propertyType = PropertyType.Array)
		EBA_LIST ("externalbrowser.agent.list", "", "External Browser Agent list", PropertyCategory.ExternalBrowser),
		@PropertyOptions(propertyType = PropertyType.Array)
		EBM_POOL ("externalbrowser.manager.pool", "", "External Browser Manager pool", PropertyCategory.ExternalBrowser),
		
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
				if (Engine.isCloudMode()) {
					return visibility == Visibility.VISIBLE;
				}
				else {
					return visibility == Visibility.VISIBLE || visibility == Visibility.HIDDEN_CLOUD;
				}
			}
			return true;
		}
    };
    
    private static Properties properties;
    public static Properties system_properties = null;
    
    /**
     * Store the servlet path, all paths starting with '.' are relative to this path.
     */
    public static String servletPath;
    
    public static synchronized String getProperty(PropertyName property) {
    	return getProperty(property, true);
    }
    
    public static boolean checkProperty(PropertyName property, String value) {
    	String current_value = getProperty(property);
    	value = encodeValue(property.getType(), value);
    	return current_value.equals(value);
    }
    
    public static synchronized long getPropertyAsLong(PropertyName property) {
    	try {
    		return Long.parseLong(getProperty(property, true));
    	} catch (Exception e) {
    		return Long.parseLong(property.getDefaultValue());
    	}
    }
    
    public static synchronized boolean getPropertyAsBoolean(PropertyName property) {
    	return "true".equals(getProperty(property, true));
    }
    
    public static synchronized String getOriginalProperty(PropertyName property) {
    	return getProperty(property, false);
    }
    
    public static synchronized String getProperty(PropertyName property, boolean bSubstitute) {
        if (property == null) {
        	throw new IllegalArgumentException("Null property key");
        }

    	String result = system_properties.getProperty(SYSTEM_PROP_PREFIX + property);
    	if (result == null) {
    		result = properties.getProperty(property.key);
    	}
    	
        if (result == null) {
        	result = property.getDefaultValue();
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
    
    public static synchronized String[] getOriginalPropertyAsStringArray(PropertyName property) {
    	return getPropertyAsStringArray(property, false);
    }
    
    public static synchronized String[] getPropertyAsStringArray(PropertyName property) {
    	return getPropertyAsStringArray(property, true);
    }
    
	public static String[] getPropertyAsStringArray(PropertyName property, boolean bSubstitute) {
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
    		value = encodeValue(property.getType(), value);
    		exvalue = (String) properties.put(property.getKey(), value);
    	}
    	if (!value.equals(exvalue) && Engine.isStarted) {
    		Engine.theApp.eventManager.dispatchEvent(new PropertyChangeEvent(property, value), PropertyChangeEventListener.class);
    	}
    }

	public static <E extends ComboEnum> E getPropertyAsEnum(PropertyName property) {
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
    
	public static void copySystemProperties(){
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
    }
    
    public static synchronized void loadProperties(boolean configureLog4J) throws EngineException {
		if (properties != null) return;

		FileInputStream propsInputStream = null;
		String enginePropertiesFile = Engine.CONFIGURATION_PATH + PROPERTIES_FILE_NAME;
        try {
        	EnginePropertiesManager.initProperties();

    		System.out.println("Loading Convertigo engine properties from " + enginePropertiesFile);

    		try {
        		propsInputStream = new FileInputStream(enginePropertiesFile);
    			properties.load(propsInputStream);
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
			
			Engine.LOG_PATH = Engine.USER_WORKSPACE_PATH + "/logs";

			String logDirectory = Engine.LOG_PATH;
			properties.put("log.directory", logDirectory);
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
        finally {
        	if (propsInputStream != null) {
        		try {
					propsInputStream.close();
				} catch (IOException e) {
		            // Silently ignore
					System.out.println("Unable to close the configuration properties file");
					e.printStackTrace();
				}
        	}
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

    public static void saveProperties(OutputStream outputStream, String comments) throws IOException, EngineException {
    	Properties modifiedProperties = new Properties();
    	for (PropertyName property : PropertyName.values()) {
    		String propertyValue = getOriginalProperty(property);
    		if (!property.getDefaultValue().equals(propertyValue)) {
    			if (property.isCiphered()) {
    				propertyValue = Crypto2.encodeToHexString(propertyValue);
    			}
    			modifiedProperties.put(property.getKey(), propertyValue);
    		}
    	}
    	modifiedProperties.store(outputStream, comments);
    }

    public static String getPropertiesAsString(String title, Properties propertiesToGet) {
    	if (propertiesToGet == null) {
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

    public static void configureLog4J() {
		Properties log4jProperties = new Properties();
		for (PropertyName propertyName : PropertyName.values()) {
			String sPropertyName = propertyName.toString();
			if (sPropertyName.startsWith("log4j."))
				log4jProperties.setProperty(sPropertyName, getProperty(propertyName));
		}
		
		log4jProperties.put("log.directory", Engine.LOG_PATH);
		
		LogManager.resetConfiguration();
	    PropertyConfigurator.configure(log4jProperties);
	    
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

	public static void load(ByteArrayInputStream byteArrayInputStream) throws IOException {
		properties.load(byteArrayInputStream);		
	}
	
	private static String encodeValue(PropertyType propertyType, String value) {
		switch (propertyType) {
		case PasswordHash:
			value = "" + value.hashCode();
			break;
		default:
			break;
		}
		return value;
	}
}
