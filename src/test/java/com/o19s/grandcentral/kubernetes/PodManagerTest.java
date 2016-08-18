package com.o19s.grandcentral.kubernetes;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static junit.framework.TestCase.assertTrue;

;
/**
 * Created by Omnifroodle on 5/17/16.
 */
public class PodManagerTest {
    KubernetesConfiguration kubecfg;
    PodManager manager;

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
        // public ObjectNode buildPodDefinition(String dockerTag) {
        manager = new PodManager(this.kubecfg, "config/grandcentral.jks", 1000000000, 2, "config/pod.yaml.example");
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
    public void buildPodDefinitionTest() throws Exception {
        // TODO add a test for adding the docker tag to the image attribute
        // I haven't done this yet because I like testing our example but don't
        // want to replace echopod for now
        String hash = "abc";
        ObjectNode node = manager.buildPodDefinition(hash);
        String a = "managed";
        String b = node.get("metadata").get("labels").get("GrandCentral").textValue();
        assert(a.equals(b));
        String c = node.get("metadata").get("name").textValue();
        assert(c.equals(hash));
    }

    @Test
    public void testRefreshPods() throws Exception {
    }
}