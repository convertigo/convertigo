package com.twinsoft.convertigo.beans.connectors;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.enums.HttpPool;

public class HttpConnectorNetworkErrorTest {
	private Engine previous;

	@Before
	public void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Logger.getLogger("http-network-error-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
	}

	@After
	public void restore() {
		Engine.theApp = previous;
	}

	private int execute(IOException failure, boolean running) throws Exception {
		return (Integer) execute(failure, running, false, new GetMethod("https://service.example/metadata"));
	}

	private Object execute(IOException failure, boolean running, boolean httpInfo, GetMethod method) throws Exception {
		return execute(failure, running, httpInfo, method, false);
	}

	private Object execute(IOException failure, boolean running, boolean httpInfo, GetMethod method, boolean cancelDuringCall) throws Exception {
		var requestable = new HttpTransaction[1];
		HttpClient client = new HttpClient() {
			@Override
			public int executeMethod(HostConfiguration host, HttpMethod method, HttpState state) throws IOException {
				if (cancelDuringCall) requestable[0].runningThread.bContinue = false;
				if (failure != null) {
					throw failure;
				}
				return 200;
			}
		};
		Context context = new Context("network-error-test") {
			@Override
			public HttpClient getHttpClient3(HttpPool pool) {
				return client;
			}
		};
		context.project = new Project() {
			@Override
			public ClassLoader getProjectClassLoader() {
				return getClass().getClassLoader();
			}
		};
		HttpTransaction transaction = new HttpTransaction();
		requestable[0] = transaction;
		transaction.context = context;
		transaction.setResponseTimeout(7);
		transaction.setHttpInfo(httpInfo);
		context.outputDocument = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		context.outputDocument.appendChild(context.outputDocument.createElement("document"));
		transaction.runningThread = transaction.new RequestableThread();
		transaction.runningThread.bContinue = running;
		context.transaction = transaction;
		context.requestedObject = transaction;
		HttpConnector connector = new HttpConnector();
		connector.handleCookie = false;
		connector.sUrl = "https://service.example/metadata";
		var execute = HttpConnector.class.getDeclaredMethod(httpInfo ? "executeMethod" : "doExecuteMethod", HttpMethod.class, Context.class);
		execute.setAccessible(true);
		try {
			return execute.invoke(connector, method, context);
		} catch (InvocationTargetException e) {
			throw (Exception) e.getCause();
		}
	}

	@Test
	public void preservesDnsConnectionTlsAndOtherIoFailures() throws Exception {
		for (IOException cause : new IOException[] {new UnknownHostException("unavailable.example"),
				new ConnectException("Connection refused"), new SSLHandshakeException("Untrusted test certificate"),
				new IOException("Connection reset")}) {
			ConnectionException error = assertThrows(ConnectionException.class, () -> execute(cause, true));
			assertSame(cause, error.getCause());
			assertTrue(error.getMessage().contains("https://service.example/metadata"));
		}
	}

	@Test
	public void preservesTimeoutCauseAndExistingTimeoutMessage() throws Exception {
		var cause = new SocketTimeoutException("test read timeout");
		ConnectionException error = assertThrows(ConnectionException.class, () -> execute(cause, true));
		assertSame(cause, error.getCause());
		assertEquals("Timeout reached (7 sec)", error.getMessage());
	}

	@Test
	public void keepsCancellationBehavior() throws Exception {
		assertEquals(-1, execute(new IOException("cancelled"), false));
	}

	@Test
	public void httpInfoPreservesTheNetworkFailureAndReleasesTheConnection() throws Exception {
		var cause = new ConnectException("Connection timed out: connect");
		var released = new java.util.concurrent.atomic.AtomicBoolean();
		var method = new GetMethod("https://service.example/metadata") {
			@Override public void releaseConnection() { released.set(true); super.releaseConnection(); }
		};
		assertNull(method.getStatusLine());
		var error = assertThrows(ConnectionException.class, () -> execute(cause, true, true, method));
		assertSame(cause, error.getCause());
		assertTrue(released.get());
	}

	@Test
	public void httpInfoDoesNotReadAMissingStatusAfterCancellation() throws Exception {
		// Cancellation happens while the client is executing, after executeMethod's initial guard.
		var method = new GetMethod("https://service.example/metadata");
		assertNull(execute(new IOException("cancelled"), true, true, method, true));
	}

	@Test
	public void returnsSuccessfulStatus() throws Exception {
		assertEquals(200, execute(null, true));
	}
}
