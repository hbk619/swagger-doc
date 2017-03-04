name := "swagger-doc"

lazy val models = project
    .settings(Common.settings: _*)
    .settings(libraryDependencies ++= Dependencies.testing)

lazy val plugin = project
    .dependsOn(models)
    .settings(Common.settings: _*)
    .settings(libraryDependencies ++= Dependencies.common ++ Dependencies.testing  )

lazy val akka = project
    .dependsOn(models)
    .settings(Common.settings: _*)
    .settings(libraryDependencies ++= Dependencies.akka)