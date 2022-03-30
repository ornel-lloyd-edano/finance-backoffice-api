package tech.pegb.backoffice.api

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import play.api.mvc.QueryStringBindable
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try

object QueryBinders {

  implicit def uuidLikeQueryBinder(implicit uuidLikeBinder: QueryStringBindable[String]): QueryStringBindable[UUIDLike] = {
    new QueryStringBindable[UUIDLike] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UUIDLike]] = {
        for {
          uuidLikeOrError ← uuidLikeBinder.bind(key, params)
        } yield {
          for {
            uuidLikeString ← uuidLikeOrError
            uuidLike ← {
              Try(UUIDLike(uuidLikeString)).toEither
                .left.map(er ⇒ s"Unable to bind a partial UUID from $uuidLikeString: $er")
            }
          } yield uuidLike
        }
      }

      override def unbind(key: String, value: UUIDLike): String = {
        uuidLikeBinder.unbind(key, value.underlying)
      }
    }
  }

  implicit def localDateTimeFromQueryBinder(implicit dateBinder: QueryStringBindable[String]): QueryStringBindable[LocalDateTimeFrom] = {
    new QueryStringBindable[LocalDateTimeFrom] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, LocalDateTimeFrom]] = {
        for {
          dateOrError ← dateBinder.bind(key, params)
        } yield {
          for {
            dateString ← dateOrError
            date ← {
              val dateToParse = if (dateString.length == 10) s"${dateString}T00:00:00" else dateString
              Try(LocalDateTime.parse(dateToParse)).toEither
                .left.map(er ⇒ s"Unable to bind a date from $dateString: $er")
            }
          } yield LocalDateTimeFrom(date)
        }
      }

      override def unbind(key: String, value: LocalDateTimeFrom): String = {
        dateBinder.unbind(key, value.localDateTime.toString)
      }
    }
  }

  implicit def localDateTimeToQueryBinder(implicit dateBinder: QueryStringBindable[String]): QueryStringBindable[LocalDateTimeTo] = {
    new QueryStringBindable[LocalDateTimeTo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, LocalDateTimeTo]] = {
        for {
          dateOrError ← dateBinder.bind(key, params)
        } yield {
          for {
            dateString ← dateOrError
            date ← {
              val dateToParse = if (dateString.length == 10) s"${dateString}T23:59:59" else dateString
              Try(LocalDateTime.parse(dateToParse)).toEither
                .left.map(er ⇒ s"Unable to bind a date from $dateString: $er")
            }
          } yield LocalDateTimeTo(date)
        }
      }

      override def unbind(key: String, value: LocalDateTimeTo): String = {
        dateBinder.unbind(key, value.localDateTime.toString)
      }
    }
  }

  implicit def localDateQueryBinder(implicit dateBinder: QueryStringBindable[String]): QueryStringBindable[LocalDate] = {
    new QueryStringBindable[LocalDate] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, LocalDate]] = {
        for {
          dateOrError ← dateBinder.bind(key, params)
        } yield {
          for {
            dateString ← dateOrError
            date ← Try(LocalDate.parse(dateString)).toEither
              .left.map(er ⇒ s"Unable to bind a date from $dateString: $er")
          } yield date
        }
      }

      override def unbind(key: String, value: LocalDate): String = {
        dateBinder.unbind(key, value.toString)
      }
    }
  }

  implicit def zonedDateTimeQueryBinder(implicit dateBinder: QueryStringBindable[String]): QueryStringBindable[ZonedDateTime] = {
    new QueryStringBindable[ZonedDateTime] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ZonedDateTime]] = {
        for {
          dateOrError ← dateBinder.bind(key, params)
        } yield {
          for {
            dateString ← dateOrError
            date ← Try(ZonedDateTime.parse(dateString)).toEither
              .left.map(er ⇒ s"Unable to bind a date from $dateString: $er")
          } yield date
        }
      }

      override def unbind(key: String, value: ZonedDateTime): String = {
        dateBinder.unbind(key, value.toString)
      }
    }
  }

}
