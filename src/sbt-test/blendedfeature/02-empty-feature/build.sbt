import blended.sbt.feature._

lazy val root = (project in file("."))
  .enablePlugins(BlendedFeaturePlugin)
  .settings(
    version := "0.1",
    scalaVersion := "2.12.8",
    featureConfig := Feature(
      name = "empty",
      bundles = Seq()
    )
  )
