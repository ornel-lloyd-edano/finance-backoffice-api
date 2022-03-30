package tech.pegb.backoffice.domain.customer.implementation

import java.sql.Connection
import java.time.{Clock, LocalDateTime}
import java.util.{Currency, UUID}

import cats.instances.list._
import cats.instances.try_._
import cats.syntax.either._
import cats.syntax.traverse._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.account.dto.{AccountToUpdate}
import tech.pegb.backoffice.domain.account.dto.{AccountCriteria, AccountToCreate}
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.dao.customer.abstraction.{BusinessUserDao, IndividualUserDao, UserDao}
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountMainType, AccountNumber, AccountType}
import tech.pegb.backoffice.domain.{BaseService}
import tech.pegb.backoffice.domain.customer.abstraction.CustomerAccount
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future
import scala.util.{Try}

class CustomerAccountService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    businessUserDao: BusinessUserDao,
    individualUserDao: IndividualUserDao,
    accountDao: AccountDao,
    accountManagement: AccountManagement) extends CustomerAccount with BaseService {

  implicit val executionContext = executionContexts.blockingIoOperations

  val clock: Clock = Clock.systemDefaultZone()

  def openBusinessUserAccount(
    customerId: UUID,
    accountType: AccountType,
    currency: Currency,
    isMainAccount: Boolean,
    mainType: AccountMainType,
    createdBy: String,
    createdAt: LocalDateTime)(implicit maybeTransaction: Option[Connection] = None): Future[ServiceResponse[Account]] = ???

  def openIndividualUserAccount(customerId: UUID, accountToCreate: AccountToCreate): Future[ServiceResponse[Account]] = {
    accountManagement.createAccount(accountToCreate.copy(customerId = customerId), Some(conf.IndividualUserType))
  }

  def getAccounts(customerId: UUID): Future[ServiceResponse[Set[Account]]] = {
    Future {
      accountDao.getAccountsByUserId(customerId.toString).fold(
        _.asDomainError.toLeft,
        accountSet ⇒ {
          accountSet.map(acc ⇒ acc.asDomain).toList.sequence[Try, Account].toEither
            .map(_.toSet)
            .leftMap { throwable ⇒
              logger.error(s"Error in getAccounts", throwable)
              dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
            }
        })
    }
  }

  def getAccountByAccountNumber(accountNumber: AccountNumber): Future[ServiceResponse[Account]] = {
    Future {
      for {
        maybeAccount ← accountDao.getAccountByAccNum(accountNumber.underlying).asServiceResponse
        account ← maybeAccount.toRight {
          notFoundError(s"Account with accountNumber ${accountNumber.underlying} not found.")
        }
        maybeCustomerUuid ← userDao.getUUIDByInternalUserId(account.userId).asServiceResponse
        customerUuid ← maybeCustomerUuid.toRight {
          notFoundError(s"User with account internal userId ${account.userId} not found")
        }
        domainAcc ← account.asDomain.toEither.leftMap { throwable ⇒
          logger.error(s"Error in getAccountByAccountNumber", throwable)
          val cause = throwable.getMessage.replaceAll("assertion failed: ", "")
          dtoMappingError(s"Failed to convert account entity to domain. Cause by: $cause")
        }
      } yield domainAcc
    }
  }

  def getAccountsByCriteria(
    criteria: AccountCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Account]]] = {
    Future {
      accountDao.getAccountsByCriteria(Some(criteria.asDao), orderBy.asDao, limit, offset).fold(
        _.asDomainError.toLeft,
        accountSet ⇒ {
          accountSet.map(acc ⇒ {
            userDao.getUUIDByInternalUserId(acc.userId).fold(
              daoError ⇒ {
                logger.warn(s"Error in getAccountsByCriteria, getUUIDByInternalUserId: ${daoError.message}")
                None
              },
              _ match {
                case Some(customerUuid) ⇒
                  Some(acc.asDomain)
                case None ⇒
                  logger.warn(s"getAccountsByCriteria: internal user id ${acc.userId} cannot be found")
                  None
              })

          }).toList.flatten.sequence[Try, Account].toEither
            .map(_.toSeq)
            .leftMap { throwable ⇒
              logger.error(s"Error in getAccountsByCriteria", throwable)
              dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
            }
        })
    }
  }

  def getAccountByAccountName(accountName: NameAttribute): Future[ServiceResponse[Account]] = {
    Future {
      accountDao.getAccountByAccountName(accountName.underlying).fold(
        _.asDomainError.toLeft,
        account ⇒ {
          account.fold[ServiceResponse[Account]](Left(notFoundError(s"Account with accountName ${accountName.underlying} not found."))) {
            acc ⇒

              userDao.getUUIDByInternalUserId(acc.userId).fold(
                daoError ⇒ Left(unknownError(daoError.message)),
                _ match {
                  case Some(customerUuid) ⇒
                    acc.asDomain.toEither
                      .leftMap { throwable ⇒
                        logger.error(s"Error in getAccountByAccountName", throwable)
                        dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
                      }
                  case None ⇒
                    Left(notFoundError(s"User with account internal userId ${acc.userId} not found"))
                })
          }
        })
    }
  }

  def getMainAccount(customerId: UUID): Future[ServiceResponse[Account]] = {
    Future {
      accountDao.getMainAccountByUserId(customerId.toString).fold(
        _.asDomainError.toLeft,
        {
          case Some(account) ⇒
            account.asDomain.toEither
              .leftMap { throwable ⇒
                logger.error(s"Error in getMainAccount", throwable)
                dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
              }
          case _ ⇒
            Left(notFoundError(s"No main account found. Customer id [${customerId}] is missing."))
        })
    }
  }

  //TODO refactor method below
  def activateIndividualUserAccount(customerId: UUID, accountId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[Account]] = Future {
    individualUserDao.getIndividualUser(customerId.toString).fold(
      daoError ⇒ Left(unknownError("Service not available")),
      _ match {
        case Some(individualUser) if individualUser.status === conf.ActiveUserStatus ⇒
          accountDao.getAccount(accountId.toString).fold(
            daoError ⇒ Left(unknownError("Service not available")),
            _ match {
              case Some(existingAccount) if existingAccount.status !== Account.BLOCKED ⇒
                Left(validationError(s"Unable to activate account $accountId of customer $customerId because account status is already ${existingAccount.status}."))

              case Some(existingAccount) ⇒
                val accountToUpdate = AccountToUpdate(
                  status = Option(Account.ACTIVE),
                  updatedAt = doneAt,
                  updatedBy = doneBy)

                accountDao.updateAccount(accountId.toString, accountToUpdate).fold(
                  daoError ⇒ Left(unknownError("Service not available")),
                  account ⇒ {
                    account.fold[ServiceResponse[Account]](Left(notFoundError(s"Cannot update status of account ${accountId} to ACTIVE because it was not found."))) {
                      acc ⇒
                        userDao.getUUIDByInternalUserId(acc.userId).fold(
                          daoError ⇒ Left(unknownError(daoError.message)),
                          _ match {
                            case Some(customerUuid) ⇒
                              acc.asDomain.toEither
                                .leftMap { throwable ⇒
                                  logger.error(s"Error in activateIndividualUserAccount", throwable)
                                  dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
                                }
                            case None ⇒
                              Left(notFoundError(s"User with account internal userId ${acc.userId} not found"))
                          })

                    }
                  })

              case None ⇒
                Left(notFoundError(s"Unable to activate account $accountId of customer $customerId because account is missing."))
            })

        case Some(individualUser) ⇒
          Left(validationError(s"Unable to activate account $accountId of customer $customerId because this customer is not active."))

        case None ⇒
          Left(notFoundError(s"Unable to activate account $accountId because customer $customerId is missing."))
      })
  }

  def deactivateIndividualUserAccount(customerId: UUID, accountId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[Account]] = Future {
    individualUserDao.getIndividualUser(customerId.toString).fold(
      daoError ⇒ Left(unknownError("Service not available")),
      _ match {
        case Some(individualUser) if individualUser.status === conf.ActiveUserStatus ⇒
          accountDao.getAccount(accountId.toString).fold(
            daoError ⇒ Left(unknownError("Service not available")),
            _ match {
              case Some(existingAccount) if existingAccount.status !== Account.ACTIVE ⇒
                Left(validationError(s"Unable to deactivate account $accountId of customer $customerId because account status is ${existingAccount.status}."))

              case Some(existingAccount) ⇒
                val accountToUpdate = AccountToUpdate(
                  status = Option(Account.BLOCKED),
                  updatedAt = doneAt,
                  updatedBy = doneBy)

                accountDao.updateAccount(accountId.toString, accountToUpdate).fold(
                  daoError ⇒ Left(unknownError("Service not available")),
                  account ⇒ {
                    account.fold[ServiceResponse[Account]](Left(notFoundError(s"Cannot update status of account ${accountId} to DEACTIVATED because it was not found."))) {
                      acc ⇒
                        userDao.getUUIDByInternalUserId(acc.userId).fold(
                          daoError ⇒ Left(unknownError(daoError.message)),
                          _ match {
                            case Some(customerUuid) ⇒
                              acc.asDomain.toEither
                                .leftMap { throwable ⇒
                                  logger.error(s"Error in deactivateIndividualUserAccount", throwable)
                                  dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
                                }
                            case None ⇒
                              Left(notFoundError(s"User with account internal userId ${acc.userId} not found"))
                          })

                    }
                  })

              case None ⇒
                Left(notFoundError(s"Unable to deactivate account $accountId of customer $customerId because account is missing."))
            })

        case Some(individualUser) ⇒
          Left(validationError(s"Unable to deactivate account $accountId of customer $customerId because this customer is not active."))

        case None ⇒
          Left(notFoundError(s"Unable to deactivate account $accountId because customer $customerId is missing."))
      })
  }

  //TODO refactor method below
  def closeIndividualUserAccount(customerId: UUID, accountId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[Account]] = Future {
    individualUserDao.getIndividualUser(customerId.toString).fold(
      daoError ⇒ Left(unknownError("Service is not available")),
      _ match {
        case Some(individualUser) if individualUser.status === conf.ActiveUserStatus ⇒

          accountDao.getAccount(accountId.toString).fold(
            daoError ⇒ Left(unknownError("Service not available")),
            _ match {
              case Some(existingAccount) if existingAccount.status !== Account.ACTIVE ⇒
                Left(validationError(s"Unable to close account $accountId of customer $customerId because account is not ACTIVE."))

              case Some(existingAccount) if !existingAccount.balance.contains(BigDecimal("0")) ⇒
                Left(validationError(s"Unable to close account $accountId of customer $customerId because account still has remaining balance of ${existingAccount.currency} ${existingAccount.balance}"))

              case Some(existingAccount) ⇒
                val accountToUpdate = AccountToUpdate(
                  status = Option(Account.CLOSED),
                  updatedAt = doneAt,
                  updatedBy = doneBy)

                accountDao.updateAccount(accountId.toString, accountToUpdate).fold(
                  daoError ⇒ Left(unknownError("Service not available")),
                  account ⇒ {
                    account.fold[ServiceResponse[Account]](Left(notFoundError(s"Cannot update status of account ${accountId} to CLOSED because it was not found."))) {
                      acc ⇒

                        userDao.getUUIDByInternalUserId(acc.userId).fold(
                          daoError ⇒ Left(unknownError(daoError.message)),
                          _ match {
                            case Some(customerUuid) ⇒
                              acc.asDomain.toEither
                                .leftMap { throwable ⇒
                                  logger.error(s"Error in closeIndividualUserAccount", throwable)
                                  dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
                                }
                            case None ⇒
                              Left(notFoundError(s"User with account internal userId ${acc.userId} not found"))
                          })

                    }
                  })

              case None ⇒
                Left(notFoundError(s"Unable to close account $accountId of customer $customerId because account is missing."))
            })

        case Some(individualUser) ⇒
          Left(validationError(s"Unable to close account $accountId of customer $customerId because this customer is not active."))

        case None ⇒
          Left(notFoundError(s"Unable to close account $accountId because customer $customerId is missing."))
      })
  }

}

