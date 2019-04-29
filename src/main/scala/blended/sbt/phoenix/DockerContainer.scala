package blended.sbt.phoenix

import blended.sbt.dockercontainer.BlendedDockerContainerPlugin
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin.autoImport.{BlendedDockerContainer => DC}
import blended.sbt.container.BlendedContainerPlugin.{autoImport => CP}
import phoenix.ProjectConfig
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

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

  def profileName: String
  def profileVersion: String

  override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(BlendedDockerContainerPlugin)

  override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
    CP.blendedVersion := blendedVersion,
    DC.appFolder := folder,
    DC.imageTag := imageTag,
    DC.ports := ports,
    DC.overlays := overlays.map(o => target.value / o),
    DC.env := env,
    DC.profile := profileName -> profileVersion
  )

}
