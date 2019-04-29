package blended.sbt.dockercontainer

import java.io.File
import java.net.URLClassLoader

import scala.sys.process.Process

import blended.sbt.container.BlendedContainerPlugin
import blended.sbt.container.BlendedContainerPlugin.autoImport.{materializeToolsCp, materializeToolsDeps}
import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}
import de.wayofquality.sbt.filterresources.FilterResources
import sbt.{Def, _}
import sbt.Keys._
import sbt.librarymanagement.{ModuleID, UnresolvedWarning, UnresolvedWarningConfiguration, UpdateConfiguration}

object BlendedDockerContainerPlugin extends AutoPlugin {

  object autoImport {

    /**
     * This object exists, to by-default prefix all tasks.
     * That way, you can access all task/settings by `BlendedDockerContainer.settings`.
     * You can also `import BlendedDockerContainer._`
     * or rename to a short prefix via `import BlendedDockerContainerPlugin.autoImport.{BlendedDockerContainer => BDC}`.
     */
    object BlendedDockerContainer {
      val containerImage = taskKey[(String, File)]("The container image and it's folder name")
      val generateDockerfile = taskKey[File]("Generate to dockerfile")
      val createDockerImage = taskKey[Unit]("Create the docker image")
      val generateOverlays = taskKey[Option[File]]("Generates to Overlays dir structure. Returns the container directory.")

      val dockerDir = settingKey[File]("The base directory for the docker image content")
      val maintainer = settingKey[String]("The maintainer of the docker image")
      val baseImage = settingKey[String]("The name of the base image (The FROM line in the Dockerfile)")
      val appFolder = settingKey[String]("The folder of target app (under /opt)")
      val appUser = settingKey[String]("The user who owns the application folder")
      val ports = settingKey[Seq[Int]]("The exposed ports")
      val imageTag = settingKey[String]("The image tag of the docker image")
      val overlays = settingKey[Seq[File]]("Additional blended container overlays to be applied to the image")
      val env = settingKey[Map[String, String]]("Additional environment variables used when running the overlay builder. Those will not be added to the docker image as ENV entry.")
      val profile = settingKey[(String, String)]("The profile name and version")
    }
  }

  import autoImport.BlendedDockerContainer._

  override def requires = super.requires &&
    FilterResources &&
    UniversalPlugin &&
    UniversalDeployPlugin

  override def trigger = NoTrigger

  override def projectSettings: Seq[Def.Setting[_]] = Seq(

    Compile / packageBin / publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,

    dockerDir := target.value / "docker",

    baseImage := "atooni/blended-base:latest",

    appUser := "blended",

    ports := Seq(),

    generateDockerfile := {
      // generate overlays
      val overlaysDockerCmd = generateOverlays.value match {
        case None => "# no overlays configured"
        case Some(_) => "ADD overlays /opt"
      }

      // make Dockerfile

      val dockerfile = dockerDir.value / "Dockerfile"

      val dockerconf = Seq(
        s"FROM ${baseImage.value}",
        s"MAINTAINER ${maintainer.value}",
        s"ADD ${containerImage.value._2.getName()} /opt",
        s"RUN ln -s /opt/${containerImage.value._1} /opt/${appFolder.value}",
        overlaysDockerCmd,
        s"RUN chown -R blended.blended /opt/${containerImage.value._1}",
        s"RUN chown -R blended.blended /opt/${appFolder.value}",
        s"USER ${appUser.value}",
        "ENV JAVA_HOME /opt/java",
        "ENV PATH ${PATH}:${JAVA_HOME}/bin",
        s"""ENTRYPOINT ["/bin/sh", "/opt/${appFolder.value}/bin/blended.sh"]"""
      ) ++
        ports.value.map(p => s"EXPOSE ${p}")

      IO.write(dockerfile, dockerconf.mkString("\n"))

      dockerfile
    },

    createDockerImage := {
      val log = streams.value.log

      // trigger dockerfile generator
      generateDockerfile.value

      // generate overlays
      generateOverlays.value.foreach { d =>
        IO.copyDirectory(d, dockerDir.value / "overlays")
      }

      // copy container pack into docker dir
      IO.copyFile(containerImage.value._2, dockerDir.value / containerImage.value._2.getName())

      log.info(s"Creating docker image ${imageTag.value}")
      Process(
        command = List("docker", "build", "-t", imageTag.value, "."),
        cwd = Some(dockerDir.value)
      ) ! log
      log.info(s"Created docker image ${imageTag.value}")

    },

    BlendedContainerPlugin.autoImport.materializeToolsDeps := Seq(
      "de.wayofquality.blended" %% "blended.updater.tools" % BlendedContainerPlugin.autoImport.blendedVersion.value
    ),

    materializeToolsCp := {
      val log = streams.value.log
      val depRes = (Compile / dependencyResolution).value

      materializeToolsDeps.value.flatMap { dep =>

        val resolved: Either[UnresolvedWarning, UpdateReport] =
          depRes.update(
            depRes.wrapDependencyInModule(dep, scalaModuleInfo.value),
            UpdateConfiguration(),
            UnresolvedWarningConfiguration(),
            log
          )

        val files = resolved match {
          case Right(report) =>
            val files: Seq[(ConfigRef, ModuleID, Artifact, File)] = report.toSeq
            files.map(_._4)
          case Left(w) => throw w.resolveException
        }

        files
      }
    },

    generateOverlays := {
      val log = streams.value.log

      // trigger dependency
      val (ciName, ciFile) = containerImage.value

      // generate overlays
      if (overlays.value.isEmpty) {
        // nothing to do
        None

      } else {

        val overlaysContainerDir = target.value / "generatedOverlays"
        if (overlaysContainerDir.exists()) IO.delete(overlaysContainerDir)
        overlaysContainerDir.mkdirs()

        val profileConf = s"${ciName}/profiles/${profile.value._1}/${profile.value._2}/profile.conf"

        // unpack only the profile.conf from the image
        log.info(s"Unpacking profile.conf from image: ${profileConf}")
        Process(
          command = List("tar", "xzf", ciFile.getAbsolutePath(), profileConf),
          cwd = overlaysContainerDir
        ) ! log

        // generate extra overlays (see maven updater plugin @add-overlays)
        val profileFile = (overlaysContainerDir / profileConf).getAbsolutePath()

        val overlayArgs = overlays.value.flatMap(o =>          Seq("--add-overlay-file", o.getAbsolutePath()))

        val envArgs = env.value.flatMap { case (k, v) => Seq("--env-var", k, v) }

        val builderArgs: Seq[String] = Seq(
          "--debug",
          "-f", profileFile,
          "-o", profileFile,
          "--write-overlays-config",
          "--create-launch-config", (overlaysContainerDir / ciName / "launch.conf").getAbsolutePath()
        ) ++ overlayArgs ++ envArgs

        log.info(s"Generating overlays with args: ${builderArgs}")
        val cl = new URLClassLoader(materializeToolsCp.value.map(_.toURI().toURL()).toArray, null)

        val builder = cl.loadClass("blended.updater.tools.configbuilder.RuntimeConfigBuilder")
        val runMethod = builder.getMethod("run", Seq(classOf[Array[String]]): _*)
        runMethod.invoke(null, builderArgs.toArray)

        // We don't want to replace the profile.conf
        IO.delete(overlaysContainerDir / profileConf)

        Some(overlaysContainerDir)
      }

    }
  )

}
