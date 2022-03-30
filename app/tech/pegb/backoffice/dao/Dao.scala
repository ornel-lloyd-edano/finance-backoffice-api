package tech.pegb.backoffice.dao

import java.sql.{Connection, SQLException}
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.implicits._
import play.api.db.{DBApi, Database}
import tech.pegb.backoffice.dao.customer.abstraction.Transactional
import tech.pegb.backoffice.util.Utils

import scala.util.control.NonFatal
import scala.util.{Either, Try}

trait Dao extends DaoErrorHandler with Transactional {

  type DaoResponse[T] = Either[DaoError, T]

  def db: Database

  //consider refactoring dbApi member away from Dao, because it may not make sense for filesystem dao
  protected val dbApi: DBApi

  def nowUTC: ZonedDateTime = Dao.nowUTC

  protected def isUniqueConstraintViolation(e: SQLException): Boolean = {
    // 1st one is mysql, 2nd is h2
    e.getMessage.contains("Duplicate entry") || e.getMessage.contains("Unique index or primary key violation")
  }

  protected def isDataConstraintViolation(e: SQLException): Boolean = {
    // 1st is mysql/h2, 2nd is h2
    val err = e.getMessage
    err.contains("too long for column") || err.contains("constraint violation")
  }

  def withTransaction[T](block: Connection ⇒ T): T = {
    db.withTransaction(block)
  }

  protected def withConnection[T](
    block: Connection ⇒ T,
    errorMsg: ⇒ String,
    handlerPF: PartialFunction[Throwable, DaoError] = PartialFunction.empty): DaoResponse[T] = {
    Try(db.withConnection(block)).toEither.leftMap(printStackTrace andThen handlerPF.orElse(nonFatalPF(errorMsg)))
  }

  private def printStackTrace: PartialFunction[Throwable, Throwable] = {
    case error: Throwable ⇒
      error.printStackTrace()
      error
  }

  protected def withConnectionAndFlatten[T](
    block: ⇒ Connection ⇒ DaoResponse[T],
    errorMsg: ⇒ String,
    handlerPF: PartialFunction[Throwable, DaoError] = PartialFunction.empty): DaoResponse[T] = {
    Try(db.withConnection(block)).toEither
      .left.map(e ⇒ handlerPF.andThen(Left(_)).applyOrElse(e, leftNonFatalPF(errorMsg)))
      .merge
  }

  def withTransaction[T](
    block: Connection ⇒ T,
    errorMsg: ⇒ String,
    handlerPF: PartialFunction[Throwable, DaoError] = PartialFunction.empty): DaoResponse[T] = {
    Try(db.withTransaction(block)).toEither.left.map(handlerPF.orElse(nonFatalPF(errorMsg)))
  }

  protected def withTransactionAndFlatten[T](
    block: Connection ⇒ DaoResponse[T],
    errorMsg: ⇒ String,
    handlerPF: PartialFunction[Throwable, DaoError] = PartialFunction.empty): DaoResponse[T] = {
    Try(db.withTransaction(block)).toEither
      .left.map(e ⇒ handlerPF.andThen(Left(_)).applyOrElse(e, leftNonFatalPF(errorMsg)))
      .merge
  }

  protected def safeRunUpdate(block: Connection ⇒ Int, notFoundMsg: ⇒ String, errorMsg: ⇒ String): DaoResponse[Unit] = {
    Try {
      if (db.withTransaction(block) > 0) {
        Right(())
      } else {
        Left(entityNotFoundError(notFoundMsg))
      }
    }.toEither.left.map(leftNonFatalPF(errorMsg)).merge
  }

  protected def nonFatalPF(errorMsg: ⇒ String): PartialFunction[Throwable, DaoError] = {
    case NonFatal(exc) ⇒
      val err = genericDbError(errorMsg)
      logger.error(err.toString, exc)
      err
  }

  protected def leftNonFatalPF[T](errorMsg: ⇒ String): Throwable ⇒ Left[DaoError, T] = {
    nonFatalPF(errorMsg).andThen(Left(_))
  }

  def isInMemory: Boolean = dbApi.databases().head.url.contains("jdbc:h2:mem")
}

object Dao {
  type DaoResponse[T] = Either[DaoError, T]
  type OldEntityId = UUID

  sealed trait EntityId {
    def id: String

    override def toString: String = id
  }

  case class UUIDEntityId(arg: UUID) extends EntityId {
    override val id: String = arg.toString
  }

  case class IntEntityId(arg: Int) extends EntityId {
    override val id: String = arg.toString
  }

  val tz: ZoneOffset = Utils.tz

  def nowUTC: ZonedDateTime = Utils.now()

  abstract class DaoException(val cause: Throwable) extends RuntimeException(cause)

  class EntityNotFoundException(val entityId: OldEntityId, override val cause: Throwable)
    extends DaoException(cause)

  class UniqueConstraintViolationException(val entityId: OldEntityId, override val cause: SQLException)
    extends DaoException(cause)
}
