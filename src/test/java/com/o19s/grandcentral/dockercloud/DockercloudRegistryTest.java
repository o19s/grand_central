package com.o19s.grandcentral.dockercloud;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.jaunt.UserAgent;
import com.jaunt.component.Label;

public class DockercloudRegistryTest {

	
	@Test
	@Ignore
	public void testCheck() throws Exception {
		DockercloudRegistry dockercloudRegistry = new DockercloudRegistry(null);
		
		assertTrue(dockercloudRegistry.imageExistsInRegistry("dep4b/datastart:v1"));
		assertFalse(dockercloudRegistry.imageExistsInRegistry("dep4b/datastart:v2"));
		
	}
	@Test
	@Ignore
	public void testScrapeDockerHub() throws Exception {
		
		

		 UserAgent userAgent = new UserAgent(); 
		  userAgent.visit("https://hub.docker.com/login/");
		 
		  userAgent.doc.fillout("FancyInput__default___1Iybp", "dep4b");       //fill out the component labelled 'Username:' with "tom"
		  userAgent.doc.fillout("FancyInput__error___TIz2p", "your password");    //fill out the component labelled 'Password:' with "secret"
//		  userAgent.doc.choose(Label.RIGHT, "Remember me");//choose the component right-labelled 'Remember me'.
		  userAgent.doc.submit();                          //submit the form
		  System.out.println(userAgent.getLocation());     //print the current location (url)
		
	}

}
