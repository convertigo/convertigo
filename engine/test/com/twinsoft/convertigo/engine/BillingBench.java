/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.GoogleAnalyticsTicketManager;
import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.sessions.RequestScopedHttpSession;

/**
 * Micro benchmark of the billing tickets, from the request threads to the ticket managers. Not a test: run it with
 * {@code ./gradlew :engine:billingBench -PbillingBenchArgs="<scenario> key=value..."}.
 * <ul>
 * <li>{@code request threads=1,5,20 manager=none|sink|slow seconds=3 warmup=2 work=0}: cost of a ticket for the
 * request threads (time, allocations, lock contention), the ticket manager doing nothing ({@code sink}) or taking
 * {@code slowMicros} per ticket ({@code slow}); {@code work} spins that many microseconds between two tickets, as a
 * requestable would, {@code idle} waits that many microseconds, as for a back end;</li>
 * <li>{@code drain manager=sink|db|ga tickets=20000 threads=4}: time for the ticket managers to write the tickets the
 * requests produce: {@code db} is the JDBC ticket manager on {@code jdbcUrl}/{@code jdbcUser}/{@code jdbcPassword}
 * (environment {@code BILLING_BENCH_JDBC_PASSWORD}), {@code ga} the HTTP ticket manager posting to a local collector
 * answering in {@code latencyMs};</li>
 * <li>{@code shutdown tickets=20000 sinkMicros=50}: tickets still written when the engine stops right after them.</li>
 * </ul>
 * The ticket manager posting events is only ever pointed at the local collector of this benchmark.
 */
public class BillingBench {
	private static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory
			.getThreadMXBean();

	public static void main(String[] args) throws Exception {
		var options = new HashMap<String, String>();
		for (int i = 1; i < args.length; i++) {
			var kv = args[i].split("=", 2);
			options.put(kv[0], kv.length > 1 ? kv[1] : "true");
		}
		var scenario = args.length > 0 ? args[0] : "request";
		if ("drain".equals(scenario) && "ga".equals(options.get("manager"))) {
			// as in the cloud: the HTTP ticket manager makes no request of its own
			System.setProperty("convertigo.cloud.customer_name", "billing-bench");
		}
		setUpEngine();
		System.out.println("# " + String.join(" ", args) + " | java " + System.getProperty("java.version") + " | "
				+ Runtime.getRuntime().availableProcessors() + " cpus | implementation "
				+ (hasTicketQueues() ? "queues per manager" : "single consumer"));
		switch (scenario) {
		case "request": request(options); break;
		case "drain": drain(options); break;
		case "shutdown": shutdown(options); break;
		default: throw new IllegalArgumentException("unknown scenario " + scenario);
		}
		System.exit(0);
	}

