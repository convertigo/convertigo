import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

/**
 * Serves an exploded Convertigo war on a port, in an embedded Tomcat: the engine of the Electron
 * Studio, which main.js starts with the Java runtime of the application.
 * Usage: DevEngine <port> <exploded war dir> <tomcat base dir>
 */
public class DevEngine {
	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		String war = new File(args[1]).getAbsolutePath();
		String base = new File(args[2]).getAbsolutePath();
		new File(base, "webapps").mkdirs();
		Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(base);
		tomcat.setAddDefaultWebXmlToWebapp(false);
		tomcat.enableNaming();
		Connector connector = new Connector();
		connector.setPort(port);
		// as the Eclipse Studio: the development server of a Flow frontend serves its sources by their path, with
		// the brackets of the SvelteKit route directories (/src/routes/product/[id]/+page.svelte)
		connector.setProperty("relaxedPathChars", "[]");
		tomcat.getService().addConnector(connector);
		tomcat.setConnector(connector);
		Context context = tomcat.addWebapp("/convertigo", war);
		context.setParentClassLoader(DevEngine.class.getClassLoader());
		// as the Eclipse Studio: the context of the workspace, with the JNDI resources of its SQL connectors
		String workspace = System.getProperty("convertigo.cems.user_workspace_path");
		File config = workspace == null ? null : new File(workspace, "studio/context.xml");
		if (config != null && config.isFile()) {
			String text = Files.readString(config.toPath(), StandardCharsets.UTF_8);
			String fixed = !text.contains("<CookieProcessor")
					? text.replace("</Context>", "\t<CookieProcessor sameSiteCookies=\"unset\" />\n</Context>")
					: text.replace(" sameSiteCookies=\"\"", " sameSiteCookies=\"unset\"");
			if (!fixed.equals(text)) {
				Files.writeString(config.toPath(), fixed, StandardCharsets.UTF_8);
			}
			context.setConfigFile(config.toURI().toURL());
		}
		// a graceful stop saves the sessions, which the next start restores
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				tomcat.stop();
				tomcat.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
		// the launcher stops the engine with the shutdown command of its Tomcat on a local port, which Windows,
		// without SIGTERM, needs to stop it gracefully: the command is the token only the launcher knows
		String shutdownPort = System.getenv("CONVERTIGO_STUDIO_SHUTDOWN_PORT");
		String token = System.getenv("CONVERTIGO_STUDIO_AUTH_TOKEN");
		if (shutdownPort != null && token != null && !token.isEmpty()) {
			tomcat.getServer().setAddress("127.0.0.1");
			tomcat.getServer().setPort(Integer.parseInt(shutdownPort));
			tomcat.getServer().setShutdown(token);
		}
		tomcat.start();
		System.out.println("DevEngine ready on http://localhost:" + port + "/convertigo/");
		tomcat.getServer().await();
		// the shutdown command: the hooks stop Tomcat and the engine, as on SIGTERM
		System.exit(0);
	}
}
