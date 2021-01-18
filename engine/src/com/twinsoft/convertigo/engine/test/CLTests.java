package com.twinsoft.convertigo.engine.test;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.w3c.dom.Document;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CLTests {
	@Test
	public void testXmlToJson() {
		try {
			System.out.println("--- testXmlToJson ---------------------------------");
			// <tag/>  => {"tag": ""}
			// <tag c8o_emptyOject=""/>  => {"tag": {}}
			// <tag c8o_needAttr=""/>  => {"tag": {"text":"", "attr":[]}}
			// <tag c8o_emptyOject="" c8o_needAttr=""/>  => {"tag": {"attr":[]}}
			
			// <tag>text</tag>  => {"tag": {"text":"text"}}
			// <tag><user>user</user></tag>  => {"tag": {"user":"user"}}
			// <tag c8o_needAttr="">text</tag>  => {"tag": {"text":"text", "attr":[]}}
			// <tag c8o_needAttr=""><user>user</user></tag>  => {"tag": {"user":"user", "attr":[]}}

			// <array c8o_arrayOfSingle=""/> => {"array": [""]}
			// <enum c8o_arrayOfSingle="">text</enum> => {"enum":["text"]}		
			// <user c8o_arrayOfSingle=""><name>name</name></user>  => {"user":[{"name":"name"}]}
			List<String> list = new ArrayList<String>();
			list.add("<root><tag/></root>");
			list.add("<root><tag c8o_emptyObject=\"\"/></root>");
			list.add("<root><tag c8o_needAttr=\"\"/></root>");
			list.add("<root><tag c8o_emptyObject=\"\" c8o_needAttr=\"\"/></root>");
			list.add("<root><tag>text</tag></root>");
			list.add("<root><tag><user>user</user></tag></root>");
			list.add("<root><tag c8o_needAttr=\"\">text</tag></root>");
			list.add("<root><tag c8o_needAttr=\"\"><user>user</user></tag></root>");
			list.add("<root><array c8o_arrayOfSingle=\"\"/></root>");
			list.add("<root><enum c8o_arrayOfSingle=\"\">text</enum></root>");
			list.add("<root><user c8o_arrayOfSingle=\"\"><name>name</name></user></root>");
			
			list.add("<root><tag type=\"boolean\">true</tag></root>");
			list.add("<root><tag><enum>val1</enum><enum>val2</enum></tag></root>");
			list.add("<root><tag><enum>val1</enum><enum>val2</enum><user>user</user></tag></root>");
			list.add("<root><tag type=\"array\"><enum>val1</enum><enum>val2</enum><user>user</user></tag></root>");
			
			for (String xmlString : list) {
				Document doc = XMLUtils.parseDOMFromString(xmlString);
				String jsonString = XMLUtils.XmlToJson(doc.getDocumentElement(), true, true);
				System.out.println(xmlString + " => " + new JSONObject(jsonString).toString());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test
	public void testXmlRestCompliancy() {
		try {
			System.out.println("--- testXmlRestCompliancy--------------------------");
			String[] args = {"-p", "C:/Dev/c8o-customers/bnpp/tests/test.properties"};
			CLIRest.main(args);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
