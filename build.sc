import $ivy.`com.goyeau::mill-scalafix:0.2.6`

import mill.scalalib._
import mill.scalalib.scalafmt._
import com.goyeau.mill.scalafix.ScalafixModule

object luafu extends ScalaModule with ScalafmtModule with ScalafixModule {
  def scalaVersion  = "3.1.0"
  def scalacOptions = super.scalacOptions() ++ customScalaOpts

  def ivyDeps = Agg(
    ivy"org.ow2.asm:asm:9.2",
    ivy"org.ow2.asm:asm-commons:9.2"
  )

  def scalafixIvyDeps = super.scalafixIvyDeps() ++ Agg(
    ivy"com.github.liancheng::organize-imports:0.6.0"
  )

  def customScalaOpts =
    Seq(
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Wunused:all",
      "-Xfatal-warnings"
    )

  def check = T {
    T.sequence(Seq(fix("--check"), checkFormat()))
  }
}
