package com.o19s.grandcentral;

public interface ImageRegistry {

	public boolean imageExistsInRegistry(String dockerTag)
			throws Exception;

}