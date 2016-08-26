package com.o19s.grandcentral.gcloud;

import io.dropwizard.jackson.JsonSnakeCase;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonSnakeCase
public class GCloudConfiguration {
  private String registryDomain;
  private String registryUsername;
  private String registryPassword;

  private String project;
  private String containerName;

  @JsonProperty
  public String getRegistryDomain() {
    return registryDomain;
  }

  @JsonProperty
  public void setRegistryDomain(String registryDomain) {
    this.registryDomain = registryDomain;
  }

  @JsonProperty
  public String getRegistryUsername() {
    return registryUsername;
  }

  @JsonProperty
  public void setRegistryUsername(String registryUsername) {
    this.registryUsername = registryUsername;
  }

  @JsonProperty
  public String getRegistryPassword() {
    return registryPassword;
  }

  @JsonProperty
  public void setRegistryPassword(String registryPassword) {
    this.registryPassword = registryPassword;
  }

  @JsonProperty
  public String getProject() {
    return project;
  }

  @JsonProperty
  public void setProject(String project) {
    this.project = project;
  }

  @JsonProperty
  public String getContainerName() {
    return containerName;
  }

  @JsonProperty
  public void setContainerName(String containerName) {
    this.containerName = containerName;
  }
}
