name := "swagger-doc"

lazy val models = project
    .settings(Common.settings: _*)

lazy val plugin = project
    .dependsOn(models)
    .settings(Common.settings: _*)
    .settings(libraryDependencies ++= Dependencies.common ++ Dependencies.testing  )

lazy val akka = project
    .dependsOn(models)
    .settings(Common.settings: _*)
    .settings(libraryDependencies ++= Dependencies.akka)