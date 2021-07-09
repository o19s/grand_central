package com.o19s.grandcentral.gcloud;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

import com.o19s.grandcentral.ImageRegistry;

/**
 * Created by cbradford on 1/18/16.
 */
public class GCloudRegistry implements ImageRegistry {
  private CloseableHttpClient httpClient;
  private HttpClientContext httpContext;
  private GCloudConfiguration config;

  public GCloudRegistry(GCloudConfiguration config, String keystorePath) throws Exception {
    this.config = config;

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
    httpClient = HttpClients.custom()
        .setConnectionManager(cm)
        .build();

    // Configure GCloud HTTP Context (Authentication)
    httpContext = HttpClientContext.create();
    CredentialsProvider gcloudCredentialsProvider = new BasicCredentialsProvider();
    gcloudCredentialsProvider.setCredentials(
        new AuthScope(config.getRegistryDomain(), 443),
        new UsernamePasswordCredentials(
            config.getRegistryUsername(),
            config.getRegistryPassword()
        )
    );
    httpContext.setCredentialsProvider(gcloudCredentialsProvider);
  }

  /* (non-Javadoc)
 * @see com.o19s.grandcentral.gcloud.ImageRegistry#imageExistsInRegistry(java.lang.String)
 */
@Override
public boolean imageExistsInRegistry(String dockerTag) throws Exception {
    // Verify the image is available from GCR.io
    HttpGet verificationGet = new HttpGet("https://" + config.getRegistryDomain() + ":443/v2/"
        + config.getProject() + "/" + config.getContainerName() + "/manifests/" + dockerTag);

    // Force-add the Authorization header, (GCR.io will return a 404 if you're not authenticated instead of a 401 or 403)
    String username = httpContext.getCredentialsProvider().getCredentials(new AuthScope(config.getRegistryDomain(), 443)).getUserPrincipal().getName();
    String password = httpContext.getCredentialsProvider().getCredentials(new AuthScope(config.getRegistryDomain(), 443)).getPassword();

    byte[] rawAuthBytes = Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    verificationGet.addHeader("Authorization", "Basic " + new String(rawAuthBytes, StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpClient.execute(verificationGet, httpContext)) {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return true;
      } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        return false;
      } else {
        throw new Exception("Exception verifying image.");
      }
    }
  }
}
