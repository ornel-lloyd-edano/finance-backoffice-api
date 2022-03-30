package tech.pegb.backoffice.util

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import java.util.concurrent.TimeoutException
import java.util.{TimerTask, UUID}

import org.apache.commons.text.StringEscapeUtils
import org.slf4j.Logger
import play.api.libs.json.JsString
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.util.Utils.timer

import scala.collection.TraversableOnce
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

object Implicits {
  implicit class Escaper(val arg: String) extends AnyVal {
    def escapeXss: String = {
      JsString(arg).toString
    }

    //This is the right fix
    def escapeJava: String = {
      StringEscapeUtils.escapeJava(arg)
    }
  }

  private val replaceAll = (arg: String) ⇒ {
    arg.replace("""'""", "")
      .replace(""""""", "")
      .replace("""%27""", "")
      .replace("""\0""", "")
      .replace("""\b""", "")
      .replace("""\n""", "")
      .replace("""\r""", "")
      .replace("""\t""", "")
      .replace("""\Z""", "")
      .replace("""\_""", "")
      .replace("""%""", "")
      .replace("""\z1a""", "")
      .replaceAll("""<(|\/|[^\/>][^>]+|\/[^>][^>]+)>""", "")
      .replaceAll("""<[^>]*>""", "")
      .replace("""\""", "")
      .replace("""{""", "")
      .replace("""}""", "")
  }

  implicit class Sanitizer(val arg: String) extends AnyVal {
    def sanitize: String = {
      replaceAll(arg)
    }
  }

  val integerPattern = """([-]?[\d]+)""".r
  val floatPattern = """([-]?[\d]+[.][\d]+)""".r
  val datePattern = """(\d\d\d\d)-(\d\d)-(\d\d)""".r
  val booleanPattern = """(true|TRUE|false|FALSE)""".r
  val arrayPattern = """(\[.+\])""".r
  val jsonPattern = """(\{.+\})""".r

  val enclosedStringPattern = """(["'`]?)((?:\\\1|.)*?)\1""".r

  implicit class RichString(val arg: String) extends AnyVal {
    def hasSomething: Boolean = {
      arg != null && arg.trim.nonEmpty
    }

    def ===(rightSide: String): Boolean = {
      arg.trim.equalsIgnoreCase(rightSide.trim)
    }

    def !==(rightSide: String): Boolean = {
      !(===(rightSide))
    }

    def toSeqByComma: Seq[String] = {
      arg.split(",").map(_.trim).filterNot(_.isEmpty).toSeq
    }

    def lenientContains(rightSide: String): Boolean = {
      arg.trim.toLowerCase.contains(rightSide.trim.toLowerCase)
    }

    def stripEnclosingQuotes = {
      arg.trim match {
        case enclosedStringPattern(_, enclosedString) ⇒ enclosedString
      }
    }

    def smartReplace(toFind: String, toReplace: String): (String, Int) = {
      val timesFound = arg.sliding(toFind.length).count(_ == toFind)
      (arg.replace(toFind, toReplace), timesFound)
    }

    def trimIfTooLong(implicit appConfig: AppConfig): String = {
      val maxLength = appConfig.LogMaxLength
      if (arg.length > maxLength) s"${arg.substring(0, maxLength)}...[trimmed, too long to show]" else arg
    }
  }

  implicit class RichOptionString(arg: Option[String]) {
    def ===(rightSide: String): Boolean = {
      if (arg.nonEmpty) {
        arg.get.equalsIgnoreCase(rightSide)
      } else {
        false
      }
    }

    def !==(rightSide: String): Boolean = {
      !(===(rightSide))
    }

    def hasSomething: Boolean = {
      arg != null && arg.nonEmpty && arg.exists(_.trim.nonEmpty)
    }

    def isReallyEmpty: Boolean = {
      arg == null || arg.isEmpty || arg.forall(_.trim.isEmpty)
    }

    def toSeqByComma: Seq[String] = {
      arg.map(_.split(",").map(_.trim).filterNot(_.isEmpty).toSeq).getOrElse(Seq.empty)
    }

    def lenientContains(rightSide: String): Boolean = {
      arg.map(_.trim.toLowerCase).contains(rightSide.trim.toLowerCase)
    }
  }

  implicit class RichIterableString(val arg: Iterable[String]) extends AnyVal {
    def containsOnly(item: String): Boolean = {
      if (arg.isEmpty)
        false
      else
        arg.find(i ⇒ i !== item).map(_ ⇒ false).getOrElse(true)
    }

    def containsOnly(items: Iterable[String]): Boolean = {
      arg.toSet.diff(items.toSet).size == 0
    }

    def lenientContains(item: String): Boolean = {
      // change to handle accont types
      if (item.split(",").length > 1)
        this.lenientContainsAll(item.split(","))
      else
        arg.map(_.trim.toLowerCase).find(_ === item).isDefined
    }

    def lenientContainsAll(items: Iterable[String]): Boolean = {
      val seq = arg.toSeq
      items.forall(item ⇒ seq.lenientContains(item))
    }
  }

  implicit class UUIDValidator(arg: String) {
    val uuidOrIntRegex = "((?i)^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-5][0-9a-f]{3}-?[089ab3][0-9a-f]{3}-?[0-9a-f]{12}$|\\d+)".r

    def isUuidOrInt: Boolean = {
      uuidOrIntRegex.pattern.matcher(arg).matches()
    }
  }

  implicit class RichThrowable(val arg: Throwable) extends AnyVal {
    def getCleanedMessage: String = {
      arg.getMessage.replace("assertion failed: ", "")
    }
  }

  implicit class FutureExtensions[T](future: Future[T]) {

    //http://justinhj.github.io/2017/07/16/future-with-timeout.html
    def futureWithTimeout(implicit timeout: FiniteDuration, ec: ExecutionContext): Future[T] = {
      // Promise will be fulfilled with either the callers Future or the timer task if it times out
      val p = Promise[T]

      // and a Timer task to handle timing out

      val timerTask = new TimerTask {
        def run(): Unit = {
          p.tryFailure(new TimeoutException("Config future-timeout has been reached."))
        }
      }

      // Set the timeout to check in the future
      timer.schedule(timerTask, timeout.toMillis)

      future.map {
        a ⇒
          if (p.trySuccess(a)) {
            timerTask.cancel()
          }
      }
        .recover {
          case e: Exception ⇒
            if (p.tryFailure(e)) {
              timerTask.cancel()
            }
        }

      p.future
    }

  }

  implicit class RichLocalDate(val arg: java.time.LocalDate) extends AnyVal {
    def atEndOfDay = arg.atTime(java.time.LocalTime.MAX)
  }

  import scala.reflect.ClassTag
  import scala.reflect.runtime.universe._

  implicit class RichCaseClass[T](arg: T)(implicit ev: TypeTag[T], ev2: ClassTag[T]) {
    def toSmartString = {
      val tpe = ev.tpe
      val allAccessors = tpe.decls.collect { case method: MethodSymbol if method.isCaseAccessor ⇒ method }

      val m = runtimeMirror(getClass.getClassLoader)
      val im = m.reflect(arg)
      val caseClassName = arg.getClass.getSimpleName
      allAccessors.map { sym ⇒
        val fldMirror = im.reflectField(sym)

        val value = fldMirror.get
        sym.name + " = " + value
      }.mkString(s"$caseClassName(", ", ", ")")
    }
  }

  implicit class RichIterable[T](val arg: Iterable[T]) extends AnyVal {
    def mkStringOrEmpty(prefx: String, sepr: String, sufx: String): String = {
      if (arg.nonEmpty) arg.mkString(prefx, sepr, sufx) else ""
    }

    def defaultMkString: String = {
      arg match {
        case arg1: Set[String] ⇒ if (arg1.nonEmpty) arg1.toSeq.sorted.mkString("[", ", ", "]") else "[]"
        case _ ⇒ if (arg.nonEmpty) arg.mkString("[", ", ", "]") else "[]"
      }
    }
  }

  implicit class RichAny[T](val arg: T) extends AnyVal {
    def toFuture: Future[T] = Future.successful(arg)

    def toRight[L]: Either[L, T] = Right(arg)

    def toLeft[R]: Either[T, R] = Left(arg)

    def toOption = Option(arg)
  }

  implicit class ZoneDateTimeConverter(val zonedDateTime: ZonedDateTime) extends AnyVal {
    def toLocalDateTimeUTC: LocalDateTime = LocalDateTime.ofInstant(zonedDateTime.toInstant, Utils.tz).withNano(0)
  }

  implicit class LocalDateTimeConverter(val localDateTime: LocalDateTime) extends AnyVal {
    def toZonedDateTimeUTC: ZonedDateTime = localDateTime.atZone(Utils.tz).withNano(0)
  }

  implicit class LocalDateConverter(val localDate: LocalDate) extends AnyVal {
    def toZonedDateTimeUTC: ZonedDateTime = localDate.atStartOfDay(Utils.tz).withNano(0)
  }

  implicit class ZonedDateTimeStringConverter(val localDateTime: String) extends AnyVal {
    def toZonedDateTimeUTC: ZonedDateTime = {
      LocalDateTime.parse(localDateTime).atZone(Utils.tz).withNano(0)
    }

  }

  implicit class UUIDLikeAdapter(val arg: UUID) extends AnyVal {
    def toUUIDLike = UUIDLike(arg.toString)
  }

  implicit class RichEither[L, T1, T2](val arg: Either[L, Tuple2[T1, Option[T2]]]) extends AnyVal {
    def toTuple2FirstOneEither: (Either[L, T1], Option[T2]) = arg match {
      case Left(error) ⇒ (Left(error), None)
      case Right((item1, maybeItem2)) ⇒ (Right(item1), maybeItem2)
    }
  }
  implicit class BigDecimalReciprocalAdapter(val arg: BigDecimal) extends AnyVal {
    def getReciprocal: BigDecimal = BigDecimal(1) / arg
  }

  implicit class StringCleaner(str: String) {
    def toValidString: String = {
      str.replaceAll("\\$", "").trim
    }
  }
  implicit class ThrowableLogger(val arg: Throwable) extends AnyVal {
    def log(customDescription: String = "", level: String = "error")(implicit logger: Logger): Throwable = {
      if (level === "error") {
        logger.error(customDescription, arg)
      } else if (level === "warn") {
        logger.warn(customDescription, arg)
      } else if (level === "info") {
        logger.info(customDescription, arg)
      } else {
        logger.debug(customDescription, arg)
      }
      arg
    }
  }

  implicit class RichInt(val arg: Int) extends AnyVal {
    def toBoolean = arg > 0
  }

  implicit class RichBoolean(val arg: Boolean) extends AnyVal {
    def toInt = if (arg) 1 else 0

    def toEither[L, R](onFalse: ⇒ L, onTrue: ⇒ R): Either[L, R] = if (arg) Right(onTrue) else Left(onFalse)
  }

  implicit class RichOption[T](val arg: Option[T]) extends AnyVal {
    //Note: somehow behaves a bit differently than the Try().toEither... maybe rename this one?
    def toEither: Either[None.type, T] = if (arg.isDefined) Right(arg.get) else Left(None)
  }

  implicit class DateRangeToCriteria(arg: DateRangeCriteriaWrapper) {
    def toCriteriaOption: Option[CriteriaField[_]] = (arg.dateFrom, arg.dateTo) match {
      case (Some(dateFrom), Some(dateTo)) ⇒
        CriteriaField[(LocalDateTime, LocalDateTime)](arg.columnName, (dateFrom.atStartOfDay, dateTo.atEndOfDay), MatchTypes.InclusiveBetween).toOption
      case (Some(dateFrom), None) ⇒
        CriteriaField[LocalDateTime](arg.columnName, dateFrom.atStartOfDay, MatchTypes.GreaterOrEqual).toOption
      case (None, Some(dateTo)) ⇒
        CriteriaField[LocalDateTime](arg.columnName, dateTo.atEndOfDay, MatchTypes.LesserOrEqual).toOption
      case _ ⇒ None
    }
  }

  implicit class DateTimeRangeToCriteria(arg: DateTimeRangeCriteriaWrapper) {
    def toCriteriaOption: Option[CriteriaField[_]] = (arg.dateTimeFrom, arg.dateTimeTo) match {
      case (Some(dateFrom), Some(dateTo)) ⇒
        CriteriaField[(LocalDateTime, LocalDateTime)](arg.columnName, (dateFrom, dateTo), MatchTypes.InclusiveBetween).toOption
      case (Some(dateFrom), None) ⇒
        CriteriaField[LocalDateTime](arg.columnName, dateFrom, MatchTypes.GreaterOrEqual).toOption
      case (None, Some(dateTo)) ⇒
        CriteriaField[LocalDateTime](arg.columnName, dateTo, MatchTypes.LesserOrEqual).toOption
      case _ ⇒ None
    }
  }

  implicit class SeqExtension[A](s: TraversableOnce[A]) {
    def foldLeftToFuture[B](initial: B)(f: (B, A) ⇒ Future[B])(implicit ec: ExecutionContext): Future[B] =
      s.foldLeft(Future(initial))((future, item) ⇒ future.flatMap(f(_, item)))

    def mapInSeries[B](f: A ⇒ Future[B])(implicit ec: ExecutionContext): Future[Seq[B]] =
      s.foldLeftToFuture(Seq[B]())((seq, item) ⇒ f(item).map(seq :+ _))
  }

}

case class DateRangeCriteriaWrapper(dateFrom: Option[LocalDate], dateTo: Option[LocalDate], columnName: String)
case class DateTimeRangeCriteriaWrapper(dateTimeFrom: Option[LocalDateTime], dateTimeTo: Option[LocalDateTime], columnName: String)
