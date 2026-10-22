/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import com.twinsoft.convertigo.engine.flow.rows.PagedJsonRows;
import com.twinsoft.convertigo.engine.flow.rows.RowStreamException;

public class PagedJsonRowsTest {
	@Test public void numericPageModeNeverUsesReturnedUrlAuthorityOrFilters() throws Exception {
		try(var server=new Server((n,q)-> {
			if(n==2)assertEquals("filter=x&filter=y&page=2",q);
			return n==1?"{\"next\":\"https://untrusted.invalid/unrelated?filter=wrong&page=2\",\"results\":[{\"id\":1}]}":"{\"results\":[{\"id\":2}],\"next\":null}";
		});var client=HttpClients.createDefault()) {
			try(var source=new PagedJsonRows(client::execute,false,URI.create(server.uri.toString().split("\\?")[0]+"?filter=x&filter=y&page=1"),Map.of(),"/results","/next",new PagedJsonRows.Bounds(3,10,10000,100,1024,16,10000),0,1000,1000,List.of(),Set.of(),"page")) {
				assertEquals(1,source.next().getInt("id"));assertEquals(2,source.next().getInt("id"));assertNull(source.next());assertEquals(2,server.calls.get());
			}
		}
	}
	@Test public void redirectsAreNotFollowedAndTimeoutClosesTheBody() throws Exception {
		var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
		var redirected=new AtomicInteger();
		server.createContext("/redirect",e->{e.getResponseHeaders().set("Location","/target");e.sendResponseHeaders(302,-1);e.close();});
		server.createContext("/target",e->{redirected.incrementAndGet();e.sendResponseHeaders(500,-1);e.close();});
		server.createContext("/slow",e->{e.sendResponseHeaders(200,0);try{Thread.sleep(500);}catch(InterruptedException stop){Thread.currentThread().interrupt();}e.close();});
		server.start();
		try {
			for(var path:List.of("/redirect","/slow")) {
				try(var source=new PagedJsonRows(HttpClients.createDefault(),true,URI.create("http://127.0.0.1:"+server.getAddress().getPort()+path),Map.of(),"/results","/next",new PagedJsonRows.Bounds(3,10,10000,100,1024,16,2000),0,1000,50,List.of(),Set.of())) {
					try{source.next();fail("HTTP failure");}catch(RowStreamException expected){assertEquals(path.equals("/slow")?504:502,expected.status());}
				}
			}
			assertEquals(0,redirected.get());
		} finally {server.stop(0);}
	}
	@Test public void cancellationInterruptsBlockedReadAndDoesNotFetchAnotherPage() throws Exception {
		var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
		var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
		server.createContext("/rows",e->{e.sendResponseHeaders(200,0);e.getResponseBody().write("{\"results\":[".getBytes(StandardCharsets.UTF_8));e.getResponseBody().flush();entered.countDown();try{release.await();}catch(InterruptedException stop){Thread.currentThread().interrupt();}e.close();});server.start();
		var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
		try(var source=new PagedJsonRows(HttpClients.createDefault(),true,URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/rows"),Map.of(),"/results","/next",new PagedJsonRows.Bounds(3,10,10000,100,1024,16,10000),0,1000,5000,List.of(),Set.of())) {
			var reader=new Thread(()->{try{source.next();}catch(Throwable error){failure.set(error);}});reader.start();
			assertTrue(entered.await(2,java.util.concurrent.TimeUnit.SECONDS));source.close();reader.join(2000);
			assertFalse(reader.isAlive());assertTrue(failure.get() instanceof RowStreamException);assertEquals(499,((RowStreamException)failure.get()).status());
		} finally {release.countDown();server.stop(0);}
	}
	@Test public void depthStringNameAndTrailingDocumentLimitsAreEnforced() throws Exception {
		for(var json:List.of("{\"results\":[{\"x\":\""+"x".repeat(5000)+"\"}]}",
				"{\"results\":[{\""+"k".repeat(5000)+"\":1}]}","{\"results\":[{\"x\":"+"[".repeat(40)+"0"+"]".repeat(40)+"}]}",
				"{\"results\":[],\"next\":null}{\"results\":[]}")) {
			try(var server=new Server((n,q)->json);var source=source(server,3,100,100000,0,"/results","/next")) {
				try{while(source.next()!=null){}fail("malformed/unbounded input");}catch(RowStreamException expected){assertTrue(expected.status()==413||expected.status()==502);}
			}
		}
	}
	interface Page { String body(int request, String query); }
	static final class Server implements AutoCloseable {
		final HttpServer server; final AtomicInteger calls = new AtomicInteger(); final URI uri;
		Server(Page page) throws IOException {
			server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
			uri=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/rows?page=1");
			server.createContext("/rows",exchange->{
				var body=page.body(calls.incrementAndGet(),exchange.getRequestURI().getRawQuery()).getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type","application/json"); exchange.sendResponseHeaders(200,0);
				try(var out=exchange.getResponseBody()) { for(int i=0;i<body.length;i+=3) { out.write(body,i,Math.min(3,body.length-i)); out.flush(); } }
				catch(IOException disconnected) { } finally { exchange.close(); }
			}); server.start();
		}
		@Override public void close() { server.stop(0); }
	}
	private PagedJsonRows source(Server server,int pages,long rows,long bytes,long take,String items,String next) throws Exception {
		return new PagedJsonRows(HttpClients.custom().disableAutomaticRetries().build(),true,server.uri,
				Map.of("Authorization","Bearer synthetic-test-credential"),items,next,
				new PagedJsonRows.Bounds(pages,rows,bytes,1000,4096,32,10_000),take,1000,1000,
				List.of("synthetic-test-credential"),Set.of("__proto__"));
	}
	@Test public void parsesChunksMetadataBeforeAndAfterRowsAndNestedPointers() throws Exception {
		try(var server=new Server((n,q)-> n==1 ? "{\"links\":{\"next\":\"?page=2\"},\"payload\":{\"results\":[{\"id\":1,\"text\":\"é☃\"}]}}"
				: "{\"payload\":{\"results\":[{\"id\":2,\"values\":[null,true,1.5]}]},\"links\":{\"next\":null}}")) {
			try(var source=source(server,10,100,100_000,0,"/payload/results","/links/next")) {
				assertEquals("é☃",source.next().getString("text")); assertEquals(2,source.next().getInt("id")); assertNull(source.next());
			}
			assertEquals(2,server.calls.get());
		}
	}
	@Test public void redactsKnownCredentialsWithoutDroppingBusinessKeys() throws Exception {
		try(var server=new Server((n,q)->"{\"results\":[{\"key\":\"business\",\"text\":\"echo synthetic-test-credential\",\"__proto__\":{\"x\":1}}],\"next\":null}")) {
			try(var source=source(server,10,100,100_000,0,"/results","/next")) {
				var row=source.next(); assertEquals("business",row.getString("key")); assertEquals("echo [redacted]",row.getString("text")); assertFalse(row.has("__proto__")); assertNull(source.next());
			}
		}
	}
	@Test public void crossOriginPathTraversalAndCredentialsNeverReceiveAnotherRequest() throws Exception {
		for(var next:List.of("http://127.0.0.1:1/rows?page=2","/another?page=2","http://user:password@127.0.0.1/rows","/../other")) {
			try(var server=new Server((n,q)->"{\"results\":[{\"id\":1}],\"next\":\""+next+"\"}")) {
				try(var source=source(server,10,100,100_000,0,"/results","/next")) {
					assertNotNull(source.next()); try { source.next(); fail("untrusted next"); } catch(RowStreamException expected) { assertFalse(expected.getMessage().contains(next)); }
				}
				assertEquals(1,server.calls.get());
			}
		}
	}
	@Test public void repeatedPageAndPageRowByteBudgetsStopFetching() throws Exception {
		for(var kind:List.of("repeat","pages","rows","bytes")) {
			try(var server=new Server((n,q)->"{\"results\":[{\"id\":1},{\"id\":2}],\"next\":\"?page="+(kind.equals("repeat")?"1":"2")+"\"}")) {
				try(var source=source(server,kind.equals("pages")?1:10,kind.equals("rows")?1:100,kind.equals("bytes")?8:100_000,0,"/results","/next")) {
					try { while(source.next()!=null) { } fail("bounded producer"); }
					catch(RowStreamException expected) { assertTrue(expected.code().startsWith("ROW_STREAM_")); }
				}
				assertEquals(1,server.calls.get());
			}
		}
	}
	@Test public void malformedAndMissingArraysAreRejected() throws Exception {
		for(var body:List.of("{\"results\":[{\"n\":", "{\"results\":{}}", "{\"other\":[]}", "{\"results\":[1],\"next\":null}")) {
			try(var server=new Server((n,q)->body);var source=source(server,10,100,100_000,0,"/results","/next")) {
				try { source.next(); fail("invalid document"); } catch(RowStreamException expected) { }
			}
		}
	}
	@Test public void requestedTakeAndAbandonmentDoNotReadAnotherPage() throws Exception {
		try(var server=new Server((n,q)->"{\"results\":[{\"id\":1},{\"id\":2}],\"next\":\"?page=2\"}")) {
			try(var source=source(server,10,100,100_000,1,"/results","/next")) { assertNotNull(source.next()); assertNull(source.next()); }
			assertEquals(1,server.calls.get());
			var abandoned=source(server,10,100,100_000,0,"/results","/next"); abandoned.close(); abandoned.close();
			assertEquals(1,server.calls.get());
		}
	}
}
