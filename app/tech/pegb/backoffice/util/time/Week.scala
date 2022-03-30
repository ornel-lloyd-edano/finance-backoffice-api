package tech.pegb.backoffice.util.time

case class Week(weekNumber: Int, year: Int) {
  assert(weekNumber > 0 && weekNumber <= 53, "invalid week number")
  assert(year > 0 && year < 9999, "invalid year")
  override def toString = s"${weekNumber}${
    weekNumber % 10 match {
      case _ if Seq(11, 12, 13).contains(weekNumber) ⇒ "th"
      case 1 ⇒ "st"
      case 2 ⇒ "nd"
      case 3 ⇒ "rd"
      case _ ⇒ "th"
    }
  } Week, $year"

  def isBefore(that: Week): Boolean = {
    this.year < that.year || (this.year == that.year && this.weekNumber < that.weekNumber)
  }
}

object Week {
  val WeekPattern = s"""([\\d]+(st|nd|rd|th)[ ]Week, [\\d]+)""".r
  def parse(arg: String): Week = {

    WeekPattern.findFirstIn(arg) match {
      case Some(captured) ⇒ //ex. 52nd Week, 2019
        val weekNum = captured.split(" ").head.replaceAll("st|nd|rd|th", "").toInt
        val year = captured.split(", ").tail.head.trim.toInt
        Week(weekNum, year)
      case None ⇒ //ex. 52, 2019
        val arr = arg.split(", ").map(_.toInt)
        Week(arr(0), arr(1))
    }

  }
}
