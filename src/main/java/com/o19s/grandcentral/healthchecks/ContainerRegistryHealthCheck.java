package com.o19s.grandcentral.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ContainerRegistryHealthCheck extends HealthCheck {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRegistryHealthCheck.class);

  private final String registryDomain;
  private final String project;
  private final String containerName;
  private final String username;
  private final String password;

  private CloseableHttpClient httpClient;
  private HttpClientContext httpContext;

  public ContainerRegistryHealthCheck(String keystorePath, String registryDomain, String project, String containerName, String username, String password) {
    this.registryDomain = registryDomain;
    this.project = project;
    this.containerName = containerName;
    this.username = username;
    this.password = password;

    try {
      // Setup SSL and plain connection socket factories
      SSLContext sslContext = SSLContexts.custom()
          .loadTrustMaterial(new File(keystorePath), "changeit".toCharArray())
          .build();

      LayeredConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
      PlainConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();

      Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
          .register("http", plainsf)
          .register("https", sslsf)
          .build();
      HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);

      // Build the HTTP Client
      this.httpClient = HttpClients.custom()
          .setConnectionManager(cm)
          .build();

      // Configure K8S HTTP Context (Authentication)
      this.httpContext = HttpClientContext.create();
      CredentialsProvider gcloudCredentialsProvider = new BasicCredentialsProvider();
      gcloudCredentialsProvider.setCredentials(
          new AuthScope(registryDomain, 443),
          new UsernamePasswordCredentials(username, password));
      httpContext.setCredentialsProvider(gcloudCredentialsProvider);
    } catch (Exception e) {
      LOGGER.error("Error configuring HTTP clients", e);
    }
  }

  @Override
  protected HealthCheck.Result check() throws Exception {
    HttpGet healthCheck = new HttpGet("https://" + this.registryDomain + ":443/v2/" + this.project + "/" + this.containerName + "/tags/list");

    // Force-add the Authorization header, (GCR.io will return a 404 if you're not authenticated instead of a 401 or 403)
    String username = httpContext.getCredentialsProvider().getCredentials(new AuthScope(this.registryDomain, 443)).getUserPrincipal().getName();
    String password = httpContext.getCredentialsProvider().getCredentials(new AuthScope(this.registryDomain, 443)).getPassword();

    byte[] rawAuthBytes = Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    healthCheck.addHeader("Authorization", "Basic " + new String(rawAuthBytes, StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpClient.execute(healthCheck, httpContext)) {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return HealthCheck.Result.healthy();
      } else {
        return HealthCheck.Result.unhealthy("Container registry not responding with a valid status code (" + response.getStatusLine().toString() + ")");
      }
    } catch (IOException ioe) {
      return HealthCheck.Result.unhealthy(ioe);
    }
  }
}
