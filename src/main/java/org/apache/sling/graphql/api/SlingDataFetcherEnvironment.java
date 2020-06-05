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

package org.apache.sling.graphql.api;

import java.util.Map;

import org.apache.sling.api.resource.Resource;

/** Provides contextual information to the {#link SlingDataFetcher} */
@SuppressWarnings("TypeParameterUnusedInFormals")
public interface SlingDataFetcherEnvironment {
    /** The parent object of the field that's being retrieved */
    Object getParentObject();

    /** @return the arguments passed to the GraphQL query */
    Map<String, Object> getArguments();

    /** @return a single argument, passed to the GraphQL query */
    <T> T getArgument(String name);

    /** @return a single argument, passed to the GraphQL query */
    <T> T getArgument(String name, T defaultValue);

    /** Get the current Sling resource */
    Resource getCurrentResource();

    /** Options, if set by the schema directive */
    String getFetcherOptions();

    /** Source, if set by the schema directive */
    String getFetcherSource();
}