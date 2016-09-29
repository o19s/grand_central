package com.o19s.grandcentral.servlets;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.o19s.grandcentral.ImageRegistry;
import com.o19s.grandcentral.LinkedContainerManager;
import com.o19s.grandcentral.kubernetes.Pod;

/**
 * Filter which drops requests that do not match the appropriate host header format.
 */
public class PodServletFilter implements javax.servlet.Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(PodServletFilter.class);

  private String grandCentralDomain;
  private LinkedContainerManager podManager;
  private ImageRegistry gCloudRegistry;

  /**
   *
   * @param grandCentralDomain The domain grand central is running on. This helps determine the portion of the URL representing the Git hash.
   * @param podManager
   */
  public PodServletFilter(String grandCentralDomain, LinkedContainerManager podManager, ImageRegistry gCloudRegistry) {
    this.grandCentralDomain = grandCentralDomain;
    this.podManager = podManager;
    this.gCloudRegistry = gCloudRegistry;
  }

  public void init(FilterConfig filterConfig) throws ServletException {}

  /**
   * Performs the filtering logic. Valid "Host" headers are prefixed with the docker tag to route against. For example
   * if Grand Central is running at *.gc.o19s.com, a valid domain is {docker tag}.gc.o19s.com.
   * @param servletRequest request being processed
   * @param servletResponse response use when evaluating / processing the request
   * @param filterChain used when forwarding the request along on a successful check
   * @throws IOException
   * @throws ServletException
   */
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    if (servletRequest instanceof HttpServletRequest) {
      HttpServletRequest request = (HttpServletRequest) servletRequest;

      String host = request.getHeader("Host");
      String hostWithoutPort, dockerTag;
      // FIXME: if the host is null, should GC blow up?   Can you have a null host?
      if (host != null && host.contains(":")) {
        hostWithoutPort = host.substring(0, host.indexOf(":"));
      } else {
        hostWithoutPort = host;
      }

      if (hostWithoutPort != null) {
        dockerTag = hostWithoutPort.replace("." + this.grandCentralDomain, "");
      } else {
        dockerTag = null;
      }

      try {
        if (host != null && host.contains(this.grandCentralDomain)) {
          // Validate Host header
          if (dockerTag != null && !(podManager.contains(dockerTag) || gCloudRegistry.imageExistsInRegistry(dockerTag))) {
            return_error(servletResponse, HttpStatus.NOT_FOUND_404, "Docker tag not found");
          } else {
            Pod pod;
            if (podManager.contains(dockerTag)) {
              pod = podManager.get(dockerTag);
            } else {
              pod = podManager.add(dockerTag);
            }

            if (pod != null) {
              LOGGER.info("Pod " + pod.getDockerTag() + " found!");

              request.setAttribute("proxyTo", pod.getAddress() + ":" + pod.getPodPort());
              filterChain.doFilter(servletRequest, servletResponse);
            } else {
              return_error(servletResponse, HttpStatus.INTERNAL_SERVER_ERROR_500, "Error processing pod");
            }
          }
        } else {
          LOGGER.info("Host:" + host + ", dockerTag:" + dockerTag + "," + "grandCentralDomain:" + this.grandCentralDomain);
          return_error(servletResponse, HttpStatus.BAD_REQUEST_400, "Host Header was not specified or is invalid:" +"Host:" + host + ", dockerTag:" + dockerTag + "," + "grandCentralDomain:" + this.grandCentralDomain);
        }
      } catch (Exception e) {
        LOGGER.error("Exception filtering request", e);

        return_error(servletResponse, HttpStatus.INTERNAL_SERVER_ERROR_500, "Error validating header");
      }
    }
  }

  /**
   * Returns an error to the the provided servletResponse with the provide HTTP status code and message.
   * @param servletResponse Response object to send the response with.
   * @param status HTTP Status code. {@see org.eclipse.jetty.http.HttpStatus}
   * @param message Message to return to the client
   * @throws IOException
   */
  private void return_error(ServletResponse servletResponse, int status, String message) throws IOException {
    LOGGER.error(message);

    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    httpResponse.setStatus(status);
    httpResponse.getWriter().print(message);
  }

  public void destroy() {}
}
