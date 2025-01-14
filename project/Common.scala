import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.core._, ProblemFilters._

object Common extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  val useShaded = settingKey[Boolean](
    "Use ReactiveMongo-Shaded (see system property 'reactivemongo.shaded')"
  )

  override def projectSettings = Seq(
    organization := "org.reactivemongo",
    autoAPIMappings := true,
    useShaded := sys.env.get("REACTIVEMONGO_SHADED").fold(true)(_.toBoolean),
    version := {
      val ver = (ThisBuild / version).value
      val suffix = {
        if (useShaded.value) "" // default ~> no suffix
        else "-noshaded"
      }

      ver.span(_ != '-') match {
        case (a, b) => s"${a}${suffix}${b}"
      }
    },
    target := {
      if (useShaded.value) target.value / "shaded"
      else target.value / "noshaded"
    },
    testFrameworks ~= { _.filterNot(_ == TestFrameworks.ScalaTest) },
    Compile / doc / scalacOptions := {
      val opts = (Test / scalacOptions).value ++ Seq(
        "-unchecked",
        "-deprecation"
      ) ++ Opts.doc.title(name.value)

      if (scalaBinaryVersion.value startsWith "3") {
        opts ++ Seq("-skip-by-id=highlightextractor")
      } else {
        opts ++ Seq("-skip-packages", "highlightextractor", "-implicits")
      }
    },
    resolvers ++= Seq(
      Resolver.sonatypeRepo("staging"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.typesafeRepo("releases")
    ),
    mimaFailOnNoPrevious := false,
    mimaPreviousArtifacts := {
      if (scalaBinaryVersion.value startsWith "2.") {
        Set(organization.value %% name.value.toLowerCase % "1.0.0")
      } else {
        Set.empty
      }
    },
    mimaBinaryIssueFilters ++= Seq(missingMethodInOld)
  )

  private val missingMethodInOld: ProblemFilter = {
    case ReversedAbstractMethodProblem(_) | ReversedMissingMethodProblem(_) =>
      false

    case DirectMissingMethodProblem(old)         => old.nonAccessible
    case InheritedNewAbstractMethodProblem(_, _) => false
    case IncompatibleResultTypeProblem(old, _)   => old.nonAccessible
    case IncompatibleMethTypeProblem(old, _)     => old.nonAccessible
    case MissingClassProblem(old)                => !old.isPublic
    case MissingTypesProblem(_, old)             => !old.exists(_.isPublic)
    case AbstractClassProblem(old)               => !old.isPublic
    case UpdateForwarderBodyProblem(old)         => !old.isPublic
    case _: NewMixinForwarderProblem             => false
    case _                                       => true
  }
}
