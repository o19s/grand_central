package com.o19s.grandcentral.kubernetes;

import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;

/**
 * Represents a Kubernetes pod.
 */
public class Pod {
  private String dockerTag;
  private String address;
  private String status;
  private AtomicLong lastRequest;
  private String uuid; // Needed for Dockercloud, we pass UUID around.

  /**
   * Creates a new Pod
   * @param dockerTag
   * @param address
   * @param status
   */
  public Pod(String dockerTag, String address, String status) {
    this(dockerTag, address, status, DateTime.now().getMillis());
  }

  /**
   * Creates a new Pod
   * @param dockerTag Identifier for the Pod
   * @param address IP Address of the Pod
   * @param status Status of the Pod
   * @param lastRequest When this pod last received a request
   */
  public Pod(String dockerTag, String address, String status, long lastRequest) {
    this.dockerTag = dockerTag;
    this.address = address;
    this.status = status;
    this.lastRequest = new AtomicLong(lastRequest);
  }

  public String getDockerTag() {
    return dockerTag;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getStatus() { return status; }

  //FIXME  the Partly running is a Dockercloud thing, we have many bits that may or may not ALL be running.
  //  For example, a init script...
  public boolean isRunning() {
    return status != null && (status.equals("Running") || status.equals("Partly running"));
  }

  public long getLastRequest() {
    return lastRequest.get();
  }

  public void setLastRequest(long requestedAt) {
    this.lastRequest.set(requestedAt);
  }

public String getUuid() {
	return uuid;
}

public void setUuid(String uuid) {
	this.uuid = uuid;
}
}
