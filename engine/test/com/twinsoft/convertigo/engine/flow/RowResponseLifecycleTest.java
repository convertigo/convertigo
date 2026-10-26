/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import com.twinsoft.convertigo.engine.util.JsonValues;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import com.twinsoft.convertigo.engine.billing.RowBilling;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.flow.rows.ResultDocument;
import com.twinsoft.convertigo.engine.requesters.JsonResponse;

public class RowResponseLifecycleTest {
	private ResultDocument document(ResultDocumentTest.Source source) throws Exception {
		return new ResultDocument(source,"data",JsonValues.object(),new ResultDocument.Limits(100,100_000,1024,2,4096));
	}
	@Test public void jsonpPreflightPreservesEarlyErrorAndWritesNoPartialArray() throws Exception {
		var source=new ResultDocumentTest.Source();source.failAt=1;
		try(var response=new JsonResponse(JsonValues.object(),"callback",document(source))) {
			assertEquals(502,response.prepare());var out=new StringWriter();response.writeTo(out);
			assertTrue(out.toString().startsWith("callback({\"error\":"));assertTrue(out.toString().endsWith(");"));
			assertFalse(out.toString().contains("data"));assertEquals(1,source.closed);
		}
	}
	@Test public void printWriterDisconnectStopsBeforePullingTheNextRow() throws Exception {
		var source=new ResultDocumentTest.Source(); var captured=new StringWriter();boolean[] closed={false};
		var output=new PrintWriter(new Writer(){
			@Override public void write(char[] c,int o,int n) { captured.write(c,o,n); }
			@Override public void flush() throws IOException { throw new IOException("synthetic disconnect"); }
			@Override public void close() { closed[0]=true; }
		});
		try(var response=new JsonResponse(JsonValues.object(),null,document(source))) {
			try { response.writeToResponse(output);fail("disconnect"); }catch(IOException expected) { }
		}
		assertEquals(2,source.pulls);assertEquals(1,source.closed);assertTrue(closed[0]);assertFalse(captured.toString().contains("error"));
	}
	@Test public void billingCapturesOneDetachedTicketAndLateStatusForAllExitPaths() throws Exception {
		for(int mode=0;mode<4;mode++) {
			var source=new ResultDocumentTest.Source();if(mode==1)source.failAt=4;if(mode==2)source.failAt=1;
			var doc=document(source);var ticket=new Ticket();ticket.setResponseTime(10);var tickets=new ArrayList<Ticket>();
			RowBilling.defer(doc,ticket,tickets::add);assertTrue(tickets.isEmpty());
			if(mode==3) doc.close(); else try(var response=new JsonResponse(JsonValues.object(),"cb",doc)) { response.writeTo(new StringWriter()); }
			doc.close();assertEquals(1,tickets.size());assertSame(ticket,tickets.get(0));assertTrue(ticket.getResponseTime()>=10);
			assertEquals(mode==0,ticket.isResponseComplete());assertEquals(mode==0?200:mode==3?499:502,ticket.getCompletionStatus());
		}
	}
	@Test public void contextResetDiscardsAnUnselectedResponseWithoutOpeningIt() throws Exception {
		com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
		com.twinsoft.convertigo.engine.Engine.logContext=org.apache.log4j.Logger.getLogger("row-test");
		var previous=com.twinsoft.convertigo.engine.Engine.theApp;
		com.twinsoft.convertigo.engine.Engine.theApp=new com.twinsoft.convertigo.engine.Engine();
		var source=new ResultDocumentTest.Source();var context=new com.twinsoft.convertigo.engine.Context("row-test");
		try { context.nativeRows=document(source);context.reset();assertEquals(0,source.pulls);assertEquals(1,source.closed);assertNull(context.nativeRows); }
		finally { com.twinsoft.convertigo.engine.Engine.theApp=previous; }
	}
}
