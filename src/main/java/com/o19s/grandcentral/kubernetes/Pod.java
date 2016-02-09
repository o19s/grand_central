package com.o19s.grandcentral.kubernetes;

import org.joda.time.DateTime;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a Kubernetes pod.
 */
public class Pod {
  private String gitHash;
  private String address;
  private String status;
  private AtomicLong lastRequest;

  /**
   * Creates a new Pod
   * @param gitHash
   * @param address
   * @param status
   */
  public Pod(String gitHash, String address, String status) {
    this(gitHash, address, status, DateTime.now().getMillis());
  }

  /**
   * Creates a new Pod
   * @param gitHash Hash identifier for the Pod (7 character git hash)
   * @param address IP Address of the Pod
   * @param status Status of the Pod
   * @param lastRequest When this pod last received a request
   */
  public Pod(String gitHash, String address, String status, long lastRequest) {
    this.gitHash = gitHash;
    this.address = address;
    this.status = status;
    this.lastRequest = new AtomicLong(lastRequest);
  }

  public String getGitHash() {
    return gitHash;
  }

  public String getAddress() {
    return address;
  }

  public String getStatus() { return status; }

  public boolean isRunning() {
    return status != null && status.equals("Running");
  }

  public long getLastRequest() {
    return lastRequest.get();
  }

  public void setLastRequest(long requestedAt) {
    this.lastRequest.set(requestedAt);
  }
}
