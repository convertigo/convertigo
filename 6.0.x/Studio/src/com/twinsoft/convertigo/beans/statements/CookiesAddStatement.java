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

package com.twinsoft.convertigo.beans.statements;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class CookiesAddStatement extends Statement {
	private static final long serialVersionUID = -8541325182087905783L;
	
	private String expression = "//todo";
	
	public CookiesAddStatement() {
		super();
	}

	public CookiesAddStatement(String key, String expression) {
		super();
		this.expression = expression;
	}
	
	protected void addCookie(HttpState httpState, String cook){
		String name="";
		String domain="";
		String path="";
		String value="";
		boolean secure=false;
		Date expires = new Date(Long.MAX_VALUE);
				
		String[] fields = cook.split(";");
		for(int i=0;i<fields.length;i++){
			String[] half = fields[i].trim().split("=");
			if(half.length==2){
				if(fields[i].startsWith("$")){
					if(half[0].equals("$Domain")) domain=half[1];
					else if(half[0].equals("$Path")) path=half[1];
					else if(half[0].equals("$Secure")) secure=Boolean.getBoolean(half[1]);
					else if(half[0].equals("$Date"))
						try { expires=DateFormat.getDateTimeInstance().parse(half[1]);
						} catch (ParseException e) {}
				}else{
					name = half[0];
					value = half[1];
				}
			}
		}
		
		Cookie cookie = null;
		try{
			cookie = new Cookie(domain,name,value,path,expires,secure);
			if(cookie!=null) httpState.addCookie(cookie);
		}catch(Exception e){
			Engine.logBeans.debug("(CookiesAdd) failed to parse those cookies : "+cook);
		}
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		return "cookies.add(eval('"+(expression.length()<20?expression:expression.substring(0,20)+"...")+"'))"+(!text.equals("") ? " // "+text:"");
	}
	
	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				HttpConnector connector = this.getConnector();
				if(connector.handleCookie){
					HttpState httpState = this.getParentTransaction().context.httpState;
					if(httpState==null){
						connector.resetHttpState(this.getParentTransaction().context);
						httpState = this.getParentTransaction().context.httpState;
					}
					evaluate(javascriptContext, scope, expression, "CookiesGet", true);
					if(evaluated!=null){
						if(evaluated instanceof NativeArray){
							NativeArray array = (NativeArray)evaluated;
							long len = array.getLength();
							for(int i=0;i<len;i++)
								addCookie(httpState, array.get(i, array).toString());

						}else{
							addCookie(httpState, evaluated.toString());
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public String toJsString() {
		return "";
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
}