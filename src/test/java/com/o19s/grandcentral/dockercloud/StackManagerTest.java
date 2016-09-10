package com.o19s.grandcentral.dockercloud;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.o19s.grandcentral.kubernetes.Pod;

public class StackManagerTest {

	private DockercloudConfiguration dockercloudConfig;
	@Before
	public void setUp() throws Exception {
		dockercloudConfig = new DockercloudConfiguration();
		dockercloudConfig.setProtocol("https");
		dockercloudConfig.setHostname("cloud.docker.com");
		dockercloudConfig.setNamespace("gctest");
		dockercloudConfig.setUsername("dep4b");
		dockercloudConfig.setApikey("YOUR_API_KEY");
		dockercloudConfig.setStackJsonPath("./src/test/resources/docker-cloud.json");
		dockercloudConfig.setStackExistsTestImage("mysql");
				
	}


	@Test
	public void testAddRemoveLifycycle() throws Exception{
		StackManager stackManager = new StackManager(dockercloudConfig,10000,2);
		
		Pod pod = stackManager.add("bogus_tag");
		assertNull(pod);

		pod = stackManager.add("latest");
		assertEquals("latest", pod.getDockerTag());
		assertTrue(pod.isRunning());
		
		stackManager.remove(pod.getDockerTag(), pod.getUuid());
		
		pod = stackManager.add("5.5");
		assertEquals("5.5", pod.getDockerTag());
		assertTrue(pod.isRunning());
		stackManager.remove(pod.getDockerTag(), pod.getUuid());
	}

}
