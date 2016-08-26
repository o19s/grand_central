package com.o19s.grandcentral.dockercloud;

import com.o19s.grandcentral.ImageRegistry;

public class DockercloudRegistry implements ImageRegistry {

	public DockercloudRegistry(DockercloudConfiguration dockercloudConfiguration) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean imageExistsInRegistry(String dockerTag) throws Exception {
		
		if (dockerTag.equals("v1")){
			return true;
		}
		else {
			return false;
		}
	}

}
