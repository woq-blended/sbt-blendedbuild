package blended.sbt.feature

import sbt._
import sbt.librarymanagement.{Constant, ModuleID}

case class Feature(name: String, features: Seq[Feature] = Seq(), bundles: Seq[FeatureBundle], featureRefs: Seq[FeatureRef] = Seq()) {

  def libDeps: Seq[ModuleID] = (features.flatMap(_.libDeps) ++ bundles.map(_.dependency)).distinct

  // This is the content of the feature file
  def formatConfig(version: String): String = {
    val prefix =
      s"""name="${name}"
         |version="${version}"
         |""".stripMargin

    val bundlesList = bundles.map(_.formatConfig).mkString(
      "bundles = [\n", ",\n", "\n]\n"
    )

    val fRefs = featureRefs ++ features.map(f => FeatureRef(f.name))

    val fRefString =
      if (fRefs.isEmpty) ""
      else fRefs.map(f => s"""{ name="${f.name}", version="${version}" }""").mkString(
        "features = [\n", ",\n", "\n]\n"
      )

    prefix + fRefString + bundlesList
  }
}

case class FeatureRef(name: String)