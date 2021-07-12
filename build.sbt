import com.softwaremill.UpdateVersionInDocs
import sbt.Def
import sbt.Reference.display

val scala212 = "2.12.14"
val scala213 = "2.13.6"

val scalaIdeaVersion = scala212 // the version for which to import sources into intellij

val scalatestVersion = "3.2.9"
val specs2Version = "4.12.3"
val smlTaggingVersion = "2.3.0"

lazy val commonSettings: Seq[Def.Setting[_]] = commonSmlBuildSettings ++ ossPublishSettings ++ Seq(
  organization := "com.softwaremill.diffx",
  scmInfo := Some(ScmInfo(url("https://github.com/softwaremill/diffx"), "git@github.com:softwaremill/diffx.git")),
  ideSkipProject := (scalaVersion.value != scalaIdeaVersion) || thisProjectRef.value.project.contains("JS"),
  updateDocs := Def.taskDyn {
    val files1 = UpdateVersionInDocs(sLog.value, organization.value, version.value)
    Def.task {
      (docs.jvm(scala213) / mdoc).toTask("").value
      files1 ++ Seq(file("generated-docs/out"))
    }
  }.value
)

val compileDocs: TaskKey[Unit] = taskKey[Unit]("Compiles docs module throwing away its output")
compileDocs := {
  (docs.jvm(scala213) / mdoc).toTask(" --out target/diffx-docs").value
}

val versionSpecificScalaSources = {
  Compile / unmanagedSourceDirectories := {
    val current = (Compile / unmanagedSourceDirectories).value
    val sv = (Compile / scalaVersion).value
    val baseDirectory = (Compile / scalaSource).value
    val suffixes = CrossVersion.partialVersion(sv) match {
      case Some((2, 13)) => List("2", "2.13+")
      case Some((2, _))  => List("2", "2.13-")
      case Some((3, _))  => List("3")
      case _             => Nil
    }
    val versionSpecificSources = suffixes.map(s => new File(baseDirectory.getAbsolutePath + "-" + s))
    versionSpecificSources ++ current
  }
}

lazy val core = (projectMatrix in file("core"))
  .settings(commonSettings)
  .settings(
    name := "diffx-core",
    libraryDependencies ++= Seq(
      "com.propensive" %%% "magnolia" % "0.17.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalatest" %%% "scalatest-flatspec" % scalatestVersion % Test,
      "org.scalatest" %%% "scalatest-freespec" % scalatestVersion % Test,
      "org.scalatest" %%% "scalatest-shouldmatchers" % scalatestVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % "2.3.0" % Test
    ),
    versionSpecificScalaSources
  )
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213)
  )

lazy val scalatest = (projectMatrix in file("scalatest"))
  .settings(commonSettings)
  .settings(
    name := "diffx-scalatest",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest-matchers-core" % scalatestVersion,
      "org.scalatest" %%% "scalatest-flatspec" % scalatestVersion % Test,
      "org.scalatest" %%% "scalatest-shouldmatchers" % scalatestVersion % Test
    )
  )
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213)
  )

lazy val specs2 = (projectMatrix in file("specs2"))
  .settings(commonSettings)
  .settings(
    name := "diffx-specs2",
    libraryDependencies ++= Seq(
      "org.specs2" %%% "specs2-core" % specs2Version
    )
  )
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213)
  )

lazy val utest = (projectMatrix in file("utest"))
  .settings(commonSettings)
  .settings(
    name := "diffx-utest",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % "0.7.10"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213)
  )

lazy val munit = (projectMatrix in file("munit"))
  .settings(commonSettings)
  .settings(
    name := "diffx-munit",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % "0.7.26"
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213),
    settings = commonSettings ++ Seq(scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) })
  )

lazy val tagging = (projectMatrix in file("tagging"))
  .settings(commonSettings)
  .settings(
    name := "diffx-tagging",
    libraryDependencies ++= Seq(
      "com.softwaremill.common" %%% "tagging" % smlTaggingVersion,
      "org.scalatest" %%% "scalatest-flatspec" % scalatestVersion % Test,
      "org.scalatest" %%% "scalatest-shouldmatchers" % scalatestVersion % Test
    )
  )
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213)
  )

lazy val cats = (projectMatrix in file("cats"))
  .settings(commonSettings)
  .settings(
    name := "diffx-cats",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.6.1",
      "org.scalatest" %%% "scalatest-freespec" % scalatestVersion % Test,
      "org.scalatest" %%% "scalatest-shouldmatchers" % scalatestVersion % Test
    )
  )
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213)
  )

lazy val refined = (projectMatrix in file("refined"))
  .settings(commonSettings)
  .settings(
    name := "diffx-refined",
    libraryDependencies ++= Seq(
      "eu.timepit" %%% "refined" % "0.9.26",
      "org.scalatest" %%% "scalatest-flatspec" % scalatestVersion % Test,
      "org.scalatest" %%% "scalatest-shouldmatchers" % scalatestVersion % Test
    )
  )
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = List(scala212, scala213)
  )
  .jsPlatform(
    scalaVersions = List(scala212, scala213)
  )
//

lazy val docs = (projectMatrix in file("generated-docs")) // important: it must not be docs/
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    name := "docs",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.scalatest" %% "scalatest-shouldmatchers" % scalatestVersion
    ),
    mdocIn := file("docs-sources"),
    moduleName := "diffx-docs",
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    mdocOut := file("generated-docs/out")
  )
  .dependsOn(core, scalatest, specs2, utest, refined, tagging, cats, munit)
  .jvmPlatform(scalaVersions = List(scala213))

val testJVM = taskKey[Unit]("Test JVM projects")
val testJS = taskKey[Unit]("Test JS projects")

val allAggregates =
  core.projectRefs ++ scalatest.projectRefs ++
    specs2.projectRefs ++ utest.projectRefs ++ cats.projectRefs ++
    refined.projectRefs ++ tagging.projectRefs ++ docs.projectRefs ++ munit.projectRefs

def filterProject(p: String => Boolean) =
  ScopeFilter(inProjects(allAggregates.filter(pr => p(display(pr.project))): _*))

lazy val rootProject = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    name := "diffx",
    scalaVersion := scalaIdeaVersion,
    testJVM := (Test / test).all(filterProject(p => !p.contains("JS") && !p.contains("Native"))).value,
    testJS := (Test / test).all(filterProject(_.contains("JS"))).value
  )
  .aggregate(allAggregates: _*)
