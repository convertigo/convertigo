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

package com.twinsoft.convertigo.engine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.common.WebClipper;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.KeyExpiredException;
import com.twinsoft.convertigo.engine.MaxCvsExceededException;

public class URLrewriter {
	protected String targetReferer;
	protected String targetPathReferer;
	protected String targetHost;
	protected String servletUrl;
	protected String mHttpTunnel;
	protected TwsCachedXPathAPI xpathApi;
	protected String attributesXpath;

	enum Patterns {
		importspace (Pattern.compile("@import[\\s]")),
		url (Pattern.compile("url *\\(.*\\)")), // handle any url(xxxx) type or url ('xxxx') type
		paren (Pattern.compile("[' ][^)]*")),
		quotes (Pattern.compile("'.*'"));
		
		Pattern pattern;
		
		Patterns(Pattern pattern) {
			this.pattern = pattern;
		}
	}
	
	public URLrewriter(String referer, Context context, String mTunnel, String attributesXpath){
		xpathApi = context.getXpathApi();
		targetReferer = referer;
		mHttpTunnel = mTunnel;
		this.attributesXpath = attributesXpath;
		HtmlConnector connector = (HtmlConnector)context.getConnector();
		
		try {
			if(targetReferer.equals("")) targetReferer = connector.getHtmlParser().getReferer(context);
		} catch (MaxCvsExceededException e1) {
			Engine.logEngine.warn("Unable to retrieve HtmlParser referer. Using connector base url.");
		} catch (KeyExpiredException e) {
			Engine.logEngine.warn("Unable to retrieve HtmlParser referer. Using connector base url.");
		}

		if(targetReferer.equals("")) targetReferer = connector.getBaseUrl();
		
		targetPathReferer = URLUtils.extractPathReferer(targetReferer);
		
		if (targetReferer.endsWith("/")) targetReferer = targetReferer.substring(0, targetReferer.length()-1);
		
		targetHost = URLUtils.extractHost(targetReferer);
		
		if(mHttpTunnel.equals(WebClipper.mHttptunnelOnCache) || mHttpTunnel.equals(WebClipper.mHttptunnelOnNoCache)){
			String requestedUrl = context.httpServletRequest.getRequestURL().toString();
			int index = requestedUrl.length();
			for(int i=0 ; i<3 ; i++) index = requestedUrl.lastIndexOf('/', index-1);
			servletUrl = requestedUrl.substring(0, index) + "/webclipper/" + context.projectName + "/" + context.connectorName + "/" + context.name + "/";
		}		
	}
	
	private boolean match(Patterns pattern, String s) {
		return pattern.pattern.matcher(s).find();
	}
	
	private String[] split(Patterns pattern, String s) {
		return pattern.pattern.split(s);
	}
	
	public void rewriteStyle(Element elt){
		try {
			NodeList list = xpathApi.selectNodeList(elt, ".//STYLE/text()");
			for(int j=0;j<list.getLength();j++){
				if(list.item(j) instanceof Text){
					Text node = (Text)list.item(j);
					node.setNodeValue(rewriteStyle(node.getNodeValue()));
				}
			}
		} catch (TransformerException e) {
			Engine.logEngine.trace("WebClipper cannot evaluate STYLE node");
		}
	}
	
	synchronized public String rewriteStyle(String txt, String filepath){
		String referer = targetReferer;
		String pathReferer = targetPathReferer;
		String host = targetHost;
		try{
			targetReferer = filepath;
			targetPathReferer = URLUtils.extractPathReferer(filepath);
			targetHost = URLUtils.extractHost(filepath, host);
			return rewriteStyle(txt);
		}finally{
			targetReferer = referer;
			targetPathReferer = pathReferer;
			targetHost = host;
		}
	}
	
	public String rewriteStyle(String txt){
		String[]lines = txt.split("\\n");
		for(int j=0;j<lines.length;j++)
			lines[j] = rewriteStyleLine(lines[j]);
		return StringUtils.join(lines, "\n");
	}
	
	public String rewriteStyleLine(String line){
		if (match(Patterns.importspace, line)) {
			String[] res = split(Patterns.importspace, line);
			line = res[0];
			String temp = "";
			for (int i = 1 ; i < res.length ; i++) {
				line += "@import ";
				temp = res[i];
				int max = temp.indexOf(';');
				int start = -1;
				String charOrder = "\"'(";
				for(int j=0;(start==-1 || start > max) && j<charOrder.length();j++)
					start = temp.indexOf(charOrder.charAt(j));
				line += temp.substring(0, start+1);
				line += rewriteURL(temp.substring(start+1)); 
			}
		}

		// handle any url(xxxx) type or url ('xxxx') type 
		Matcher matcher = Patterns.url.pattern.matcher(line);
		if (matcher.find()) {
			String temp = matcher.group();
			String[] res = split(Patterns.url, line);

			matcher = Patterns.paren.pattern.matcher(temp);
			if(matcher.find()){
				temp = matcher.group();
			}else{
				int idp = temp.indexOf(")");
				if(idp>4) temp = temp.substring(4, idp); //url(/blabla)
			}
			// test if there is a leading '
			if (match(Patterns.quotes, temp)) {
				temp = temp.substring(1, temp.length()-1);
			}
			line = res[0] + "url('" + rewriteURL(temp) + "') " + res[1];
		}
		return line;
	}
	
	public void rewriteStyleAttr(Element elt){
		try {
			NodeList list = xpathApi.selectNodeList(elt, ".//@style");
			for(int j=0;j<list.getLength();j++){
				if(list.item(j) instanceof Attr){
					Attr attr = (Attr)list.item(j);
					attr.setValue(rewriteStyle( attr.getValue()) );
				}
			}
		} catch (TransformerException e) {
			Engine.logEngine.trace("WebClipper cannot evaluate STYLE node");
		}
	}
	
	public String rewriteURL(String url){
		String newUrl;
		
		if(url.equals("#")) return url; // <a href="#"> for webclipping
		
		if(url.startsWith("/")) newUrl = targetHost+url;
		else if(url.startsWith("http://")||url.startsWith("https://")) newUrl = url;
		else if(url.startsWith("#") || url.startsWith("?")) newUrl = targetReferer+url;
		else newUrl = targetPathReferer+url;
		
		if(mHttpTunnel.equals(WebClipper.mHttptunnelOnCache)) newUrl = servletUrl+"c/?"+newUrl;
		else if(mHttpTunnel.equals(WebClipper.mHttptunnelOnNoCache)) newUrl = servletUrl+"n/?"+newUrl;
		
		return newUrl;
	}
	
	public void rewriteURL(Element elt){
		if(attributesXpath.length()==0) return;
		try {
			NodeList list = xpathApi.selectNodeList(elt, attributesXpath);
			for(int i = 0 ; i < list.getLength() ; i++) {
				Attr attr_n = (Attr)list.item(i);
				attr_n.setValue(rewriteURL(attr_n.getValue()));
			}
		} catch (TransformerException e) {
			Engine.logEngine.trace("WebClipper cannot evaluate this xpath : "+attributesXpath);
		}
	}
}