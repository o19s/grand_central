package com.o19s.grandcentral;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.o19s.grandcentral.gcloud.GCloudConfiguration;
import com.o19s.grandcentral.kubernetes.KubernetesConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.jackson.JsonSnakeCase;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Configuration values for the {@link GrandCentralApplication}. Data is loaded from a provided YAML file on start or
 * passed in via Environment variables.
 */
@JsonSnakeCase
public class GrandCentralConfiguration extends Configuration {
  @Valid
  @NotNull
  private long janitorCleanupThreshold;

  @Valid
  @NotNull
  private int maximumPodCount;

  @Valid
  @NotEmpty
  @NotNull
  private String grandcentralDomain;

  @Valid
  @NotNull
  private long refreshIntervalInMs;

  @Valid
  @NotNull
  @NotEmpty
  private String keystorePath;

  @Valid
  @NotNull
  @NotEmpty
  private String podYamlPath;

  @Valid
  @NotNull
  private int podPort;

  @Valid
  @NotNull
  private KubernetesConfiguration kubernetes = new KubernetesConfiguration();

  @Valid
  @NotNull
  private GCloudConfiguration gcloud = new GCloudConfiguration();

  @JsonProperty
  public long getJanitorCleanupThreshold() {
    return janitorCleanupThreshold;
  }

  @JsonProperty
  public void setJanitorCleanupThreshold(long janitorCleanupThreshold) {
    this.janitorCleanupThreshold = janitorCleanupThreshold;
  }

  @JsonProperty
  public int getMaximumPodCount() {
    return maximumPodCount;
  }

  @JsonProperty
  public void setMaximumPodCount(int maximumPodCount) {
    this.maximumPodCount = maximumPodCount;
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
  public String getKeystorePath() {
    return keystorePath;
  }

  @JsonProperty
  public void setKeystorePath(String keystorePath) {
    this.keystorePath = keystorePath;
  }

  @JsonProperty
  public void setRefreshIntervalInMs(long refreshIntervalInMs) {
    this.refreshIntervalInMs = refreshIntervalInMs;
  }

  @JsonProperty
  public String getPodYamlPath() {
    return podYamlPath;
  }

  @JsonProperty
  public void setPodYamlPath(String podYamlPath) {
    this.podYamlPath = podYamlPath;
  }

  @JsonProperty
  public int getPodPort() {
    return podPort;
  }

  @JsonProperty
  public void setPodPort(int podPort) {
    this.podPort = podPort;
  }

  @JsonProperty("kubernetes")
  public KubernetesConfiguration getKubernetesConfiguration() {
    return kubernetes;
  }

  @JsonProperty("kubernetes")
  public void setKubernetesConfiguration(KubernetesConfiguration factory) {
    this.kubernetes = factory;
  }

  @JsonProperty("gcloud")
  public GCloudConfiguration getGCloudConfiguration() {
    return gcloud;
  }

  @JsonProperty("gcloud")
  public void setGCloudConfiguration(GCloudConfiguration gcloud) {
    this.gcloud = gcloud;
  }
}
