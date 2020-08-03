[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-org-apache-sling-graphql-core/master)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-graphql-core/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-org-apache-sling-graphql-core/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-graphql-core/job/master/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.graphql.core/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.graphql.core%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.graphql.core.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.graphql.core) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![graphql](https://sling.apache.org/badges/group-graphql.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/graphql.md)
 
 [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-graphql-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-graphql-core)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-graphql-core&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-graphql-core)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-graphql-core&metric=bugs)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-graphql-core)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-graphql-core&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-graphql-core)

Apache Sling GraphQL Core
----

This module allows for running GraphQL queries in Sling, using dynamically built GraphQL schemas and
OSGi services for data fetchers (aka "resolvers") which provide the data.

To take advantage of Sling's flexibility, it allows for running GraphQL queries in three different modes,
using client or server-side queries and optionally being bound to the current Sling Resource.

Server-side queries are implemented as a Sling Script Engine.

The current version uses the [graphql-java](https://github.com/graphql-java/graphql-java) library but that's 
only used internally. The corresponding OSGi bundles must be active in your Sling instance but there's no
need to use their APIs directly.

The [GraphQL sample website](https://github.com/apache/sling-samples/tree/master/org.apache.sling.graphql.samples.website)
provides usage examples and demonstrates using GraphQL queries (and Handlebars templates) on both the server and
client sides.

> As I write this, work is ongoing at [SLING-9550](https://issues.apache.org/jira/browse/SLING-9550) to implement custom 
> GraphQL Scalars
 
## Supported GraphQL endpoint styles

This module enables the following GraphQL "styles"

  * The **traditional GraphQL endpoint** style, where the clients supply requests to a single URL. It is easy to define
    multiple such endpoints with different settings, which can be useful to provide different "views" of your content.
  * A **Resource-based GraphQL endpoints** style where every Sling Resource can be a GraphQL endpoint (using specific 
    request selectors and extensions) where queries are executed in the context of that Resource. This is an experimental
    idea at this point but it's built into the design so doesn't require more efforts to support. That style supports both
    server-side "**prepared GraphQL queries**" and the more traditional client-supplied queries.
    
The GraphQL requests hit a Sling resource in all cases, there's no need for path-mounted servlets which are [not desirable](https://sling.apache.org/documentation/the-sling-engine/servlets.html#caveats-when-binding-servlets-by-path-1).

## Resource-specific GraphQL schemas

Schemas are provided by `SchemaProvider` services:

    public interface SchemaProvider {
  
      /** Get a GraphQL Schema definition for the given resource and optional selectors
       *
       *  @param r The Resource to which the schema applies
       *  @param selectors Optional set of Request Selectors that can influence the schema selection
       *  @return a GraphQL schema that can be annotated to define the data fetchers to use, see
       *      this module's documentation. Can return null if a schema cannot be provided, in which
       *      case a different provider should be used.
       */
      @Nullable
      String getSchema(@NotNull Resource r, @Nullable String [] selectors) throws IOException;
    }

The default provider makes an internal Sling request with for the current Resource with a `.GQLschema` extension.

This allows the Sling script/servlet resolution mechanism and its script engines to be used to generate 
schemas dynamically, taking request selectors into account.

Unless you have specific needs not covered by this mechanism, there's no need to implement your
own `SchemaProvider` services.

## SlingDataFetcher selection with Schema Directives

The GraphQL schemas used by this module can be enhanced using
[schema directives](http://spec.graphql.org/June2018/#sec-Language.Directives)
(see also the [Apollo docs](https://www.apollographql.com/docs/graphql-tools/schema-directives/) for how those work)
that select specific `SlingDataFetcher` services to return the appropriate data.

A default data fetcher is used for types and fields which have no such directive.

Here's a simple example, the test code has more:

    # This directive maps fields to our Sling data fetchers
    directive @fetcher(
        name : String,
        options : String = "",
        source : String = ""
    ) on FIELD_DEFINITION

    type Query {
      withTestingSelector : TestData @fetcher(name:"test/pipe")
    }

    type TestData {
      farenheit: Int @fetcher(name:"test/pipe" options:"farenheit")
    }

The names of those `SlingDataFetcher` services are in the form

    <namespace>/<name>

The `sling/` namespace is reserved for `SlingDataFetcher` services
which hava Java package names that start with `org.apache.sling`.

The `<options>` and `<source>` arguments of the directive can be used by the
`SlingDataFetcher` services to influence their behavior.

### Scripted SlingDataFetchers

Besides Java, `SlingDataFetcher` scripts can be written in any scripting language that supported by the Sling instance's configuration.

Here's an example from the test code.

The schema contains the following statement:

    scriptedFetcher (testing : String) : Test @fetcher(name:"scripted/example")

And here's the data fetcher code:

    var result = { 
        boolValue: true,
        resourcePath: "From the test script: " + resource.path,
        testingArgument: environment.getArgument("testing"),
        anotherValue: 450 + 1
    };

    result;
    
The data fetcher provider then looks for a script that handles the `graphql/fetchers/scripted/example` resource type with a `fetcher`script name. `graphql/fetchers`is a prefix (hardcoded for now) and `scripted/example` comes from the above schema's `@fetcher` directive.

In that test, the `/graphql/fetchers/scripted/example/fetcher.js` shown above resolves with those requirements, it is executed to retrieve the requested data. That execution happens with a context consisting of the current `SlingDataFetcherEnvironment` under the `environment` key, and the current Sling Resource under the `resource` key, both used in this test script.
    




