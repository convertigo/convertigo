/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

public enum Parameter {

	Abort("__abort"),
	Async("__async"),
	Context("__context"),
	Connector("__connector"),
	ConnectorConnectionString("_OVER__ConnectionString"),
	DynamicVariablePost(DynamicHttpVariable.__POST_.name()),		// __POST_XX
	DynamicVariableGet(DynamicHttpVariable.__GET_.name()), 		// __GET_XX
	HtmlStatefull("__statefull"),
	HttpHeader(DynamicHttpVariable.__header_.name()), 			// __header_XX
	HttpUri(DynamicHttpVariable.__uri.name()),
	HttpBody(DynamicHttpVariable.__body.name()),
	HttpContentType(DynamicHttpVariable.__contentType.name()),
	HttpDownloadFolder(DynamicHttpVariable.__download_folder.name()),
	HttpDownloadFilename(DynamicHttpVariable.__download_filename.name()),
	HttpDownloadUseContentDisposition(DynamicHttpVariable.__download_useContentDisposition.name()),
	Lang("__lang"),
	NoCache("__nocache"),
	Pool("__pool"),
	Project("__project"),
	ProxyPost("__post"),
	ProxyGoto("__goto"),
	ProxyThen("__then"),
	RemoveContext("__removeContext"),
	RemoveNamespaces("__removeNamespaces"),
	RemoveSession("__removeSession"),
	ResponseExpiryDate("__responseExpiryDate"),
	RsaEncoded("__encoded"),
	Sequence("__sequence"),
	SequenceInheritedTransactionContext("__inheritedContext"),
	SessionId("__jSessionID"),
	Stub("__stub"),
	StubFilename("__stub_filename"),
	Supervision("__supervision"),
	Testcase("__testcase"),
	Transaction("__transaction"),
	VoidTransaction("_void__transaction"),
	MotherSequenceContext("__motherContext"),
	UserReference("__user_reference"),
	UIid("__uid"),
	DeviceUUID("__uuid"),
	Xslt("__xslt"),

	Carioca("__bCarioca"),
	CariocaUser("__user"),
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
	JavelinModifiedFields("__javelin_modified_fields"),
	JavelinSignature("__signature"),
	
	WebEventAction("__event_action"),
	WebEvent("__event_"), 				// __event_XX
	
	WidgetContainer("__container"),
	WidgetName("__widget_name"),
	WidgetType("__widget_type"),
	
	ContentType("__content_type"),
	
	LocalCachePriority("__localCache_priority"),
	LocalCacheTimeToLive("__localCache_ttl"),
	
	XsrfToken("__xsrfToken")
	;
	
	String param_name;
	
	Parameter(String name) {
		this.param_name = name;
	}
	
	public String getName() {
		return param_name;
	}
	
}
