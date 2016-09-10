package com.o19s.grandcentral.dockercloud;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.o19s.grandcentral.ImageRegistry;

public class DockercloudRegistry implements ImageRegistry {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(DockercloudRegistry.class);
	private DockercloudConfiguration dockercloudConfiguration;
	private CloseableHttpClient httpClient;
	private final JsonFactory jsonFactory = new JsonFactory();
	private final ObjectMapper jsonObjectMapper = new ObjectMapper(jsonFactory);

	public DockercloudRegistry(DockercloudConfiguration dockercloudConfiguration) {
		this.dockercloudConfiguration = dockercloudConfiguration;

		httpClient = HttpClients.createDefault();
	}

	@Override
	public boolean imageExistsInRegistry(String dockerTag) throws Exception {
		
		LOGGER.info("Checking if Docker tag exists in registry: " + dockerTag);
		
		boolean imageExists = false;
		String podUUID = null;

		String serviceName = "chk-" + dockerTag;

		String imageName = dockercloudConfiguration.getStackExistsTestImage() + ":" + dockerTag;
		
		podUUID = createValidityCheckService(serviceName, imageName);
		
		imageExists = startService(podUUID);

		deleteService(podUUID);

		return imageExists;
	}
	
	private String createValidityCheckService(String serviceName, String imageName){
		String podUUID = null;
		try {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			String s = String.join("\n", "{", "	\"name\": \"" + serviceName
					+ "\",", "	\"image\": \"" + imageName + "\""
					+ ",", "	\"run_command\": \"ls\""
					, "}");
			
			LOGGER.info("Checking for " + imageName + " via " + s);
			baos.write(s.getBytes());

			HttpPost serviceCreate = new HttpPost(
					dockercloudConfiguration.getProtocol() + "://"
							+ dockercloudConfiguration.getHostname()
							+ "/api/app/v1/service/");
			serviceCreate.addHeader("accept", "application/json");
			serviceCreate.addHeader(BasicScheme.authenticate(
					new UsernamePasswordCredentials(dockercloudConfiguration
							.getUsername(), dockercloudConfiguration
							.getApikey()), "UTF-8", false));

			HttpEntity podJson = new ByteArrayEntity(baos.toByteArray());
			serviceCreate.setEntity(podJson);

			try (CloseableHttpResponse response = httpClient
					.execute(serviceCreate)) {
				int status = response.getStatusLine().getStatusCode();
				HttpEntity entity = response.getEntity();
				InputStream responseBody = entity.getContent();

				JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
				JsonNode objectsNode = rootNode.get("objects");
				if (status == HttpStatus.SC_CREATED) {
					LOGGER.info("Pod " + serviceName + ": Scheduled");
					podUUID = rootNode.get("uuid").asText();

				} else if (status == HttpStatus.SC_CONFLICT) {
					LOGGER.info("Pod " + serviceName + ": Already running");
				} else {
					LOGGER.info("Pod " + serviceName + ": Not scheduled ("
							+ response.getStatusLine().toString() + ":" + rootNode+  ")");
				}
			} catch (IOException ioe) {
				LOGGER.error("Pod " + serviceName + ": Error scheduling pod", ioe);
			}
		} catch (IOException ioe) {
			LOGGER.error("Pod " + serviceName + ": Error scheduling pod", ioe);
		}		
		return podUUID;
	}

	private void deleteService(String podUUID) {

		HttpDelete serviceDelete = new HttpDelete(
				dockercloudConfiguration.getProtocol() + "://"
						+ dockercloudConfiguration.getHostname()
						+ "/api/app/v1/service/" + podUUID + "/");
		serviceDelete.addHeader("accept", "application/json");
		serviceDelete.addHeader(BasicScheme.authenticate(
				new UsernamePasswordCredentials(dockercloudConfiguration
						.getUsername(), dockercloudConfiguration.getApikey()),
				"UTF-8", false));

		try (CloseableHttpResponse response = httpClient.execute(serviceDelete)) {
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			InputStream responseBody = entity.getContent();

			JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
			
			if (status == HttpStatus.SC_ACCEPTED) {
				LOGGER.info("Pod " + podUUID + ": Deleted");

			}

		} catch (IOException ioe) {
			LOGGER.error("Pod " + podUUID + ": Error  pod", ioe);
		}

	}

	private boolean startService(String podUUID) {
		boolean result = false;
		
		HttpPost serviceStart = new HttpPost(
				dockercloudConfiguration.getProtocol() + "://"
						+ dockercloudConfiguration.getHostname()
						+ "/api/app/v1/service/" + podUUID + "/start/");
		serviceStart.addHeader("accept", "application/json");
		serviceStart.addHeader(BasicScheme.authenticate(
				new UsernamePasswordCredentials(dockercloudConfiguration
						.getUsername(), dockercloudConfiguration.getApikey()),
				"UTF-8", false));
	
		try (CloseableHttpResponse response = httpClient.execute(serviceStart)) {
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			InputStream responseBody = entity.getContent();
	
			JsonNode rootNode = jsonObjectMapper.readTree(responseBody);
			
			if (status == HttpStatus.SC_ACCEPTED) {
				LOGGER.info("Pod " + podUUID + ": Started");
				result = true;
	
			}
			else if (status == HttpStatus.SC_BAD_REQUEST){
				LOGGER.info("Pod " + podUUID + ": attempted start, and image not found");
				result = false;
			}
	
		} catch (IOException ioe) {
			LOGGER.error("Pod " + podUUID + ": Error  pod", ioe);
		}
		
		return result;
	
	}

}
