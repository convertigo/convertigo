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

package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class DelegateServlet extends HttpServlet {
	private static final long serialVersionUID = 1996273484366589049L;
	
	private static class Singleton {
		long failed = 0;
		Map<String, String> map = new HashMap<>();
		String lastUrl = null;
		
		boolean canDelegate() {
			String url = EnginePropertiesManager.getProperty(PropertyName.DELEGATE_URL);
			
			if (StringUtils.isBlank(url) || (System.currentTimeMillis() < failed && url.equals(lastUrl))) {
				return false;
			}
			
			failed = 0;
			lastUrl = url;
			return true;
		}
		
		JSONObject delegate(JSONObject instruction) {
			JSONObject response = null;
			String token;
			synchronized (map) {
				do {
					token = RandomStringUtils.secure().nextAlphabetic(32);
				} while (map.containsKey(token));
				map.put(token, instruction.toString());
			}
			HttpGet get = new HttpGet(lastUrl + "&token=" + token);
			try (CloseableHttpResponse resp = Engine.theApp.httpClient4.execute(get)) {
				InputStream is = resp.getEntity().getContent();
				if ("gzip".equals(HeaderName.ContentEncoding.getHeader(resp))) {
					is = new GZIPInputStream(is);
				}
				String json = IOUtils.toString(is, "UTF-8");
				response = new JSONObject(json);
			} catch (Exception e) {
				Engine.logEngine.error("(DelegateServlet) failed to delegate to " + lastUrl, e);
				failed = System.currentTimeMillis() + 60000;
			}
			
			synchronized (map) {
				map.remove(token);
			}
			
			return response;
		}
		
		String peek(String token) {
			synchronized (map) {
				return map.remove(token);
			}
		}
	}
	
	private static Singleton singleton = new Singleton();


	public static boolean canDelegate() {
		return singleton.canDelegate();
	}
	
	public static JSONObject delegate(JSONObject instruction) {
		return singleton.delegate(instruction);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String token = req.getParameter("token");
		String content = singleton.peek(token);
		if (content != null) {
			resp.setCharacterEncoding("UTF-8");
			try (Writer w = resp.getWriter()) {
				w.write(content);
			}
		}
	}

}
