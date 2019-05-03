lazy val root = (project in file("."))
  .enablePlugins(
    Sonatype,
    SbtPlugin
  )
  .settings(
    organization := "de.wayofquality.blended",
    name := "sbt-blendedbuild",
    version := "0.1.0",

    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),

    scmInfo := Some(
      ScmInfo(
        url("https://github.com/woq-blended/sbt-blendedbuild"),
        "scm:git@github.com:woq-blended/sbt-blendedbuild.git"
      )
    ),

        developers := List(
      Developer(id = "atooni", name = "Andreas Gies", email = "andreas@wayofquality.de", url = url("https://github.com/atooni")),
      Developer(id = "lefou", name = "Tobias Roeser", email = "tobias.roser@tototec.de", url = url("https://github.com/lefou"))
    ),

    scalaVersion := "2.12.8",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-encoding", "UTF-8"
    ),

    libraryDependencies ++= Seq(
      Dependencies.sbtFilterResources.value,
      Dependencies.sbtNativePackager.value,
      Dependencies.sbtPhoenix.value,
      Dependencies.sbtOsgi.value
    ),

    // Scripted plugin allows us the integration test the sbt plugin
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024m",
      s"-Dplugin.org=${organization.value}",
      s"-Dplugin.name=${name.value}",
      s"-Dplugin.version=${version.value}"
    ),
    scriptedBufferLog := false,

    // publishing
    publishMavenStyle := true,
    publishArtifact in Test := false,
    credentials ++= (for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
      } yield Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        username,
        password)).toSeq,
    publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value) {
          Some("snapshots" at nexus + "content/repositories/snapshots")
        } else {
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
        }
      },

    sonatypeProfileName := "de.wayofquality"
  )
