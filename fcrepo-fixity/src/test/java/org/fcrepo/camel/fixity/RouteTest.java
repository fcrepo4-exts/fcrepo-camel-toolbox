/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.fixity;

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-06-18
 */
public class RouteTest extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String identifier = "/file1";

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
         final Properties props = new Properties();
         props.put("fixity.failure", "mock:failure");
         props.put("fixity.success", "mock:success");
         props.put("fixity.stream", "seda:foo");
         return props;
    }

    @Test
    public void testBinaryFixitySuccess() throws Exception {

        context.getRouteDefinition("FcrepoFixity").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo:*");
            }
        });
        context.start();

        getMockEndpoint("mock:failure").expectedMessageCount(0);
        getMockEndpoint("mock:success").expectedMessageCount(1);

        final String body = IOUtils.toString(loadResourceAsStream("fixity.rdf"), "UTF-8");
        template.sendBodyAndHeader(body, FCREPO_URI, baseURL + identifier);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testBinaryFixityFailure() throws Exception {

        context.getRouteDefinition("FcrepoFixity").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo:*");
            }
        });
        context.start();

        getMockEndpoint("mock:failure").expectedMessageCount(1);
        getMockEndpoint("mock:success").expectedMessageCount(0);

        final String body = IOUtils.toString(loadResourceAsStream("fixityFailure.rdf"), "UTF-8");
        template.sendBodyAndHeader(body, FCREPO_URI, baseURL + identifier);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testNonBinary() throws Exception {

        context.getRouteDefinition("FcrepoFixity").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo:*");
            }
        });
        context.start();

        getMockEndpoint("mock:failure").expectedMessageCount(0);
        getMockEndpoint("mock:success").expectedMessageCount(0);

        final String body = IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8");
        template.sendBodyAndHeader(body, FCREPO_URI, baseURL + identifier);

        assertMockEndpointsSatisfied();
    }
}
