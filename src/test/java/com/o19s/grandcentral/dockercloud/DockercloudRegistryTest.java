package com.o19s.grandcentral.dockercloud;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DockercloudRegistryTest {

	
	@Test
//	@Ignore
	public void testCheck() throws Exception {
		
		DockercloudConfiguration dockercloudConfig = new DockercloudConfiguration();
		dockercloudConfig.setProtocol("https");
		dockercloudConfig.setHostname("cloud.docker.com");
		dockercloudConfig.setNamespace("datastart");
		dockercloudConfig.setUsername("dep4b");
		dockercloudConfig.setApikey("YOUR_API_KEY");
		
		
		
		DockercloudRegistry dockercloudRegistry = new DockercloudRegistry(dockercloudConfig);
		
		assertTrue(dockercloudRegistry.imageExistsInRegistry("dep4b/datastart:v1"));
		assertFalse(dockercloudRegistry.imageExistsInRegistry("dep4b/datastart:v2"));
		
	}

}
