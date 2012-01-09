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

package com.twinsoft.convertigo.engine.enums;

public enum Parameter {

	Abort("__abort"),
	Async("__async"),
	Context("__context"),
	Connector("__connector"),
	ConnectorConnectionString("_OVER__ConnectionString"),
	DynamicVariablePost("__POST_"),		// __POST_XX
	DynamicVariableGet("__GET_"), 		// __GET_XX
	HtmlStatefull("__statefull"),
	HttpHeader("__header_"), 			// __header_XX
	HttpUri("__uri"),
	NoCache("__nocache"),
	Pool("__pool"),
	Project("__project"),
	ProxyPost("__post"),
	ProxyGoto("__goto"),
	ProxyThen("__then"),
	RemoveContext("__removeContext"),
	RemoveSession("__removeSession"),
	RsaEncoded("__encoded"),
	Sequence("__sequence"),
	SequenceInheritedTransactionContext("__inheritedContext"),
	SessionId("__jSessionID"),
	Supervision("__supervision"),
	Testcase("__testcase"),
	Transaction("__transaction"),
	TransactionMotherSequenceContext("__motherContext"),
	Xslt("__xslt"),
	
	Carioca("__bCarioca"),
	CariocaUser("__user,"),
	CariocaPassword("__password"),
	CariocaSesskey("__sesskey"),
	CariocaService("__service"),
	
	Vic("__bVic"),
	VicUser("__VicUser"),
	VicGroup("__VicGroup"),
	VicServiceCode("__VicServiceCode"),
	VicDteAddress("__VicDteAddress"),
	VicCommDevice("__VicCommDevice"),
	
	JsonCallback("__callback"),
	
	JavelinBlock("__block_"),
	JavelinField("__field_"), 			// __field_XX
	JavelinAction("__javelin_action"),
	JavelinCurrentField("__javelin_current_field"),
	JavelinSignature("__signature"),
	
	WebEventAction("__event_action"),
	WebEvent("__event_"), 				// __event_XX
	
	WidgetContainer("__container"),
	WidgetName("__widget_name"),
	WidgetType("__widget_type");
	
	String param_name;
	
	Parameter(String name) {
		this.param_name = name;
	}
	
	public String getName() {
		return param_name;
	}
	
}
