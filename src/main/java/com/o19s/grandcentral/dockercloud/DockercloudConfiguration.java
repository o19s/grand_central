package com.o19s.grandcentral.dockercloud;

import io.dropwizard.jackson.JsonSnakeCase;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;


@JsonSnakeCase
public class DockercloudConfiguration {
  @NotNull
  @NotEmpty
  private String hostname;

  @NotNull
  @NotEmpty
  private String username;

  @NotNull
  @NotEmpty
  private String apikey;

  @NotNull
  @NotEmpty
  private String namespace;

  // this can be null
  private String protocol;
  
  @NotNull
  @NotEmpty
  private String stackJsonPath;

  @JsonProperty
  public String getHostname() {
    return hostname;
  }

  @JsonProperty
  public void setHostname(String masterIp) {
    this.hostname = masterIp;
  }

  @JsonProperty
  public String getUsername() {
    return username;
  }

  @JsonProperty
  public String getProtocol() {
    return (protocol == null) ? "https" : protocol;
  }

  @JsonProperty
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  @JsonProperty
  public void setUsername(String username) {
    this.username = username;
  }

  @JsonProperty
  public String getApikey() {
    return apikey;
  }

  @JsonProperty
  public void setApikey(String apikey) {
    this.apikey = apikey;
  }

  @JsonProperty
  public String getNamespace() {
    return namespace;
  }

  @JsonProperty
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
  
  @JsonProperty
  public String getStackJsonPath() {
    return stackJsonPath;
  }

  @JsonProperty
  public void setStackJsonPath(String stackJsonPath) {
    this.stackJsonPath = stackJsonPath;
  }
}
