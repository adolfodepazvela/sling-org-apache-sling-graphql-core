/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.graphql.core.it;

import javax.inject.Inject;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;

import org.apache.sling.graphql.api.SchemaProvider;
import org.apache.sling.graphql.core.mocks.ReplacingSchemaProvider;
import org.apache.sling.resource.presence.ResourcePresence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GraphQLServletIT extends GraphQLScriptingTestSupport {

    @Inject
    @Filter(value = "(path=/content/graphql/two)")
    private ResourcePresence resourcePresence;

    @Inject
    private BundleContext bundleContext;

    @Inject
    private SchemaProvider defaultSchemaProvider;

    private static final String GRAPHQL_SERVLET_CONFIG_PID = "org.apache.sling.graphql.core.GraphQLServlet";

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            pipeDataFetcher(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/content/graphql/two")
                .asOption(),

            // The GraphQL servlet is disabled by default, try setting up two of them
            factoryConfiguration(GRAPHQL_SERVLET_CONFIG_PID)
                .put("sling.servlet.resourceTypes", "sling/servlet/default")
                .put("sling.servlet.extensions", "gql")
                .put("sling.servlet.methods", new String[] { "GET", "POST" })
                .asOption(),
            factoryConfiguration(GRAPHQL_SERVLET_CONFIG_PID)
                .put("sling.servlet.resourceTypes", "graphql/test/two")
                .put("sling.servlet.selectors", new String[] { "testing", "another" })
                .put("sling.servlet.extensions", "otherExt")
                .asOption(),
        };
    }

    @Test
    public void testGqlExt() throws Exception {
        final String json = getContent("/graphql/two.gql", "query", "{ currentResource { resourceType name } }");
        assertThat(json, hasJsonPath("$.data.currentResource.resourceType", equalTo("graphql/test/two")));
        assertThat(json, hasJsonPath("$.data.currentResource.name", equalTo("two")));
        assertThat(json, hasNoJsonPath("$.data.currentResource.path"));
    }

    @Test
    public void testGqlExtWithPost() throws Exception {
        final String json = getContentWithPost("/graphql/two.gql", "{ currentResource { resourceType name } }", null);
        assertThat(json, hasJsonPath("$.data.currentResource.resourceType", equalTo("graphql/test/two")));
        assertThat(json, hasJsonPath("$.data.currentResource.name", equalTo("two")));
        assertThat(json, hasNoJsonPath("$.data.currentResource.path"));
    }

    @Test
    public void testOtherExtAndTestingSelector() throws Exception {
        executeRequest("GET", "/graphql/two.otherExt", null, 404);
        final String json = getContent("/graphql/two.testing.otherExt", "query", "{ withTestingSelector { farenheit } }");
        assertThat(json, hasJsonPath("$.data.withTestingSelector.farenheit", equalTo(451)));
    }

    @Test
    public void testOtherExtAndOtherSelector() throws Exception {
        final String json = getContent("/graphql/two.another.otherExt", "query", "{ currentResource { resourceType name } }");
        assertThat(json, hasJsonPath("$.data.currentResource.resourceType", equalTo("graphql/test/two")));
        assertThat(json, hasJsonPath("$.data.currentResource.name", equalTo("two")));
        assertThat(json, hasNoJsonPath("$.data.currentResource.path"));
    }

    @Test
    public void testMissingQuery() throws Exception {
        executeRequest("GET", "/graphql/two.gql", null, 400);
    }

    @Test
    public void testDefaultJson() throws Exception {
        final String json = getContent("/graphql/two.json");
        assertThat(json, hasJsonPath("$.title", equalTo("GraphQL two")));
        assertThat(json, hasJsonPath("$.jcr:primaryType", equalTo("nt:unstructured")));
    }

    @Test
    public void testMultipleSchemaProviders() throws Exception {
        new ReplacingSchemaProvider("currentResource", "REPLACED").register(bundleContext, defaultSchemaProvider, 1);
        new ReplacingSchemaProvider("currentResource", "NOT_THIS_ONE").register(bundleContext, defaultSchemaProvider, Integer.MAX_VALUE);
        final String json = getContent("/graphql/two.gql", "query", "{ REPLACED { resourceType name } }");
        assertThat(json, hasJsonPath("$.data.REPLACED.resourceType", equalTo("graphql/test/two")));
        assertThat(json, hasJsonPath("$.data.REPLACED.name", equalTo("two")));
        assertThat(json, hasNoJsonPath("$.data.REPLACED.path"));
    }
}
