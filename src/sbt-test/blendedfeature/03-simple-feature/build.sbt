import blended.sbt.feature._

lazy val root = (project in file("."))
  .enablePlugins(BlendedFeaturePlugin)
  .settings(
    version := "0.1",
    scalaVersion := "2.12.8",
    featureConfig := Feature(
      name = "simple",
      bundles = Seq(
        FeatureBundle("org.slf4j" % "slf4j-api" % "1.7.14")
      )
    )
  )

