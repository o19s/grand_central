package com.o19s.grandcentral.dockercloud;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.o19s.grandcentral.LinkedContainerManager;
import com.o19s.grandcentral.http.HttpDelete; // IMPORTANT, allows DELETE requests with bodies
import com.o19s.grandcentral.kubernetes.Pod;

/**
 * Manages all stacks present within a namespace
 * 
 * Currently reusing the K8N Pod object.
 * Should be implementing some sort of interface along with PodManager!
 * Using the word Stack and Pod interchangable right now!
 */
public class StackManager implements LinkedContainerManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(StackManager.class);

  private long lastRefresh;
  private static final Map<String, Pod> pods = new HashMap<>();

  private DockercloudConfiguration dockercloudConfiguration;

  private long refreshIntervalInMs;
  private int maximumPodCount;

  private CloseableHttpClient httpClient;
  private HttpClientContext httpContext;

  private final JsonFactory jsonFactory = new JsonFactory();
  private final YAMLFactory yamlFactory = new YAMLFactory();
  private final ObjectMapper jsonObjectMapper = new ObjectMapper(jsonFactory);
  private final ObjectMapper yamlObjectMapper = new ObjectMapper(yamlFactory);
  private final ObjectNode podDefinition = null;

  static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
  static final Lock readLock = readWriteLock.readLock();
  static final Lock writeLock = readWriteLock.writeLock();

  /**
   * Instantiates a new manages with the specified settings
   * @param k8sConfiguration Kubernetes Configuration
   * @param keystorePath Path to the Java Keystore containing trusted certificates
   * @param maximumPodCount Maximum number of pods to ever have running at once
   * @param refreshIntervalInMs Interval with which to refresh the pods
   * @param podYamlPath the location of the yaml config for the application pod
   */
  public StackManager(DockercloudConfiguration dockercloudConfiguration,
  
                    long refreshIntervalInMs,
                    int maximumPodCount) throws IOException {
    lastRefresh = 0;

    this.dockercloudConfiguration = dockercloudConfiguration;

    this.refreshIntervalInMs = refreshIntervalInMs;
    this.maximumPodCount = maximumPodCount;

  //  podDefinition = jsonObjectMapper.createObjectNode();
//    podDefinition.setAll((ObjectNode) yamlObjectMapper.readTree(new File(podYamlPath)));

    LOGGER.info("Loaded Pod Definition: " /*+ podDefinition*/);

    try {
      // Setup SSL and plain connection socket factories
/*      SSLContext sslContext = null; SSLContexts.custom()
          .loadTrustMaterial(new File(keystorePath), "changeit".toCharArray())
          .build();
          */

  //    LayeredConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
    //  PlainConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
/*
      Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
          .register("http", plainsf)
          .register("https", sslsf)
          .build();
      HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);
      */

      // Build the HTTP Client
      httpClient = HttpClients.createDefault();
//      httpClient = HttpClients.custom()
  //        .setConnectionManager(cm)
    //      .build();

      // Configure K8S HTTP Context (Authentication)
      httpContext = HttpClientContext.create();
      CredentialsProvider dockercloudCredentialsProvider = new BasicCredentialsProvider();
      dockercloudCredentialsProvider.setCredentials(
          new AuthScope(dockercloudConfiguration.getHostname(), 80),
          new UsernamePasswordCredentials(dockercloudConfiguration.getUsername(), dockercloudConfiguration.getApikey()));
      httpContext.setCredentialsProvider(dockercloudCredentialsProvider);
      
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
    Pod pod = null;

    // Force a refresh of the data from K8S if the interval has passed
    if (DateTime.now().getMillis() - lastRefresh > refreshIntervalInMs) {
      refreshPods();
    }


    if (contains(dockerTag)) {
      readLock.lock();
      try {
        pod = pods.get(dockerTag);
      } finally {
        readLock.unlock();
      }
    }

    return pod;
  }

  /**
   * Does the provided dockerTag currently exist within the cluster
   * @param dockerTag Git hash / name of the pod to check
   * @return True if the pod exists
   */
  public Boolean contains(String dockerTag) {
    readLock.lock();
    boolean contains = false;

    try {
      contains = pods.containsKey(dockerTag);
    } finally {
      readLock.unlock();
    }

    return contains;
  }

  /**
   * Adds a pod with the docker tag
   * @param dockerTag Git hash / name of the pod to deploy
   */
  public Pod add(String dockerTag) throws Exception {
    if (!contains(dockerTag)) {
      Pod pod = null;

      // Get the read lock
      readLock.lock();

      try {
    	  
    	  ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    	  
    	  String s = String.join("\n"
    		         , "{"
    		         , "	\"name\": \"" + dockercloudConfiguration.getNamespace() + "-" + dockerTag + "\","
    		         , "	\"services\": ["
    		         , "		{"
    		         , "			\"name\":\"hello-world\","  
    		         , "			\"image\":\"dep4b/datastart:v1\","  
    		         ,"				\"ports\": ["
    		         , "				\"81:8080\""      		         
    		         , "			]"    		         
    		         , "		}"    		         
    		         , "	]"
    		         , "}"
    		);
    	  
    	  baos.write(s.getBytes());
    	  
        // Schedule the new Pod
/*       
        JsonGenerator generator = jsonFactory.createGenerator(baos);

        ObjectNode newPodDefinition = podDefinition.deepCopy();
        ((ObjectNode) newPodDefinition.get("metadata")).put("name", dockerTag);
        String image;
        for (JsonNode containerNode : newPodDefinition.get("spec").get("containers")) {
          image = containerNode.get("image").asText();
          if (image.endsWith("__DOCKER_TAG__")) {
            ((ObjectNode) containerNode).put("image", image.replace("__DOCKER_TAG__", dockerTag));
          }
        }

        LOGGER.info("Generated definition for \"" + dockerTag + "\": " + newPodDefinition);

        generator.writeObject(newPodDefinition);
        generator.flush();
        generator.close();
*/
    	  HttpPost stackCreate = new HttpPost(dockercloudConfiguration.getProtocol() + "://" + dockercloudConfiguration.getHostname() + "/api/app/v1/stack/");
    	  stackCreate.addHeader("accept", "application/json");
    	  stackCreate.addHeader(BasicScheme.authenticate(
    	    		 new UsernamePasswordCredentials(dockercloudConfiguration.getUsername(), dockercloudConfiguration.getApikey()),
    	    		 "UTF-8", false));


    	HttpEntity podJson = new ByteArrayEntity(baos.toByteArray());
    	stackCreate.setEntity(podJson);

    	String podUUID = null;
        try (CloseableHttpResponse response = httpClient.execute(stackCreate)) {
          int status = response.getStatusLine().getStatusCode();
          HttpEntity entity = response.getEntity();
          InputStream responseBody = entity.getContent();
          	
              JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
              JsonNode objectsNode = rootNode.get("objects");
          if (status == HttpStatus.SC_CREATED) {
            LOGGER.info("Pod " + dockerTag + ": Scheduled");     
            podUUID = rootNode.get("uuid").asText();
            
          } else if (status == HttpStatus.SC_CONFLICT) {
            LOGGER.info("Pod " + dockerTag + ": Already running");
          } else {
            LOGGER.info("Pod " + dockerTag + ": Not scheduled (" + response.getStatusLine().toString() + ")");
          }
        } catch (IOException ioe) {
          LOGGER.error("Pod " + dockerTag + ": Error scheduling pod", ioe);
        }

        // Wait until Pod is running
        boolean podRunning = false;
        boolean podCreated = true;
        
        podRunning = true;
        // Here should be a check to see if it the stack was created, not sure how long it takes!!!
        
        
        if (podCreated && podUUID != null){
        	// Start the stack
        	HttpPost stackStart = new HttpPost(dockercloudConfiguration.getProtocol() + "://" + dockercloudConfiguration.getHostname() + "/api/app/v1/stack/" + podUUID + "/start/");
        	stackStart.addHeader("accept", "application/json");
        	stackStart.addHeader(BasicScheme.authenticate(
      	    		 new UsernamePasswordCredentials(dockercloudConfiguration.getUsername(), dockercloudConfiguration.getApikey()),
      	    		 "UTF-8", false));
        
      	try (CloseableHttpResponse response = httpClient.execute(stackStart)) {
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            InputStream responseBody = entity.getContent();
            JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
      	}
      	catch (IOException ioe) {
            LOGGER.error("Pod " + dockerTag + ": Error scheduling pod", ioe);
          }
      	
        
        }
        
        

        
        do {
            HttpGet stackStatus = new HttpGet(dockercloudConfiguration.getProtocol() + "://" + dockercloudConfiguration.getHostname() + "/api/app/v1/stack/" + podUUID);
            stackStatus.addHeader("accept", "application/json");
            stackStatus.addHeader(BasicScheme.authenticate(
            		 new UsernamePasswordCredentials(dockercloudConfiguration.getUsername(), dockercloudConfiguration.getApikey()),
            		 "UTF-8", false));

          LOGGER.info("Pod " + dockerTag + ": Waiting for start");
          Thread.sleep(1000);

          try (CloseableHttpResponse response = httpClient.execute(stackStatus)) {
            HttpEntity entity = response.getEntity();
            try (InputStream responseBody = entity.getContent()) {
            	
    
               JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
               String name = rootNode.get("name").asText();
    	       String status = rootNode.get("state").asText();
    	       JsonNode servicesNode = rootNode.get("services");

    	        
    	       String serviceURI = servicesNode.get(0).asText();
    	       String publicDNS = getDNSForStack(serviceURI);
    	        
    	      pod = new Pod(dockerTag, publicDNS, status);
    	        
              podRunning = pod != null && pod.isRunning();
            } catch (IOException ioe) {
              LOGGER.error("Pod " + dockerTag + ": Error getting DNS for pod", ioe);
            }
          }
        } while (!podRunning);

        LOGGER.info("Pod " + dockerTag + ": Started");
      } finally {
        readLock.unlock();
      }

      // Force a refresh of the pod list
      refreshPods();

      return pod;
    }

    return null;
  }

  /**
   * Stops the pod containing the specified docker tag. Note this does not force a refresh of the pods state
   * @param dockerTag
   * @throws IOException
   */
  private void remove(String dockerTag) throws IOException {
    if (contains(dockerTag)) {
      readLock.lock();

      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = jsonFactory.createGenerator(baos);

        ObjectNode root = jsonObjectMapper.createObjectNode();
        root.put("gracePeriodSeconds", 0);

        generator.writeObject(root);
        generator.flush();

        HttpDelete podDelete = new HttpDelete("https://" /*+ k8sConfiguration.getMasterIp()*/ + ":443/api/v1/namespaces/" /*+ k8sConfiguration.getNamespace() */+ "/pods/" + dockerTag);
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
      } finally {
        readLock.unlock();
      }
    } else {
      throw new IllegalArgumentException("Pod doesn't exist");
    }
  }

  /**
   * Removes the oldest running pods until maximumPodCount is reached
   * @throws IOException
   */
  private void removeExtraPods() throws IOException {
    readLock.lock();
    if (pods.size() > maximumPodCount) {
      readLock.unlock();
      writeLock.lock();

      try {
        // Check again since there was a time where we didn't have the lock
        if (pods.size() > maximumPodCount) {
          LOGGER.info("Removing extra pods");

          // Determine the pods to remove
          Pod[] sortedPodsByRequestAge = null;
          sortedPodsByRequestAge = pods.values().toArray(new Pod[pods.size()]);
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

          // Remove the pods
          int amountToRemove = sortedPodsByRequestAge.length - maximumPodCount;
          for (int i = 0; i < amountToRemove; i++) {
            remove(sortedPodsByRequestAge[i].getDockerTag());
          }
        }
      } finally {
        writeLock.unlock();
      }

      refreshPods();
    } else {
      readLock.unlock();
    }
  }

  /**
   * Refreshes the internal map which tracks all running pods
   * @throws IOException
   */
  private void refreshPods() throws IOException {
    HttpGet stacksGet = new HttpGet(dockercloudConfiguration.getProtocol() + "://" + dockercloudConfiguration.getHostname() + "/api/app/v1/stack/");
    stacksGet.addHeader("accept", "application/json");
    stacksGet.addHeader(BasicScheme.authenticate(
    		 new UsernamePasswordCredentials(dockercloudConfiguration.getUsername(), dockercloudConfiguration.getApikey()),
    		 "UTF-8", false));

//    try (CloseableHttpResponse response = httpClient.execute(stacksGet, httpContext)) {
    try (CloseableHttpResponse response = httpClient.execute(stacksGet)) {
    	
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        // Grab the write lock
    	 System.out.println("writelock:" + writeLock.toString());
        writeLock.lock();

        try (InputStream responseBody = entity.getContent()) {
        	
          JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
          JsonNode objectsNode = rootNode.get("objects");

          // Update our internal pod hash
          Set<String> toDelete = new HashSet<>(pods.size());
          toDelete.addAll(pods.keySet());
          for (int i = 0; i < objectsNode.size(); i++) {
        	  Pod pod = null;
        	  String name = objectsNode.get(i).get("name").asText();
        	  String dockerTag = null;
        	  String podName = name;
        	  String state = objectsNode.get(i).get("state").asText();
        	  String servicesURI = objectsNode.get(i).get("services").get(0).asText();
        	  
        	  if (name.indexOf("-")> -1){
        		  dockerTag = name.split("-")[1];        		  
        	  }
        	  
        	  if (dockerTag != null && servicesURI != "" && podName.contains(dockercloudConfiguration.getNamespace())){
        		  
        		  String publicDNS = getDNSForStack(servicesURI);
        		  
        		  pod = new Pod(dockerTag, publicDNS, state);
        	  }
        	  
        	  

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

          // Delete pods that have been removed (delete refers to our hash, not k8s). This calls remove on the hash, not the manager.
//          toDelete.forEach((dockerTag) -> LOGGER.info("Refresh: Removing pod " + dockerTag + " from internal hash"));
//          toDelete.forEach(pods::remove);
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
//    removeExtraPods();

    // Update the lastRefresh time
    lastRefresh = DateTime.now().getMillis();
  }
  
  private String getDNSForStack(String serviceURI){
	  String publicDNS = null;
	  HttpGet stackServices = new HttpGet(dockercloudConfiguration.getProtocol() + "://" + dockercloudConfiguration.getHostname() + serviceURI);
      stackServices.addHeader("accept", "application/json");
      stackServices.addHeader(BasicScheme.authenticate(
      		 new UsernamePasswordCredentials(dockercloudConfiguration.getUsername(), dockercloudConfiguration.getApikey()),
      		 "UTF-8", false));
      try (CloseableHttpResponse response2 = httpClient.execute(stackServices)) {
          HttpEntity entity2 = response2.getEntity();
          try (InputStream responseBody2 = entity2.getContent()) {
          	
   
              JsonNode rootNode2 = jsonObjectMapper.readTree(responseBody2);
               publicDNS = rootNode2.get("public_dns").asText();
  	         	                    
          } catch (IOException ioe) {
            LOGGER.error("Pod at " + serviceURI + ": Error starting pod", ioe);
          }
        
      
  } catch (IOException ioe) {
    LOGGER.error("Pod " + serviceURI + ": Error getting public dns for pod", ioe);
  }
      return publicDNS;
  
  }
}
