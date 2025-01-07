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

package com.twinsoft.convertigo.engine.oauth;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;

import oauth.signpost.AbstractOAuthConsumer;
import oauth.signpost.http.HttpRequest;

@SuppressWarnings("serial")
public class HttpOAuthConsumer extends AbstractOAuthConsumer {
	
	private HostConfiguration hc;

    public HttpOAuthConsumer(String consumerKey, String consumerSecret, HostConfiguration hostConfiguration) {
        super(consumerKey, consumerSecret);
        hc = hostConfiguration;
    }

    @Override
    protected HttpRequest wrap(Object request) {
        if (!(request instanceof HttpMethod)) {
            throw new IllegalArgumentException(
                    "This consumer expects requests of type "
                            + HttpMethod.class.getCanonicalName());
        }

        return new HttpMethodAdapter((HttpMethod)request, hc);
    }

}