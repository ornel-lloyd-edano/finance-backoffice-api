package tech.pegb.backoffice.dao

import tech.pegb.backoffice.dao.DaoError._
import tech.pegb.backoffice.util.Logging

trait DaoErrorHandler extends Logging {

  protected def rowParsingError(msg: String) = RowParsingError(msg)

  protected def genericDbError(msg: String) = GenericDbError(msg)

  protected def constraintViolationError(msg: String) = ConstraintViolationError(msg)

  protected def entityAlreadyExistsError(msg: String) = EntityAlreadyExistsError(msg)

  protected def entityNotFoundError(msg: String) = EntityNotFoundError(msg)

  protected def wrongCredsError(msg: String) = WrongCredentials(msg)

  protected def timeoutError(msg: String) = TimeoutError(msg)

  protected def preconditionFailed(msg: String) = PreconditionFailed(msg)

  protected def connectionFailed(msg: String) = ConnectionFailed(msg)
}
