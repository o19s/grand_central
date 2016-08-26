package com.o19s.grandcentral.kubernetes;

import java.io.IOException;

public interface LinkedContainerManager {

	/**
	 * Get pod information for the given name
	 * @param dockerTag Git hash / name of the pod to return
	 * @return The pod which matches the given key.
	 */
	public abstract Pod get(String dockerTag) throws IOException;

	/**
	 * Does the provided dockerTag currently exist within the cluster
	 * @param dockerTag Git hash / name of the pod to check
	 * @return True if the pod exists
	 */
	public abstract Boolean contains(String dockerTag);

	/**
	 * Adds a pod with the docker tag
	 * @param dockerTag Git hash / name of the pod to deploy
	 */
	public abstract Pod add(String dockerTag) throws Exception;

}