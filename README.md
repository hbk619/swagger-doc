Swagger Doc
========

This project allows you to generate swagger json from Akka Http tests.
It's inspired by https://github.com/kodemaniak/akka-http-restdoc which creates
some generic documentation from your tests.

It has 2 components you need:

* An sbt plugin `swagger-doc` to run after tests have completed to generate the single swagger json file
* A trait for running Akka Http tests

## Trait Example

Add the dependency:

`"com.ksquared" %% "swagger-doc-akka" % "0.1"`

Then write a test!

```
class UserRoutesSpec extends WordSpec with ScalatestRouteTest with AkkaHttpRestDoc {

    "the route" should {
        "do some things" in {
            val body = SomeBody("123")
            setupRequestWithBody(Post("/some/url", body)
                .perform("some route") {
                  status shouldEqual OK
                  // rest of assertions
                }
        }
    }
}
```

Mixin the AkkaHttpRestDoc and use the performWithBody function for requests that have a body.

## Plugin Example

In `plugins.sbt`

`addSbtPlugin("com.ksquared" % "swagger-doc-generator" % "0.1")`

The `swagger-doc` command will automatically become available.

### Building the project

`sbt compile`

Or a specific project:

`sbt plugin/compile`

To set a specific scala version:

`sbt '++ 2.11.8 models/compile models/publishLocal`

