package blended.sbt.feature

import sbt.Binary
import sbt.librarymanagement.{Constant, ModuleID}

case class FeatureBundle(
  dependency: ModuleID,
  startLevel: Option[Int] = None,
  start: Boolean = false
) {

  def formatConfig: String = {

    val builder: StringBuilder = new StringBuilder("{ ")

    builder.append("url=\"")
    builder.append("mvn:")

    builder.append(dependency.organization)
    builder.append(":")
    builder.append(dependency.name)

    builder.append(FeatureBundle.artifactNameSuffix(dependency))

    builder.append(":")

    // if true, we render the long form with 5 parts (4 colons) instead of 3 parts (2 colons)
    //      val longForm = dependency.classifier.isDefined || !dependency.`type`.equals("jar")

    val longForm = !dependency.explicitArtifacts.isEmpty

    val classifiersAndTypes: Seq[(String, String)] = dependency.explicitArtifacts.collect {
      case a =>
        a.classifier.getOrElse("") -> a.`type`
    }

    if (longForm) {
      builder.append(classifiersAndTypes.head._1)
      builder.append(":")
    }

    builder.append(dependency.revision)

    if (longForm) {
      builder.append(":")
      builder.append(classifiersAndTypes.head._2)
    }

    builder.append("\"")

    startLevel.foreach { sl => builder.append(s", startLevel=${sl}") }
    if (start) builder.append(", start=true")

    builder.append(" }")

    builder.toString()
  }
}

object FeatureBundle {
  /**
   *
   * @param moduleID
   * @param scalaBinVersion We hard-code the default, to avoid to make this def a sbt setting.
   * @return
   */
  def artifactNameSuffix(moduleID: ModuleID, scalaBinVersion: String = "2.12"): String = moduleID.crossVersion match {
    case b: Binary => s"_${b.prefix}${scalaBinVersion}${b.suffix}"
    case c: Constant => s"_${c.value}"
    case _ => ""
  }
}