import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

/**
 * Serves an exploded Convertigo war on a port, in an embedded Tomcat: the engine of the desktop
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
		tomcat.getService().addConnector(connector);
		tomcat.setConnector(connector);
		Context context = tomcat.addWebapp("/convertigo", war);
		context.setParentClassLoader(DevEngine.class.getClassLoader());
		// a graceful stop saves the sessions, which the next start restores
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				tomcat.stop();
				tomcat.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
		tomcat.start();
		System.out.println("DevEngine ready on http://localhost:" + port + "/convertigo/");
		tomcat.getServer().await();
	}
}
