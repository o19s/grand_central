package com.o19s.grandcentral.kubernetes;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Factory for Pod objects
 */
public class PodFactory {
  /**
   * Parses pod JSON data from Kubernetes converting it into an object
   * @param podJson JSON representing the node
   * @return Pod represented by the JSON or null
   */
  public static Pod podFromJson(JsonNode podJson, int podPort) {
    if (podJson != null) {
      String name = podJson.get("metadata").get("name").asText();
      String status = podJson.get("status").get("phase").asText();
      String podIP = status.equals("Running") ? podJson.get("status").get("podIP").asText() : "";

      return new Pod(name, podIP, status, podPort);
    } else {
      return null;
    }
  }
}
