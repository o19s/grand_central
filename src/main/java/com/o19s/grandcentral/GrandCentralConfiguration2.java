package com.o19s.grandcentral;

import io.dropwizard.Configuration;
import io.dropwizard.jackson.JsonSnakeCase;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.o19s.grandcentral.dockercloud.DockercloudConfiguration;

/**
 * Configuration values for the {@link GrandCentralApplication}. Data is loaded from a provided YAML file on start or
 * passed in via Environment variables.
 */
@JsonSnakeCase
public class GrandCentralConfiguration2 extends Configuration {
  @Valid
  @NotNull
  private long janitorCleanupThreshold;

  @Valid
  @NotNull
  private int maximumStackCount;

  @Valid
  @NotEmpty
  @NotNull
  private String grandcentralDomain;

  @Valid
  @NotNull
  private long refreshIntervalInMs;
  
  @Valid
  @NotNull
  private int podPort;



  
  @Valid
  @NotNull
  private DockercloudConfiguration dockercloud = new DockercloudConfiguration();
  

  @JsonProperty
  public long getJanitorCleanupThreshold() {
    return janitorCleanupThreshold;
  }

  @JsonProperty
  public void setJanitorCleanupThreshold(long janitorCleanupThreshold) {
    this.janitorCleanupThreshold = janitorCleanupThreshold;
  }

  
  @JsonProperty
  public int getMaximumStackCount() {
    return maximumStackCount;
  }

  @JsonProperty
  public void setMaximumStackCount(int maximumStackCount) {
    this.maximumStackCount = maximumStackCount;
  }

  @JsonProperty
  public String getGrandcentralDomain() {
    return grandcentralDomain;
  }

  @JsonProperty
  public void setGrandcentralDomain(String grandcentralDomain) {
    this.grandcentralDomain = grandcentralDomain;
  }

  @JsonProperty
  public long getRefreshIntervalInMs() {
    return refreshIntervalInMs;
  }


  @JsonProperty
  public void setRefreshIntervalInMs(long refreshIntervalInMs) {
    this.refreshIntervalInMs = refreshIntervalInMs;
  }


  @JsonProperty("dockercloud")
  public DockercloudConfiguration getDockercloudConfiguration() {
    return dockercloud;
  }

  @JsonProperty("dockercloud")
  public void setDockercloudConfiguration(DockercloudConfiguration factory) {
    this.dockercloud = factory;
  }


  @JsonProperty
  public int getPodPort() {
    return podPort;
  }

  @JsonProperty
  public void setPodPort(int podPort) {
    this.podPort = podPort;
  }
}
