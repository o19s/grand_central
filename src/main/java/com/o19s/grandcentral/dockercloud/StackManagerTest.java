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
		dockercloudConfig.setNamespace("datastart");
		dockercloudConfig.setUsername("dep4b");
		dockercloudConfig.setApikey("YOUR_API_KEY");
		dockercloudConfig.setStackJsonPath("./src/test/resources/docker-cloud.json");
				
	}

	@Test
	public void testGet() {
		fail("Not yet implemented");
	}

	@Test
	public void testContains() {
		fail("Not yet implemented");
	}

	@Test
	public void testAdd() throws Exception{
		StackManager stackManager = new StackManager(dockercloudConfig,10000,2);
		Pod pod = stackManager.add("v1");
		assertEquals("v1", pod.getDockerTag());
		assertTrue(pod.isRunning());
	
	}

}
