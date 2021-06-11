package com.softwaremill.diffx.specs2

import com.softwaremill.diffx.{ConsoleColorConfig, Diff, DiffResultDifferent}
import org.specs2.matcher.{Expectable, MatchResult, Matcher}

trait DiffMatcher {
  def matchTo[A: Diff](left: A)(implicit c: ConsoleColorConfig): DiffForMatcher[A] = DiffForMatcher(left)

  case class DiffForMatcher[A: Diff](right: A) extends Matcher[A] {
    override def apply[S <: A](left: Expectable[S]): MatchResult[S] = {
      val diff = Diff[A]
      result(
        test = {
          diff.apply(left.value, right).isIdentical
        },
        okMessage = "",
        koMessage = diff.apply(left.value, right) match {
          case c: DiffResultDifferent =>
            c.show()
          case _ =>
            ""
        },
        left
      )
    }
  }
}

object DiffMatcher extends DiffMatcher
