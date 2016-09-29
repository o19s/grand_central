package com.o19s.grandcentral;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import com.o19s.grandcentral.gcloud.GCloudRegistry;
import com.o19s.grandcentral.healthchecks.ContainerRegistryHealthCheck;
import com.o19s.grandcentral.healthchecks.KubernetesMasterHealthCheck;
import com.o19s.grandcentral.kubernetes.PodManager;
import com.o19s.grandcentral.servlets.PodProxyServlet;
import com.o19s.grandcentral.servlets.PodServletFilter;

public class GrandCentralApplication extends Application<GrandCentralConfiguration> {
  public static void main(String[] args) throws Exception {
    new GrandCentralApplication().run(args);
  }

  @Override
  public String getName() {
    return "Grand Central";
  }

  @Override
  public void initialize(Bootstrap<GrandCentralConfiguration> bootstrap) {}

  @Override
  public void run(GrandCentralConfiguration config, Environment environment) throws Exception {
    // Add health checks
    environment.healthChecks().register("container_registry", new ContainerRegistryHealthCheck(
        config.getKeystorePath(),
        config.getGCloudConfiguration().getRegistryDomain(),
        config.getGCloudConfiguration().getProject(),
        config.getGCloudConfiguration().getContainerName(),
        config.getGCloudConfiguration().getRegistryUsername(),
        config.getGCloudConfiguration().getRegistryPassword()));
    environment.healthChecks().register("kubernetes_master", new KubernetesMasterHealthCheck(
        config.getKubernetesConfiguration().getMasterIp(),
        config.getKeystorePath(),
        config.getKubernetesConfiguration().getUsername(),
        config.getKubernetesConfiguration().getPassword(),
        config.getKubernetesConfiguration().getNamespace()));

    // Build the PodManager
    LinkedContainerManager podManager = new PodManager(
        config.getKubernetesConfiguration(),
        config.getKeystorePath(),
        config.getRefreshIntervalInMs(),
        config.getMaximumPodCount(),
        config.getPodYamlPath()
    );

    ImageRegistry gCloudRegistry = new GCloudRegistry(config.getGCloudConfiguration(), config.getKeystorePath());

    // Define the filter and proxy
    final PodServletFilter psv = new PodServletFilter(config.getGrandcentralDomain(), podManager, gCloudRegistry);
    final PodProxyServlet pps = new PodProxyServlet();

    // Disable Jersey in the proxy environment
    environment.jersey().disable();

    // Setup Servlet filters and proxies
    environment.servlets().addFilter("Pod Servlet Filter", psv)
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    environment.servlets().addServlet("Pod Proxy Servlet", pps)
        .addMapping("/*");
  }
}
