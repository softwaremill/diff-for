package com.softwaremill.diffx.test

import com.github.difflib.patch.{AbstractDelta, Chunk, InsertDelta, Patch}
import com.github.difflib.text.DiffRow.Tag
import com.github.difflib.text.{DiffRowGenerator}
import com.softwaremill.diffx.{
  ConsoleColorConfig,
  Diff,
  DiffContext,
  DiffResult,
  DiffResultAdditional,
  DiffResultMissing,
  DiffResultString,
  DiffResultValue,
  IdenticalValue
}
import com.softwaremill.diffx.instances.string.ExDiff
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util
import java.util.{ArrayList, List}
import scala.collection.JavaConverters._

//TODO diffUtil whole text
//for each different line where left is not empty & right is not empty
//perform diffUtil => save as DiffResultLine(rs: List[DiffResult])
class DiffStringTest extends AnyFreeSpec with Matchers {
  "1" in {
    val right = "kamil"
    val left = "kasper cos tam\nkamil"
    display(right, left)
  }

  "2" in {
    val right = "kamil kamil kamil"
    val left = "kamil kamil"
    display(right, left)
  }

  private def display(right: String, left: String) = {
    println(new ExDiff(left, right).unifiedDiff.show())
    println(Diff.diffForString.apply(left, right).show())
    println(new DiffBetterString().apply(left, right).show())
  }
}

class DiffBetterString extends Diff[String] {
  val strike = "\u0336"
  override def apply(left: String, right: String, context: DiffContext): DiffResult = {
    val generator = DiffRowGenerator
      .create()
      .showInlineDiffs(true)
      .mergeOriginalRevised(true)
      .inlineDiffByWord(true)
      .oldTag(f => "") //introduce markdown style for strikethrough
      .newTag(f => "") //introduce markdown style for bold
      .build();
    val rows = generator.generateDiffRows(left.split("\n").toList.asJava, right.split("\n").toList.asJava)
    DiffResultString(rows.asScala.toList.map { row =>
      row.getTag match {
        case Tag.INSERT => DiffResultMissing(row.getNewLine)
        case Tag.DELETE => DiffResultAdditional(row.getOldLine)
        case Tag.CHANGE =>
          if (row.getNewLine.isEmpty) {
            DiffResultAdditional(row.getOldLine)
          } else if (row.getOldLine.isEmpty) {
            DiffResultMissing(row.getNewLine)
          } else {
            DiffResultValue(row.getNewLine, row.getOldLine)
          }
        case Tag.EQUAL => IdenticalValue(row.getNewLine)
      }
    })
  }

}
