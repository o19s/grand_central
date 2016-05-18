package com.o19s.grandcentral.kubernetes;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

;
/**
 * Created by Omnifroodle on 5/17/16.
 */
public class PodManagerTest {
    private KubernetesConfiguration kubecfg;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8888);

    @Before
    public void setUp() throws Exception {
        this.kubecfg = new KubernetesConfiguration();
        kubecfg.setMasterIp("127.0.0.1:8888");
        kubecfg.setProtocol("http");
        kubecfg.setNamespace("test");
        kubecfg.setUsername("fred");
        kubecfg.setPassword("flintstone");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGet() throws Exception {

    }

    @Test
    public void testContains() throws Exception {

    }

    @Test
    public void testAdd() throws Exception {

    }

    @Test
    public void testRefreshPods() throws Exception {
        stubFor(get(urlEqualTo("/api/v1/namespaces/test/pods"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"items\":[" +
                                "   {" +
                                "       \"metadata\":{" +
                                "           \"name\": \"abc\"" +
                                "       }," +
                                "       \"status\": {" +
                                "           \"phase\": \"Running\"," +
                                "           \"podIP\": \"1.1.1.1\"" +
                                "       }" +
                                "   }," +
                                "   {" +
                                "       \"metadata\":{" +
                                "           \"name\": \"abcd\"" +
                                "       }," +
                                "       \"status\": {" +
                                "           \"phase\": \"Running\"," +
                                "           \"podIP\": \"1.1.1.2\"" +
                                "       }" +
                                "   }" +
                                "   ]}")));

        PodManager manager = new PodManager(this.kubecfg, "config/grandcentral.jks", 100, 1, "./config/configuration.yml");
    }
}