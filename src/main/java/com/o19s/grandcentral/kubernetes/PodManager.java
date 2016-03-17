package com.o19s.grandcentral.kubernetes;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import com.o19s.grandcentral.http.HttpDelete; // IMPORTANT
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages all pods present within a namespace
 */
public class PodManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PodManager.class);

  private long lastRefresh;
  private static final Map<String, Pod> pods = new HashMap<>();

  private KubernetesConfiguration k8sConfiguration;

  private long refreshIntervalInMs;
  private int maximumPodCount;

  private CloseableHttpClient httpClient;
  private HttpClientContext httpContext;

  private final JsonFactory jsonFactory = new JsonFactory();
  private final YAMLFactory yamlFactory = new YAMLFactory();
  private final ObjectMapper jsonObjectMapper = new ObjectMapper(jsonFactory);
  private final ObjectMapper yamlObjectMapper = new ObjectMapper(yamlFactory);
  private final ObjectNode podDefinition;

  static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
  static final Lock readLock = readWriteLock.readLock();
  static final Lock writeLock = readWriteLock.writeLock();

  /**
   * Instantiates a new manages with the specified settings
   * @param k8sConfiguration Kubernetes Configuration
   * @param keystorePath Path to the Java Keystore containing trusted certificates
   * @param maximumPodCount Maximum number of pods to ever have running at once
   * @param refreshIntervalInMs Interval with which to refresh the pods
   */
  public PodManager(KubernetesConfiguration k8sConfiguration,
                    String keystorePath,
                    long refreshIntervalInMs,
                    int maximumPodCount, String podYamlPath) throws IOException {
    lastRefresh = 0;

    this.k8sConfiguration = k8sConfiguration;

    this.refreshIntervalInMs = refreshIntervalInMs;
    this.maximumPodCount = maximumPodCount;

    podDefinition = jsonObjectMapper.createObjectNode();
    podDefinition.setAll((ObjectNode) yamlObjectMapper.readTree(new File(podYamlPath)));

    LOGGER.info("Loaded Pod Definition: " + podDefinition);

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
      httpClient = HttpClients.custom()
          .setConnectionManager(cm)
          .build();

      // Configure K8S HTTP Context (Authentication)
      httpContext = HttpClientContext.create();
      CredentialsProvider k8sCredentialsProvider = new BasicCredentialsProvider();
      k8sCredentialsProvider.setCredentials(
          new AuthScope(k8sConfiguration.getMasterIp(), 443),
          new UsernamePasswordCredentials(k8sConfiguration.getUsername(), k8sConfiguration.getPassword()));
      httpContext.setCredentialsProvider(k8sCredentialsProvider);
    } catch (Exception e) {
      LOGGER.error("Error configuring HTTP clients", e);
    }

    // Initial loading of pod information
    refreshPods();
  }

  /**
   * Get pod information for the given name
   * @param dockerTag Git hash / name of the pod to return
   * @return The pod which matches the given key.
   */
  public Pod get(String dockerTag) throws IOException {
    // Force a refresh of the data from K8S if the interval has passed
    if (DateTime.now().getMillis() - lastRefresh > refreshIntervalInMs) {
      refreshPods();
    }

    readLock.lock();
    Pod pod = pods.get(dockerTag);
    readLock.unlock();

    return pod;
  }

  /**
   * Does the provided dockerTag currently exist within the cluster
   * @param dockerTag Git hash / name of the pod to check
   * @return True if the pod exists
   */
  public Boolean contains(String dockerTag) {
    readLock.lock();
    boolean contains = pods.containsKey(dockerTag);
    readLock.unlock();

    return contains;
  }

  /**
   * Adds a pod with the docker tag
   * @param dockerTag Git hash / name of the pod to deploy
   */
  public Pod add(String dockerTag) throws Exception {
    if (!contains(dockerTag)) {
      // Get the read lock
      readLock.lock();

      // Schedule the new Pod
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JsonGenerator generator = jsonFactory.createGenerator(baos);

      ObjectNode newPodDefinition = podDefinition.deepCopy();
      ((ObjectNode)newPodDefinition.get("metadata")).put("name", dockerTag);
      String image;
      for (JsonNode containerNode : newPodDefinition.get("spec").get("containers")) {
        image = containerNode.get("image").asText();
        if (image.endsWith("__DOCKER_TAG__")) {
          ((ObjectNode) containerNode).put("image", image.replace("__DOCKER_TAG__", dockerTag));
        }
      }

      LOGGER.info("Generated definition for \"" + dockerTag +"\": " + newPodDefinition);

      generator.writeObject(newPodDefinition);
      generator.flush();
      generator.close();

      HttpPost podSchedule = new HttpPost("https://" + k8sConfiguration.getMasterIp() + ":443/api/v1/namespaces/" + k8sConfiguration.getNamespace() + "/pods");
      HttpEntity podJson = new ByteArrayEntity(baos.toByteArray());
      podSchedule.setEntity(podJson);

      try (CloseableHttpResponse response = httpClient.execute(podSchedule, httpContext)) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
          LOGGER.info("Pod " + dockerTag + ": Scheduled");
        } else {
          LOGGER.info("Pod " + dockerTag + ": Not scheduled (" + response.getStatusLine().toString() + ")");
        }
      } catch (IOException ioe) {
        LOGGER.error("Pod " + dockerTag + ": Error scheduling pod", ioe);
      }

      // Wait until Pod is running
      boolean podRunning = false;
      Pod pod = null;
      HttpGet podStatusGet = new HttpGet("https://" + k8sConfiguration.getMasterIp() + ":443/api/v1/namespaces/" + k8sConfiguration.getNamespace() + "/pods/" + dockerTag);
      do {
        LOGGER.info("Pod " + dockerTag + ": waiting for start");
        Thread.sleep(1000);

        try (CloseableHttpResponse response = httpClient.execute(podStatusGet, httpContext)) {
          HttpEntity entity = response.getEntity();
          try (InputStream responseBody = entity.getContent()) {
            pod = parsePod(jsonObjectMapper.readTree(responseBody));

            podRunning = pod != null && pod.isRunning();
          } catch (IOException ioe) {
            LOGGER.error("Pod " + dockerTag + ": Error checking pod status", ioe);
          }
        }
      } while (!podRunning);

      LOGGER.info("Pod " + dockerTag + ": Started");

      readLock.unlock();

      // Force a refresh of the pod list
      refreshPods();

      return pod;
    }

    return null;
  }

  private void remove(String dockerTag) throws IOException {
    if (contains(dockerTag)) {
      readLock.lock();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JsonGenerator generator = jsonFactory.createGenerator(baos);

      ObjectNode root = jsonObjectMapper.createObjectNode();
      root.put("gracePeriodSeconds", 0);

      generator.writeObject(root);
      generator.flush();

      HttpDelete podDelete = new HttpDelete("https://" + k8sConfiguration.getMasterIp() + ":443/api/v1/namespaces/" + k8sConfiguration.getNamespace() + "/pods/" + dockerTag);
      podDelete.setEntity(new ByteArrayEntity(baos.toByteArray()));

      try (CloseableHttpResponse response = httpClient.execute(podDelete, httpContext)) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          LOGGER.info("Pod " + dockerTag + ": Removed");
        } else {
          LOGGER.info("Pod " + dockerTag + ": Error removing pod (" + response.getStatusLine().toString() + ")");
        }
      } catch (IOException ioe) {
        LOGGER.error("Pod " + dockerTag + ": Error removing pod", ioe);
      }
    } else {
      throw new IllegalArgumentException("Pod doesn't exist");
    }

    readLock.unlock();

    refreshPods();
  }

  private void removeExtraPods() throws IOException {
    if (pods.size() > maximumPodCount) {
      LOGGER.info("Removing extra pods");

      readLock.lock();
      Pod[] sortedPodsByRequestAge = pods.values().toArray(new Pod[pods.size()]);
      Arrays.sort(
          sortedPodsByRequestAge,
          (Pod left, Pod right) -> {
            if (left.getLastRequest() > right.getLastRequest())
              return 1;
            else if (left.getLastRequest() < right.getLastRequest())
              return -1;
            else
              return 0;
          }
      );
      readLock.unlock();

      writeLock.lock();
      int amountToRemove = sortedPodsByRequestAge.length - maximumPodCount;
      for (int i = 0; i < amountToRemove; i++) {
        remove(sortedPodsByRequestAge[i].getDockerTag());
      }
      writeLock.unlock();
    }
  }

  private void refreshPods() throws IOException {
    HttpGet podsGet = new HttpGet("https://" + k8sConfiguration.getMasterIp() + ":443/api/v1/namespaces/" + k8sConfiguration.getNamespace() + "/pods");

    try (CloseableHttpResponse response = httpClient.execute(podsGet, httpContext)) {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        try (InputStream responseBody = entity.getContent()) {
          JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
          JsonNode itemsNode = rootNode.get("items");

          // Grab the write lock
          writeLock.lock();

          // Update our internal pod hash
          Set<String> toDelete = new HashSet<>(pods.size());
          toDelete.addAll(pods.keySet());
          for (int i = 0; i < itemsNode.size(); i++) {
            Pod pod = parsePod(itemsNode.get(i));

            if (pod != null && pod.isRunning()) {
              // The pod is valid and should be managed
              if (pods.containsKey(pod.getDockerTag())) {
                LOGGER.info("Refresh: Updating pod " + pod.getDockerTag() + " in internal hash");

                // Update the pod's address
                pods.get(pod.getDockerTag()).setAddress(pod.getAddress());

                // Remove the pending delete task for pods that exist
                toDelete.remove(pod.getDockerTag());
              } else {
                LOGGER.info("Refresh: Adding pod " + pod.getDockerTag() + " to internal hash");
                pods.put(pod.getDockerTag(), pod);
              }
            }
          }

          // Delete pods that have been removed (delete refers to our hash, not k8s)
          toDelete.forEach((dockerTag) -> LOGGER.info("Refresh: Removing pod " + dockerTag + " from internal hash"));
          toDelete.forEach(pods::remove);
        } catch (IOException ioe) {
          LOGGER.error("Pod Refresh: Error parsing pods", ioe);
        } finally {
          writeLock.unlock();
        }
      }
    } catch (IOException ioe) {
      LOGGER.error("Pod Refresh: Error retrieving pods", ioe);
    }

    // Cleanup old pods
    removeExtraPods();

    // Update the lastRefresh time
    lastRefresh = DateTime.now().getMillis();
  }

  private Pod parsePod(JsonNode podNode) {
    if (podNode != null) {
      String name = podNode.get("metadata").get("name").asText();
      String status = podNode.get("status").get("phase").asText();
      String podIP = status.equals("Running") ? podNode.get("status").get("podIP").asText() : "";

      return new Pod(name, podIP, status);
    } else {
      return null;
    }
  }
}
