package com.o19s.grandcentral.kubernetes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import io.dropwizard.setup.Environment;
import org.apache.http.client.HttpClient;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;


@JsonSnakeCase
public class KubernetesConfiguration {
  @NotNull
  @NotEmpty
  private String masterIp;

  @NotNull
  @NotEmpty
  private String username;

  @NotNull
  @NotEmpty
  private String password;

  @NotNull
  @NotEmpty
  private String namespace;

  @JsonProperty
  public String getMasterIp() {
    return masterIp;
  }

  @JsonProperty
  public void setMasterIp(String masterIp) {
    this.masterIp = masterIp;
  }

  @JsonProperty
  public String getUsername() {
    return username;
  }

  @JsonProperty
  public void setUsername(String username) {
    this.username = username;
  }

  @JsonProperty
  public String getPassword() {
    return password;
  }

  @JsonProperty
  public void setPassword(String password) {
    this.password = password;
  }

  @JsonProperty
  public String getNamespace() {
    return namespace;
  }

  @JsonProperty
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
}
