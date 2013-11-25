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