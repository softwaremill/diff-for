package com.softwaremill.diffx.instances.string

import com.softwaremill.diffx.DiffResultString

import scala.collection.JavaConverters._

class ExDiff(val obtained: String, val expected: String) extends Serializable {
  val obtainedClean: String = filterAnsi(obtained)
  val expectedClean: String = filterAnsi(expected)
  val obtainedLines: Seq[String] = splitIntoLines(obtainedClean)
  val expectedLines: Seq[String] = splitIntoLines(expectedClean)
  val unifiedDiff: DiffResultString = createUnifiedDiff(obtainedLines, expectedLines)

  def createReport(
      title: String,
      printObtainedAsStripMargin: Boolean = true
  ): String = {
    val sb = new StringBuilder
    if (title.nonEmpty) {
      sb.append(title)
        .append("\n")
    }
    if (obtainedClean.length < 1000) {
      header("Obtained", sb).append("\n")
      if (printObtainedAsStripMargin) {
        sb.append(asStripMargin(obtainedClean))
      } else {
        sb.append(obtainedClean)
      }
      sb.append("\n")
    }
    appendDiffOnlyReport(sb)
    sb.toString()
  }

  def createDiffOnlyReport(): String = {
    val out = new StringBuilder
    appendDiffOnlyReport(out)
    out.toString()
  }

  private def appendDiffOnlyReport(sb: StringBuilder): Unit = {
    sb.append(unifiedDiff)
  }

  private def asStripMargin(obtained: String): String = {
    if (!obtained.contains("\n")) obtained
    else {
      val out = new StringBuilder
      val lines = obtained.trim.linesIterator
      val head = if (lines.hasNext) lines.next() else ""
      out.append("    \"\"\"|" + head + "\n")
      lines.foreach(line => {
        out.append("       |").append(line).append("\n")
      })
      out.append("       |\"\"\".stripMargin")
      out.toString()
    }
  }

  private def header(t: String, sb: StringBuilder): StringBuilder = {
    sb.append(s"=> $t")
  }

  private def createUnifiedDiff(
      original: Seq[String],
      revised: Seq[String]
  ): DiffResultString = {
    val diff = DiffUtils.diff(original.asJava, revised.asJava)
    val result = DiffUtils
      .generateUnifiedDiff(
        "obtained",
        "expected",
        original.asJava,
        diff,
        1
      )
      .asScala
      .iterator
    DiffResultString(result.toList)
  }

  private def splitIntoLines(string: String): Seq[String] = {
    string.trim().replace("\r\n", "\n").split("\n").toIndexedSeq
  }

  private def filterAnsi(s: String): String = {
    if (s == null) {
      null
    } else {
      val len = s.length
      val r = new java.lang.StringBuilder(len)
      var i = 0
      while (i < len) {
        val c = s.charAt(i)
        if (c == '\u001B') {
          i += 1
          while (i < len && s.charAt(i) != 'm') i += 1
        } else {
          r.append(c)
        }
        i += 1
      }
      r.toString()
    }
  }
}
