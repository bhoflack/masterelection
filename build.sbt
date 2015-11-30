lazy val root = (project in file(".")).
    settings(
        name := "masterelection",
        version := "1.0.0-SNAPSHOT",
        scalaVersion := "2.11.6",
        libraryDependencies ++= Seq(
            "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"
          )
        )
