package tech.pegb.backoffice.mapping.dao.domain

import java.util.UUID

import cats.syntax.either._
import org.slf4j.Logger
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.DaoError._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.ServiceError._

object Implicits {

  implicit class DaoErrorDomainAdapter(val arg: DaoError) extends AnyVal {
    def asDomainError: ServiceError = arg match {
      case err: EntityAlreadyExistsError ⇒ duplicateError(err.message, Some(err.id))
      case err: EntityNotFoundError ⇒ notFoundError(err.message, Some(err.id))
      case err: ConstraintViolationError ⇒ validationError(err.message, Some(err.id))
      case err: WrongCredentials ⇒ notAuthorizedError(err.message, Some(err.id))
      case err: PreconditionFailed ⇒ staleResourceAccessError(err.message, Some(err.id))
      case err: ConnectionFailed ⇒ notAvailableError(err.message, Some(err.id))
      case err: TimeoutError ⇒ notAvailableError(err.message, Some(err.id))
      case e ⇒ unknownError(e.message, Some(e.id))
    }
  }

  implicit class DaoToServiceResponseConverter[T, R](val daoResponse: DaoResponse[T]) extends AnyVal {
    def asServiceResponse(implicit logger: Logger) = daoResponse.leftMap(error ⇒ {
      logger.debug(s"Error encountered in Dao ${error}")
      error.asDomainError
    })

    def asServiceResponse(f: T ⇒ R): ServiceResponse[R] = daoResponse.bimap(_.asDomainError, f)
  }

  implicit class BooleanDaoResponseConverter(val daoResponse: DaoResponse[Boolean]) extends AnyVal {
    def validateBoolResp(err: ⇒ String)(implicit requestId: UUID, logger: Logger): ServiceResponse[Unit] = {
      daoResponse
        .asServiceResponse
        .flatMap(flag ⇒ if (flag) Right(()) else Left(ServiceError.validationError(err, Some(requestId))))
    }
  }

}
