package com.o19s.grandcentral.servlets;

import org.eclipse.jetty.proxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;

/**
 * Proxies requests to Pods running within the Kubernetes cluster.
 */
public class PodProxyServlet extends ProxyServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(PodProxyServlet.class);

  private final int podPort;

  public PodProxyServlet(int podPort) {
    this.podPort = podPort;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  @Override
  protected String rewriteTarget(HttpServletRequest request) {
    String path = request.getRequestURI();

    StringBuilder uri = new StringBuilder("http://" + request.getAttribute("proxyTo") + ":" + podPort);
    uri.append(path);

    String query = request.getQueryString();
    if (query != null)
    {
      // Is there at least one path segment ?
      String separator = "://";
      if (uri.indexOf("/", uri.indexOf(separator) + separator.length()) < 0)
        uri.append("/");
      uri.append("?").append(query);
    }
    URI rewrittenURI = URI.create(uri.toString()).normalize();

    if (!this.validateDestination(rewrittenURI.getHost(), rewrittenURI.getPort()))
      return null;

    LOGGER.debug(uri.toString());

    return rewrittenURI.toString();
  }
}
