Swagger Doc
========

## WORK IN PROGRESS

It's all very hard coded and not well named as this is draft 1
and I'm learning SBT/Scala as I go.

## Intro

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

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}
import com.ksquared.swaggerdoc.akka.AkkaHttpDoc

class RoutesSpec extends WordSpec with Matchers
    with ScalatestRouteTest with AkkaHttpDoc {

    "the route" should {
        "post some things" in {
            val body = SomeBody("123")
            setup(Post("/some/url", body)
                .perform("some route") {
                  status shouldEqual OK
                  // rest of assertions
                }
        }
        
        "get some things" in {
            setup(Get("/some/url/3456", ".*([0-9]*)".r("anId")
                .perform("Get some item") {
                    status shouldEqual OK
                    // rest of assertions
                }
        }
    }
}
```

Mixin the AkkaHttpDoc and use the setup function for requests.

Urls with parameters in them:

`setup(req: HttpRequest, pathRegex: Regex)`

The regular expression should contain named groups to allow for
clearer documentation. 

`setup(Get("/some/url/123456/a/12345"), ".*([0-9]{6})\\/nickname\\/([0-9]{5})".r("userId", "nickname"))`


## Plugin Example

In `plugins.sbt`

`addSbtPlugin("com.ksquared" % "swagger-doc-generator" % "0.1")`

The `swagger-doc` command will automatically become available.

## TODO

* Actually test it against the Swagger UI...
* Write instructions on how to integrate with the Swagger UI
* Write more tests
* Make more configurable (where to write/read files etc)
* Work out how to group routes
* Figure out a friendlier way to do GET/DELETE requests

### Building the project

`sbt compile`

Or a specific project:

`sbt plugin/compile`

To set a specific scala version:

`sbt '++ 2.11.8 models/compile models/publishLocal`

