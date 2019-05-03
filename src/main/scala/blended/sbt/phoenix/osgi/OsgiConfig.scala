package blended.sbt.phoenix.osgi

import java.nio.file.Files

import sbt._
import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}
import phoenix.ProjectConfig
import sbt.AutoPlugin

trait OsgiConfig extends ProjectConfig {

  /** If `true` (default), this project is packaged as OSGi bundle. */
  def osgi: Boolean = true

  /**
   * The Bundle configuration. The Bundle ID is the [[projectName]]
   */
  def bundle: OsgiBundle = OsgiBundle(
    bundleSymbolicName = projectName,
    exportPackage = Seq(projectName),
    privatePackage = Seq(s"${projectName}.internal.*"),
    defaultImports = true
  )

  override def plugins: Seq[AutoPlugin] = super.plugins ++ {
    if (osgi) Seq(SbtOsgi) else Seq()
  }

  override def settings: Seq[sbt.Setting[_]] = super.settings ++ {
    if (osgi) {
      val b = bundle

      Seq(
        // This setting does not seem to have an effect as it does not fail if a package is missing from the jar
        // OsgiKeys.failOnUndecidedPackage := true,

        OsgiKeys.bundleSymbolicName := Option(b.bundleSymbolicName).getOrElse(Keys.name.value),
        OsgiKeys.bundleVersion := Option(b.bundleVersion).getOrElse(Keys.version.value),
        OsgiKeys.bundleActivator := Option(b.bundleActivator),
        OsgiKeys.importPackage := {
          val effectiveImports = (if (b.defaultImports) {
            Seq(OsgiConfig.scalaRangeImport(Keys.scalaBinaryVersion.value))
          } else {
            Seq.empty
          }) ++
            b.importPackage ++
            (if (b.defaultImports) {
              Seq("*")
            } else {
              Seq.empty
            })

          Keys.sLog.value.debug(s"Effective package imports for [${b.bundleSymbolicName}] : [${effectiveImports}]")

          effectiveImports
        },
        OsgiKeys.exportPackage := b.exportPackage,
        OsgiKeys.privatePackage := b.privatePackage,
        // ensure we build a package with OSGi Manifest
        Compile / Keys.packageBin := {
          OsgiKeys.bundle
        }.dependsOn(Def.task[Unit] {
          // Make sure the classes directory exists before we start bundling
          // to avoid unnecessary bnd errors
          val classDir = (Compile / Keys.classDirectory).value
          if (!classDir.exists()) {
            Files.createDirectories(classDir.toPath())
          }
        }).value
      ) ++
        Option(b.embeddedJars) ++
        Seq(
          OsgiKeys.additionalHeaders ++= Map(
            "Bundle-Name" -> b.bundleSymbolicName,
            "Bundle-Description" -> Keys.description.value
          ) ++
            Option(b.exportContents).map(c => Map("-exportcontents" -> c.mkString(","))).getOrElse(Map()) ++
            b.additionalHeaders
        )

    } else Seq()
  }

}

object OsgiConfig {
  def scalaRangeImport(scalaBinaryVersion: String): String = s"""scala.*;version="[$scalaBinaryVersion,$scalaBinaryVersion.50)""""
}