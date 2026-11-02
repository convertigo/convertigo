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

package com.twinsoft.convertigo.engine.billing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.apache.log4j.Logger;
import org.hibernate.exception.JDBCConnectionException;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/**
 * The tickets waiting for one ticket manager, written by a thread of its own: a request only adds its ticket, without
 * lock, without waiting for the manager nor waking up its thread, and a slow manager does not delay the others. The
 * thread hands the manager the tickets gathered since its last pass, at most {@link #BATCH} at once.
 * <p>
 * Bounded: when the manager is slower than the requests for long, a ticket beyond the capacity is dropped and counted,
 * instead of the memory growing until the server fails. When the billing stops, the tickets already queued are still
 * written, for at most {@link #FLUSH_TIMEOUT} ms.
 */
public final class TicketQueue {
	/** the most tickets handed at once to the manager */
	public static final int BATCH = 500;
	/** the longest wait of the writer for new tickets: its latency, the requests never wake it up */
	static final long LINGER = TimeUnit.MILLISECONDS.toNanos(100);
	/** the longest time to write the queued tickets when the billing stops */
	public static final long FLUSH_TIMEOUT = 5000;
	private static final long DROP_LOG_INTERVAL = TimeUnit.MINUTES.toNanos(1);

	private final ITicketManager manager;
	private final int capacity;
	private final Logger log;
	private final ConcurrentLinkedQueue<Ticket> queue = new ConcurrentLinkedQueue<>();
	private final AtomicInteger size = new AtomicInteger();
	private final AtomicLong dropped = new AtomicLong();
	private final Thread writer;
	private volatile boolean closed;
	private volatile boolean finished;
	private volatile long graceEnd;
	private volatile long deadline;
	private long droppedLogged;
	private long nextDropLog;

	public TicketQueue(ITicketManager manager, int capacity, Logger log) {
		this.manager = manager;
		this.capacity = Math.max(1, capacity);
		this.log = log;
		writer = new Thread(this::run, "BillingManager writer " + manager.getClass().getSimpleName());
		writer.setDaemon(true);
		writer.start();
	}

	public ITicketManager getManager() {
		return manager;
	}

	/**
	 * Adds a ticket to write, unless the queue is full or its writer ended: then it is counted as dropped. A queue
	 * closed but still writing its last tickets takes it, for a request that read the queues just before they were
	 * replaced: its writer waits {@link #LINGER} after the close for such tickets.
	 */
	public boolean offer(Ticket ticket) {
		if (finished) {
			dropped.incrementAndGet();
			return false;
		}
		if (size.incrementAndGet() > capacity) {
			size.decrementAndGet();
			dropped.incrementAndGet();
			return false;
		}
		queue.offer(ticket);
		if (finished && queue.remove(ticket)) {
			// the writer ended meanwhile, without taking it
			size.decrementAndGet();
			dropped.incrementAndGet();
			return false;
		}
		return true;
	}

	/** tickets queued and not written yet */
	public int size() {
		return size.get();
	}

	/** tickets dropped since the creation of the queue */
	public long dropped() {
		return dropped.get();
	}

	/**
	 * Stops the writer: the queued tickets are written for at most {@link #FLUSH_TIMEOUT} ms, then the manager is
	 * destroyed, by the writer. With {@code wait}, returns once it is done or after the timeout.
	 */
	public void close(boolean wait) {
		if (!closed) {
			long now = System.nanoTime();
			graceEnd = now + LINGER;
			deadline = now + TimeUnit.MILLISECONDS.toNanos(FLUSH_TIMEOUT);
			closed = true;
			LockSupport.unpark(writer);
		}
		if (wait && writer != Thread.currentThread()) {
			try {
				writer.join(FLUSH_TIMEOUT + 1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void run() {
		Log4jHelper.mdcClear();
		var batch = new ArrayList<Ticket>(BATCH);
		try {
			while (true) {
				boolean stopping = closed;
				if (stopping && System.nanoTime() - deadline > 0) {
					break;
				}
				if (take(batch)) {
					write(batch);
					batch.clear();
					logDropped(false);
					continue;
				}
				if (stopping && System.nanoTime() - graceEnd > 0) {
					break;
				}
				LockSupport.parkNanos(this, LINGER);
			}
		} finally {
			finished = true;
			// the tickets added while the writer was ending: written while there is time, counted as dropped then
			long left = 0;
			while (take(batch)) {
				if (!closed || System.nanoTime() - deadline <= 0) {
					write(batch);
				} else {
					left += batch.size();
					dropped.addAndGet(batch.size());
				}
				batch.clear();
			}
			if (left > 0) {
				log.warn("(TicketQueue) " + left + " billing tickets not written by " + manager.getClass().getSimpleName()
						+ " within " + FLUSH_TIMEOUT + " ms when the billing stopped");
			}
			logDropped(true);
			try {
				manager.destroy();
			} catch (Throwable e) {
				log.error("(TicketQueue) TicketManager failed to destroy: [" + e.getClass().getName() + "] " + e.getMessage());
			}
		}
	}

	/** the next tickets, {@link #BATCH} at most: false when there is none */
	private boolean take(List<Ticket> batch) {
		Ticket ticket;
		while (batch.size() < BATCH && (ticket = queue.poll()) != null) {
			batch.add(ticket);
		}
		if (batch.isEmpty()) {
			return false;
		}
		size.addAndGet(-batch.size());
		return true;
	}

	private void write(List<Ticket> batch) {
		if (batch.size() > 1) {
			try {
				if (manager.addTickets(batch)) {
					return;
				}
			} catch (Throwable e) {
				if (log.isDebugEnabled()) {
					log.debug("(TicketQueue) " + batch.size() + " tickets not written together, written one by one: ["
							+ e.getClass().getName() + "] " + e.getMessage());
				}
			}
		}
		for (Ticket ticket : batch) {
			try {
				manager.addTicket(ticket);
			} catch (JDBCConnectionException e) {
				Throwable cause = e.getCause();
				log.info("JDBCConnectionException on ticket insertion"
						+ (cause == null ? "" : " (cause by " + cause.getClass().getSimpleName() + ")") + ": " + ticket);
			} catch (Throwable e) {
				log.error("Something failed in ticket insertion : " + ticket + "\n[" + e.getClass().getName() + "] " + e.getMessage());
			}
		}
	}

	private void logDropped(boolean now) {
		long total = dropped.get();
		if (total == droppedLogged) {
			return;
		}
		long time = System.nanoTime();
		if (!now && time - nextDropLog < 0) {
			return;
		}
		log.warn("(TicketQueue) " + (total - droppedLogged) + " billing tickets dropped, " + total + " in all: "
				+ manager.getClass().getSimpleName() + " writes them slower than the requests make them (capacity of "
				+ capacity + " tickets, " + PropertyName.ANALYTICS_QUEUE_CAPACITY.getKey() + ")");
		droppedLogged = total;
		nextDropLog = time + DROP_LOG_INTERVAL;
	}
}
