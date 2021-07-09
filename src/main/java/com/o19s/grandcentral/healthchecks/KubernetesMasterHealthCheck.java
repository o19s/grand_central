package com.o19s.grandcentral.healthchecks;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLContext;

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

import com.codahale.metrics.health.HealthCheck;

public class KubernetesMasterHealthCheck extends HealthCheck {
  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesMasterHealthCheck.class);

  private final String kubernetesMasterIp;
  private final String namespace;
  private CloseableHttpClient httpClient;
  private HttpClientContext httpContext;

  public KubernetesMasterHealthCheck(String masterIp, String keystorePath, String username, String password, String namespace) {
    this.kubernetesMasterIp = masterIp;
    this.namespace = namespace;

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
      CredentialsProvider k8sCredentialsProvider = new BasicCredentialsProvider();
      k8sCredentialsProvider.setCredentials(
          new AuthScope(this.kubernetesMasterIp, 443),
          new UsernamePasswordCredentials(username, password));
      httpContext.setCredentialsProvider(k8sCredentialsProvider);
    } catch (Exception e) {
      LOGGER.error("Error configuring HTTP clients", e);
    }
  }

  @Override
  protected Result check() throws Exception {
    HttpGet healthCheck = new HttpGet("https://" + kubernetesMasterIp + ":443/api/v1/namespaces/" + namespace + "/pods");
    try (CloseableHttpResponse response = httpClient.execute(healthCheck, httpContext)) {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return Result.healthy();
      } else {
        return Result.unhealthy("Kubernetes master not responding with a valid status code (" + response.getStatusLine().toString() + ")");
      }
    } catch (IOException ioe) {
      return Result.unhealthy(ioe);
    }
  }
}
