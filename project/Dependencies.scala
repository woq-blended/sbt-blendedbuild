
import sbt._
import sbt.Keys._

object Dependencies {

  /** Attach these attributes to dependencies to sbt plugins */
  val sbtAttributes = Def.setting(Map(
    "e:scalaVersion" -> scalaBinaryVersion.value,
    "e:sbtVersion" -> "1.0"
  ))

  val sbtFilterResources = Def.setting("de.wayofquality" % "sbt-filterresources" % "0.1.1" withExtraAttributes sbtAttributes.value)

  val sbtNativePackager = Def.setting("com.typesafe.sbt" % "sbt-native-packager" % "1.3.9" withExtraAttributes sbtAttributes.value)

  val sbtPhoenix = Def.setting("de.wayofquality.sbt" % "sbt-phoenix" % "0.1.0" withExtraAttributes sbtAttributes.value)

  val sbtOsgi = Def.setting("com.typesafe.sbt" % "sbt-osgi" % "0.9.5" withExtraAttributes sbtAttributes.value)
}
