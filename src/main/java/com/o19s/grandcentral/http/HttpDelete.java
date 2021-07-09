package com.o19s.grandcentral.http;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

/**
 * HTTP Delete request with a request body
 */
public class HttpDelete extends HttpEntityEnclosingRequestBase {
  public static final String METHOD_NAME = "DELETE";

  public String getMethod() {
    return METHOD_NAME;
  }

  public HttpDelete(final String uri) {
    super();
    setURI(URI.create(uri));
  }

  public HttpDelete(final URI uri) {
    super();
    setURI(uri);
  }

  public HttpDelete() {
    super();
  }
}
