package blended.sbt.phoenix.docker

import blended.sbt.dockercontainer.BlendedDockerContainerPlugin
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin.{autoImport => DC}
import blended.sbt.container.BlendedContainerPlugin.{autoImport => BC}
import phoenix.ProjectConfig
import sbt.AutoPlugin
import sbt.Keys.target
import sbt._

/**
 * You need at least add an sbt-setting for `containerImage`
 */
trait DockerContainer extends ProjectConfig {

  def blendedVersion: String

  def imageTag: String

  def folder: String

  def ports: List[Int] = List()

  /**
   * Filenames to overlays, relative to the project directory.
   */
  def overlays: Seq[String] = Seq()

  def env: Map[String, String] = Map()

  def profileName: Option[String] = None

  def profileVersion: Option[String] = None

  def maintainer: String

  override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(BlendedDockerContainerPlugin)

  override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
    BC.blendedVersion := blendedVersion,
    DC.appFolder := folder,
    DC.imageTag := imageTag,
    DC.ports := ports,
    DC.overlays := overlays.map(o => target.value / o),
    DC.env := env,
    DC.profile := profileName.flatMap(pn => profileVersion.map(pv => pn -> pv)),
    DC.maintainer := maintainer
  )

}