	public static void setUpEngine() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		Engine.logBillers = Logger.getLogger("cems.Billers");
		Engine.logEngine = Logger.getLogger("cems.Engine");
		Engine.logContext = Logger.getLogger("cems.Context");
		EnginePropertiesManager.initProperties();
		// as in a Studio: only the ticket managers of the configuration
		Field studio = Engine.class.getDeclaredField("bStudioMode");
		studio.setAccessible(true);
		studio.set(null, true);
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_GOOGLE_ENABLED, "false");
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_ENABLED, "false");
		Engine.theApp = allocate(Engine.class);
		Engine.theApp.eventManager = new EventManager();
		// no proxy for the HTTP ticket manager
		Engine.theApp.proxyManager = allocate(ProxyManager.class);
		Engine.theApp.proxyManager.proxyMode = EnginePropertiesManager.ProxyMode.off;
		if (com.twinsoft.tas.KeyManager.keys == null) {
			// no license key: the HTTP ticket manager posts no license
			com.twinsoft.tas.KeyManager.keys = new java.util.Hashtable<Object, Object>();
		}
	}

	@SuppressWarnings("unchecked")
	static <T> T allocate(Class<T> cls) throws Exception {
		Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		field.setAccessible(true);
		return (T) ((sun.misc.Unsafe) field.get(null)).allocateInstance(cls);
	}

	static boolean hasTicketQueues() {
		try {
			Class.forName("com.twinsoft.convertigo.engine.billing.TicketQueue");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/** The billing started as the engine does, then with the given ticket managers instead of the configured ones. */
	public static BillingManager start(List<ITicketManager> managers) throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_ENABLED, managers.isEmpty() ? "false" : "true");
		var billing = new BillingManager();
		billing.init(); // the JDBC ticket manager it creates connects only at its first ticket: replaced before
		if (!managers.isEmpty()) {
			useTicketManagers(billing, managers);
		}
		Engine.theApp.billingManager = billing;
		return billing;
	}

	@SuppressWarnings("unchecked")
	public static void useTicketManagers(BillingManager billing, List<ITicketManager> managers) throws Exception {
		try {
			Method method = BillingManager.class.getDeclaredMethod("useTicketManagers", List.class);
			method.setAccessible(true);
			method.invoke(billing, managers);
			return;
		} catch (NoSuchMethodException e) {
			// a single consumer for all the ticket managers
		}
		Field field = BillingManager.class.getDeclaredField("managers");
		field.setAccessible(true);
		var current = (Collection<ITicketManager>) field.get(billing);
		for (var manager : current) {
			manager.destroy();
		}
		current.clear();
		current.addAll(managers);
	}

	/** A request of a session, as the billing reads it. */
	static Context context(int index, RequestableObject requestable) throws Exception {
		Context context = allocate(Context.class);
		context.statistics = new EngineStatistics();
		context.requestedObject = requestable;
		context.projectName = "BenchProject";
		context.remoteAddr = "10.0.0." + (index % 250 + 1);
		context.userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) bench/" + index;
		var session = new RequestScopedHttpSession(null, "BENCHSESSION" + index);
		SessionAttribute.authenticatedUser.set(session, "user" + index);
		session.setAttribute(Parameter.UIid.name(), "ui-" + index);
		session.setAttribute(Parameter.DeviceUUID.name(), "web-" + index);
		context.httpSession = session;
		return context;
	}

	static RequestableObject sequence(String name) throws Exception {
		var sequence = new GenericSequence();
		sequence.setName(name);
		return sequence;
	}

	/** Counts the tickets written, each costing {@code costNanos} of waiting (a remote write) to the writer. */
	static final class Sink implements ITicketManager {
		final LongAdder written = new LongAdder();
		final long costNanos;

		Sink(long costNanos) {
			this.costNanos = costNanos;
		}

		@Override public void addTicket(Ticket ticket) {
			if (costNanos > 0) {
				LockSupport.parkNanos(costNanos);
			}
			written.increment();
		}

		@Override public void removeTicket(Ticket ticket) {
		}

		@Override public Ticket newTicket() {
			return new Ticket();
		}

		@Override public void destroy() {
		}
	}

	/** Durations in ns, in buckets of 1/8 of a power of two. */
	static final class Histogram {
		final long[] counts = new long[64 * 8];
		long total;
		long sum;

		void add(long nanos) {
			if (nanos < 1) {
				nanos = 1;
			}
			int exp = 63 - Long.numberOfLeadingZeros(nanos);
			int sub = exp < 3 ? 0 : (int) ((nanos >>> (exp - 3)) & 7);
			counts[exp * 8 + sub]++;
			total++;
			sum += nanos;
		}

		void addAll(Histogram other) {
			for (int i = 0; i < counts.length; i++) {
				counts[i] += other.counts[i];
			}
			total += other.total;
			sum += other.sum;
		}

		long percentile(double p) {
			long rank = (long) Math.ceil(total * p);
			long seen = 0;
			for (int i = 0; i < counts.length; i++) {
				seen += counts[i];
				if (seen >= rank) {
					int exp = i / 8;
					int sub = i % 8;
					return exp < 3 ? (1L << exp) : (1L << exp) + ((long) sub << (exp - 3));
				}
			}
			return 0;
		}
	}

	static final class Producer extends Thread {
		final Context context;
		final BillingManager billing;
		final long workNanos;
		final long idleNanos;
		final AtomicBoolean measuring;
		final AtomicBoolean running;
		final Histogram histogram = new Histogram();
		long produced;
		long allocated;
		long blockedCount;
		long blockedMillis;
		long waitedCount;
		long waitedMillis;

		Producer(Context context, BillingManager billing, long workNanos, long idleNanos, AtomicBoolean measuring,
				AtomicBoolean running) {
			super("bench-producer-" + context.httpSession.getId());
			this.context = context;
			this.billing = billing;
			this.workNanos = workNanos;
			this.idleNanos = idleNanos;
			this.measuring = measuring;
			this.running = running;
		}

		@Override public void run() {
			try {
				while (!measuring.get()) {
					bill();
					between();
				}
				long id = getId();
				var start = THREADS.getThreadInfo(id);
				long bytes = THREADS.getThreadAllocatedBytes(id);
				while (running.get()) {
					long t0 = System.nanoTime();
					bill();
					histogram.add(System.nanoTime() - t0);
					produced++;
					between();
				}
				allocated = THREADS.getThreadAllocatedBytes(id) - bytes;
				var stop = THREADS.getThreadInfo(id);
				blockedCount = stop.getBlockedCount() - start.getBlockedCount();
				blockedMillis = stop.getBlockedTime() - start.getBlockedTime();
				waitedCount = stop.getWaitedCount() - start.getWaitedCount();
				waitedMillis = stop.getWaitedTime() - start.getWaitedTime();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		void bill() throws EngineException {
			billing.insertBilling(context);
		}

		/** the rest of a requestable: computing ({@code work}) or waiting for a back end ({@code idle}) */
		void between() {
			if (workNanos > 0) {
				long end = System.nanoTime() + workNanos;
				while (System.nanoTime() < end) {
					Thread.onSpinWait();
				}
			}
			if (idleNanos > 0) {
				LockSupport.parkNanos(idleNanos);
			}
		}
	}

	static void request(Map<String, String> options) throws Exception {
		var manager = options.getOrDefault("manager", "sink");
		long seconds = Long.parseLong(options.getOrDefault("seconds", "3"));
		long warmup = Long.parseLong(options.getOrDefault("warmup", "2"));
		long workNanos = TimeUnit.MICROSECONDS.toNanos(Long.parseLong(options.getOrDefault("work", "0")));
		long idleNanos = TimeUnit.MICROSECONDS.toNanos(Long.parseLong(options.getOrDefault("idle", "0")));
		long slowNanos = TimeUnit.MICROSECONDS.toNanos(Long.parseLong(options.getOrDefault("slowMicros", "1000")));
		THREADS.setThreadContentionMonitoringEnabled(true);
		var requestable = sequence("BenchSequence");
		System.out.println("threads  tickets/s  ns/ticket(mean  p50  p99  p99.9)  bytes/ticket  blocked/1k  blockedMs  waited/1k  waitedMs  written  backlog");
		for (var t : options.getOrDefault("threads", "1,5,20").split(",")) {
			int threads = Integer.parseInt(t.trim());
			var sink = "none".equals(manager) ? null : new Sink("slow".equals(manager) ? slowNanos : 0);
			var billing = start(sink == null ? List.of() : List.of(sink));
			var measuring = new AtomicBoolean();
			var running = new AtomicBoolean(true);
			var producers = new ArrayList<Producer>();
			for (int i = 0; i < threads; i++) {
				producers.add(new Producer(context(i, requestable), billing, workNanos, idleNanos, measuring, running));
			}
			producers.forEach(Thread::start);
			Thread.sleep(TimeUnit.SECONDS.toMillis(warmup));
			long writtenBefore = sink == null ? 0 : sink.written.sum();
			measuring.set(true);
			long t0 = System.nanoTime();
			Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
			running.set(false);
			for (var producer : producers) {
				producer.join();
			}
			long elapsed = System.nanoTime() - t0;
			long writtenDuring = sink == null ? 0 : sink.written.sum() - writtenBefore;
			var all = new Histogram();
			long produced = 0, allocated = 0, blocked = 0, blockedMs = 0, waited = 0, waitedMs = 0;
			for (var producer : producers) {
				all.addAll(producer.histogram);
				produced += producer.produced;
				allocated += producer.allocated;
				blocked += producer.blockedCount;
				blockedMs += producer.blockedMillis;
				waited += producer.waitedCount;
				waitedMs += producer.waitedMillis;
			}
			System.out.println(String.format(Locale.ROOT, "%7d  %9.0f  %15.0f %4d %4d %6d  %12.0f  %10.2f  %9d  %9.2f  %8d  %8d  %7s", threads,
					produced * 1e9 / elapsed, (double) all.sum / all.total, all.percentile(0.5), all.percentile(0.99),
					all.percentile(0.999), (double) allocated / produced, blocked * 1000.0 / produced, blockedMs,
					waited * 1000.0 / produced, waitedMs, writtenDuring, sink == null ? "-" : "" + backlog(billing)));
			billing.destroy();
			System.gc();
		}
	}

	/** Tickets waiting for their ticket manager, whatever the implementation. */
	static long backlog(BillingManager billing) {
		try {
			Method method = BillingManager.class.getDeclaredMethod("pendingTickets");
			method.setAccessible(true);
			return ((Number) method.invoke(billing)).longValue();
		} catch (NoSuchMethodException e) {
			try {
				Field field = BillingManager.class.getDeclaredField("tickets");
				field.setAccessible(true);
				var tickets = (Collection<?>) field.get(billing);
				if (tickets == null) {
					return 0;
				}
				synchronized (tickets) {
					return tickets.size();
				}
			} catch (ReflectiveOperationException ex) {
				return -1;
			}
		} catch (ReflectiveOperationException e) {
			return -1;
		}
	}

	interface Written {
		long count() throws Exception;
	}

	static void drain(Map<String, String> options) throws Exception {
		var manager = options.getOrDefault("manager", "sink");
		int tickets = Integer.parseInt(options.getOrDefault("tickets", "20000"));
		int threads = Integer.parseInt(options.getOrDefault("threads", "4"));
		long timeout = TimeUnit.SECONDS.toNanos(Long.parseLong(options.getOrDefault("timeout", "120")));
		BillingManager billing;
		Written written;
		HttpServer collector = null;
		java.sql.Connection connection = null;
		var posts = new LongAdder();
		switch (manager) {
		case "sink": {
			var sink = new Sink(TimeUnit.MICROSECONDS.toNanos(Long.parseLong(options.getOrDefault("sinkMicros", "0"))));
			billing = start(List.of(sink));
			written = () -> sink.written.sum();
			break;
		}
		case "db": {
			var url = options.getOrDefault("jdbcUrl", "jdbc:mariadb://127.0.0.1:3306/c8oAnalytics");
			var user = options.getOrDefault("jdbcUser", "root");
			var password = System.getenv().getOrDefault("BILLING_BENCH_JDBC_PASSWORD", "");
			EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_URL, url);
			EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_USERNAME, user);
			EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_PASSWORD, password);
			EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_PERSISTENCE_ENABLED, "true");
			billing = new BillingManager();
			billing.init();
			Engine.theApp.billingManager = billing;
			Class.forName(EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_DRIVER));
			connection = DriverManager.getConnection(url, user, password);
			var jdbc = connection;
			// the table is created by the first ticket
			billing.insertBilling(context(0, sequence("Warmup")));
			long deadline = System.nanoTime() + timeout;
			while (true) {
				try (var statement = jdbc.createStatement(); var rs = statement.executeQuery("SELECT COUNT(*) FROM Ticket")) {
					rs.next();
					if (rs.getLong(1) > 0) {
						break;
					}
				} catch (java.sql.SQLException e) {
					// not created yet
				}
				if (System.nanoTime() > deadline) {
					throw new IllegalStateException("no ticket table");
				}
				Thread.sleep(50);
			}
			try (var statement = jdbc.createStatement()) {
				statement.execute("DELETE FROM Ticket");
			}
			written = () -> {
				try (var statement = jdbc.createStatement(); var rs = statement.executeQuery("SELECT COUNT(*) FROM Ticket")) {
					rs.next();
					return rs.getLong(1);
				}
			};
			break;
		}
		case "ga": {
			long latency = Long.parseLong(options.getOrDefault("latencyMs", "20"));
			var events = new LongAdder();
			collector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			collector.setExecutor(Executors.newFixedThreadPool(16));
			collector.createContext("/mp/collect", exchange -> {
				try (InputStream in = exchange.getRequestBody()) {
					var body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
					if (latency > 0) {
						Thread.sleep(latency);
					}
					events.add(new JSONObject(body).getJSONArray("events").length());
					posts.increment();
					exchange.sendResponseHeaders(204, -1);
				} catch (Exception e) {
					exchange.sendResponseHeaders(400, -1);
				} finally {
					exchange.close();
				}
			});
			collector.start();
			var ga = new GoogleAnalyticsTicketManager("G-BENCH", "bench", Engine.logBillers);
			Field url = GoogleAnalyticsTicketManager.class.getDeclaredField("url");
			url.setAccessible(true);
			url.set(ga, new URI("http://127.0.0.1:" + collector.getAddress().getPort() + "/mp/collect?measurement_id=G-BENCH&api_secret=bench"));
			if (!"127.0.0.1".equals(((URI) url.get(ga)).getHost())) {
				throw new IllegalStateException("the collector must be local");
			}
			billing = start(List.of(ga));
			written = () -> events.sum();
			break;
		}
		default:
			throw new IllegalArgumentException("unknown manager " + manager);
		}

		var requestable = sequence("BenchSequence");
		var ready = new CountDownLatch(threads);
		var go = new CountDownLatch(1);
		var producerNanos = new LongAdder();
		var producers = new ArrayList<Thread>();
		for (int i = 0; i < threads; i++) {
			var context = context(i, requestable);
			int share = tickets / threads + (i < tickets % threads ? 1 : 0);
			var producer = new Thread(() -> {
				try {
					ready.countDown();
					go.await();
					long t0 = System.nanoTime();
					for (int n = 0; n < share; n++) {
						billing.insertBilling(context);
					}
					producerNanos.add(System.nanoTime() - t0);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			producers.add(producer);
			producer.start();
		}
		ready.await();
		long t0 = System.nanoTime();
		go.countDown();
		for (var producer : producers) {
			producer.join();
		}
		long produced = System.nanoTime() - t0;
		long count;
		long deadline = t0 + timeout;
		while ((count = written.count()) < tickets && System.nanoTime() < deadline) {
			Thread.sleep(5);
		}
		long elapsed = System.nanoTime() - t0;
		System.out.println(String.format(Locale.ROOT,
				"manager=%s tickets=%d written=%d producedIn=%.1fms (%.0f ns/ticket/thread) writtenIn=%.1fms -> %.0f tickets/s%s",
				manager, tickets, count, produced / 1e6, producerNanos.sum() / (double) tickets, elapsed / 1e6,
				count * 1e9 / elapsed, "ga".equals(manager) ? " posts=" + posts.sum() : ""));
		billing.destroy();
		if (collector != null) {
			collector.stop(0);
		}
		if (connection != null) {
			connection.close();
		}
	}

	static void shutdown(Map<String, String> options) throws Exception {
		int tickets = Integer.parseInt(options.getOrDefault("tickets", "20000"));
		var sink = new Sink(TimeUnit.MICROSECONDS.toNanos(Long.parseLong(options.getOrDefault("sinkMicros", "50"))));
		var billing = start(List.of(sink));
		var context = context(0, sequence("BenchSequence"));
		for (int n = 0; n < tickets; n++) {
			billing.insertBilling(context);
		}
		long beforeStop = sink.written.sum();
		long t0 = System.nanoTime();
		billing.destroy();
		long stopped = System.nanoTime() - t0;
		long atStop = sink.written.sum();
		Thread.sleep(1000);
		long later = sink.written.sum();
		System.out.println(String.format(Locale.ROOT,
				"tickets=%d writtenBeforeStop=%d writtenWhenStopReturned=%d writtenOneSecondLater=%d lost=%d stopTook=%.1fms",
				tickets, beforeStop, atStop, later, tickets - later, stopped / 1e6));
	}
}
