package com.o19s.grandcentral;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import com.o19s.grandcentral.dockercloud.DockercloudRegistry;
import com.o19s.grandcentral.dockercloud.StackManager;
import com.o19s.grandcentral.kubernetes.LinkedContainerManager;
import com.o19s.grandcentral.servlets.PodProxyServlet;
import com.o19s.grandcentral.servlets.PodServletFilter;
//import com.o19s.grandcentral.healthchecks.ContainerRegistryHealthCheck;
//import com.o19s.grandcentral.healthchecks.KubernetesMasterHealthCheck;

public class GrandCentralApplication2 extends Application<GrandCentralConfiguration2> {
  public static void main(String[] args) throws Exception {
    new GrandCentralApplication2().run(args);
  }

  @Override
  public String getName() {
    return "Grand Central";
  }

  @Override
  public void initialize(Bootstrap<GrandCentralConfiguration2> bootstrap) {}

  @Override
  public void run(GrandCentralConfiguration2 config, Environment environment) throws Exception {
    // Add health checks
	  /*
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
        */

    // Build the StackManager
	LinkedContainerManager stuffManagerInterfaceNeedBetterName = new StackManager(
        config.getDockercloudConfiguration(),
        config.getRefreshIntervalInMs(),
        config.getMaximumStackCount()
        );
   

    ImageRegistry gCloudRegistry = new DockercloudRegistry(config.getDockercloudConfiguration());


    // Define the filter and proxy
    final PodServletFilter psv = new PodServletFilter(config.getGrandcentralDomain(), stuffManagerInterfaceNeedBetterName, gCloudRegistry);
    final PodProxyServlet pps = new PodProxyServlet(config.getPodPort());

    // Disable Jersey in the proxy environment
    environment.jersey().disable();

    // Setup Servlet filters and proxies
    environment.servlets().addFilter("Pod Servlet Filter", psv)
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    environment.servlets().addServlet("Pod Proxy Servlet", pps)
        .addMapping("/*");
  }
}
