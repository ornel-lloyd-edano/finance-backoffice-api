package tech.pegb.backoffice.util.time

import java.text.SimpleDateFormat
import java.util.{Calendar, Locale}

case class Month(monthNumber: Int, year: Int) {
  assert(monthNumber >= 1 && monthNumber <= 12, "invalid month, 1 to 12 only")
  assert(year > 0 && year < 9999, "invalid year")
  override def toString = {
    val c = Calendar.getInstance()
    c.set(Calendar.MONTH, monthNumber - 1) //Calendar api treats 0 as starting point of month
    s"${c.getDisplayName(Calendar.MONTH, Calendar.SHORT_FORMAT, Locale.US)} $year"
  }

  def isBefore(that: Month): Boolean = {
    this.year < that.year || (this.year == that.year && this.monthNumber < that.monthNumber)
  }
}

object Month {
  val MonthPattern = s"""([A-Z-a-z]+ [\\d]+)""".r
  def parse(arg: String): Month = {
    MonthPattern.findFirstIn(arg) match {
      case Some(captured) ⇒ //ex. Jan 2019

        val monthSplit = captured.split(" ")

        val date = new SimpleDateFormat("MMM").parse(monthSplit(0).trim)
        val c = Calendar.getInstance()
        c.setTime(date)
        val monthNum = c.get(Calendar.MONTH) + 1 //Calendar api treats 0 as starting point of month

        val year = monthSplit(1).trim.toInt

        Month(monthNum, year)
      case None ⇒ //1, 2019
        val arr = arg.split(", ").map(_.toInt)
        Month(arr(0), arr(1))
    }
  }
}
