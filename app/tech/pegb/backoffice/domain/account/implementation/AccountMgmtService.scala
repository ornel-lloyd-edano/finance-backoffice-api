package tech.pegb.backoffice.domain.account.implementation

import java.time.{Clock, LocalDateTime}
import java.util.UUID

import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.account.dto.AccountToUpdate
import tech.pegb.backoffice.dao.customer.abstraction.{IndividualUserDao, UserDao}
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionDao
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.account.dto.{AccountCriteria, AccountToCreate}
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountNumber, AccountStatus}
import tech.pegb.backoffice.domain.account.model.{Account, FloatAccountAggregation}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

class AccountMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    individualUserDao: IndividualUserDao,
    accountDao: AccountDao,
    txnDao: TransactionDao,
    systemSettingsDao: SystemSettingsDao) extends AccountManagement with BaseService {

  implicit val executionContext = executionContexts.blockingIoOperations

  val clock: Clock = Clock.systemDefaultZone()

  //account creation will only work correctly for Individual User because account name appends msisdn with account type but business user may not have msisdn
  def createAccount(accountToCreate: AccountToCreate, expectedUserType: Option[String] = None): Future[ServiceResponse[Account]] = {
    Future {
      val userId = accountToCreate.customerId
      userDao.getUser(userId.toString).fold(
        _.asDomainError.toLeft,
        userOption ⇒ {
          userOption match {
            case Some(user) ⇒
              if (user.status === conf.PassiveUserStatus) {
                Left(validationError(s"Cannot create account for user ${userId}: User is deactivated"))
              } else if (expectedUserType.isDefined && expectedUserType != user.`type`) {
                Left(validationError(s"Error on UserType validation user of ${userId}. Found usertype = ${user.`type`.getOrElse("")}, Expected userType = ${expectedUserType.getOrElse("")}."))
              } else if (user.`type`.contains(conf.IndividualUserType) && accountToCreate.accountType.underlying != conf.WalletAccountType) {
                Left(validationError(s"Cannot create account for individual user ${userId}: IndividualUser can only create ${conf.WalletAccountType}"))
              } else {
                accountDao.getAccountsByUserId(userId.toString).fold(
                  _.asDomainError.toLeft,
                  userAccounts ⇒ {
                    if (accountToCreate.isMainAccount && userAccounts.exists(_.isMainAccount.contains(true)))
                      Left(validationError(s"Cannot create main account for user ${userId}: User already has a main account"))
                    else if (user.`type`.contains(conf.IndividualUserType) && userAccounts.map(_.currency).contains(accountToCreate.currency.getCurrencyCode)) {
                      Left(validationError(s"Cannot create account for individual user ${userId}: User already has an account with currency ${accountToCreate.currency}"))
                    } else {

                      val maybeAccountNumber = accountDao.countTotalAccountsByCriteria(
                        AccountCriteria(customerId = Option(UUIDLike(userId.toString))).asDao).fold(
                          _.asDomainError.toLeft,
                          count ⇒ {
                            val index = count + 1
                            Right(s"${user.id}.${index}")
                          })
                      val maybeAccountName = expectedUserType match {
                        case Some(conf.IndividualUserType) ⇒
                          individualUserDao.getIndividualUser(accountToCreate.customerId.toString).fold(
                            _.asDomainError.toLeft,
                            {
                              case Some(individualUser) ⇒
                                Right(s"${individualUser.msisdn}_${accountToCreate.accountType.underlying}")
                              case _ ⇒
                                Left(notFoundError(s"Failed to create account. Individual user id [$userId] is missing"))
                            })
                        case _ ⇒
                          Left(validationError(s"Failed to create account. This user type [${expectedUserType.getOrElse("UNKNOWN")}] is not yet supported."))
                      }

                      for {
                        accountName ← maybeAccountName
                        accountNumber ← maybeAccountNumber
                        completedAccountToCreate ← accountToCreate.copy(
                          accountNumber = Option(AccountNumber(accountNumber)),
                          accountName = Option(NameAttribute(accountName)),
                          accountStatus = Option(AccountStatus(conf.NewlyCreatedAccountStatus))).asDao.fold(
                            error ⇒ Left(dtoMappingError(s"Unable to form dao AccountToCreate correctly")),
                            dto ⇒ Right(dto))
                        insertedAccount ← accountDao.insertAccount(completedAccountToCreate).fold(
                          _.asDomainError.toLeft,
                          insertedAccount ⇒ {
                            insertedAccount.asDomain.fold(
                              error ⇒ Left(dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${error.getMessage.replaceAll("assertion failed: ", "")}")),
                              correctDomainDto ⇒ Right(correctDomainDto))
                          })
                      } yield (insertedAccount)

                    }
                  })
              }
            case None ⇒
              Left(notFoundError(s"Cannot create account for user ${userId}: User not found"))
          }
        })
    }
  }

  def getAccountByAccountNumber(accountNumber: AccountNumber): Future[ServiceResponse[Account]] = {
    Future {
      accountDao.getAccountByAccNum(accountNumber.underlying).fold(
        _.asDomainError.toLeft,
        account ⇒ {
          account.fold[ServiceResponse[Account]](Left(notFoundError(s"Account with accountNumber ${accountNumber.underlying} not found."))) {
            acc ⇒
              userDao.getUUIDByInternalUserId(acc.userId).fold(
                daoError ⇒ Left(unknownError(daoError.message)),
                _ match {
                  case Some(customerUuid) ⇒
                    acc.asDomain.toEither
                      .leftMap { throwable ⇒
                        logger.error(s"Error in getAccountByAccountNumber", throwable)
                        dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
                      }
                  case None ⇒
                    Left(notFoundError(s"User with account internal userId ${acc.userId} not found"))
                })
          }
        })
    }
  }

  def getAccountById(id: UUID): Future[ServiceResponse[Account]] = {
    Future {
      accountDao.getAccount(id.toString).fold(
        _.asDomainError.toLeft,
        account ⇒ {
          account.fold[ServiceResponse[Account]](Left(notFoundError(s"Account with id ${id} not found."))) {
            _.asDomain.toEither
              .leftMap { throwable ⇒
                logger.error(s"Error in getAccountById", throwable)
                dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
              }
          }
        })
    }
  }

  def countAccountsByCriteria(criteria: AccountCriteria): Future[ServiceResponse[Int]] = Future {
    accountDao.countTotalAccountsByCriteria(criteria.asDao).leftMap(_.asDomainError)
  }

  def getAccountsByCriteria(
    criteria: AccountCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Account]]] = Future {
    for {
      daoAccounts ← accountDao.getAccountsByCriteria(Some(criteria.asDao), orderBy.asDao, limit, offset).asServiceResponse
      accounts ← daoAccounts.map(_.asDomain.toEither.leftMap { exc ⇒
        val cause = exc.getMessage.replaceAll("assertion failed: ", "")
        dtoMappingError(s"Failed to convert account entity to domain. Cause by: $cause")
      }).toList.sequence[ServiceResponse, Account]
    } yield accounts
  }

  def getAccountByAccountName(accountName: NameAttribute): Future[ServiceResponse[Account]] = {
    Future {
      accountDao.getAccountByAccountName(accountName.underlying).fold(
        _.asDomainError.toLeft,
        account ⇒ {
          account.fold[ServiceResponse[Account]](Left(notFoundError(s"Account with accountName ${accountName.underlying} not found."))) {
            _.asDomain.toEither
              .leftMap { throwable ⇒
                logger.error(s"Error in getAccountByAccountName", throwable)

                dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
              }
          }
        })
    }
  }

  def deleteAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Account]] = {
    accountDao.getAccount(id.toString).fold(
      daoError ⇒ Future.successful(Left(unknownError("Could not connect to database"))),
      _ match {
        case Some(existingAccount) ⇒
          if (existingAccount.balance.contains(BigDecimal("0"))) {
            updateAccountStatus(id.toString, Account.CLOSED, doneBy, doneAt, lastUpdatedAt)
          } else {
            Future.successful(Left(notFoundError(s"Unable to close account [${id.toString}] because it has remaining balance ${existingAccount.currency} ${existingAccount.balance}.")))
          }
        case None ⇒
          Future.successful(Left(notFoundError(s"Unable to close account [${id.toString}] because it cannot be found.")))
      })
  }

  def blockAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Account]] = {
    updateAccountStatus(id.toString, Account.BLOCKED, doneBy, doneAt, lastUpdatedAt)
  }

  def freezeAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Account]] = {
    updateAccountStatus(id.toString, Account.FROZEN, doneBy, doneAt, lastUpdatedAt)
  }

  def activateAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Account]] = {
    updateAccountStatus(id.toString, Account.ACTIVE, doneBy, doneAt, lastUpdatedAt)
  }

  def getBalance(id: UUID): Future[ServiceResponse[BigDecimal]] = ???

  def withdrawAmount(id: UUID, amount: BigDecimal): Future[ServiceResponse[Account]] = ???

  def depositAmount(id: UUID, amount: BigDecimal): Future[ServiceResponse[Account]] = ???

  def transferAmount(sourceAccountId: UUID, destinationAccountId: UUID, amount: BigDecimal): Future[ServiceResponse[Unit]] = ???

  def executeOnFlyAggregation(
    txnCriteria: TransactionCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[FloatAccountAggregation]]] = {
    Future {
      for {
        daoAccounts ← accountDao.getAccountsByCriteria(
          AccountCriteria(
            accountNumbers = txnCriteria.accountNumbers.toSet.toOption).asDao.toOption, orderBy.asDao, limit, offset).asServiceResponse

        domainAccounts ← daoAccounts.map(_.asDomain.toEither.leftMap { exc ⇒
          val cause = exc.getMessage.replaceAll("assertion failed: ", "")

          dtoMappingError(s"Failed to convert account entity to domain. Cause by: $cause")
        }).toList.sequence[ServiceResponse, Account]

        txnsOnFlyAggregations ← domainAccounts.map { account ⇒
          txnDao.getOnFlyAggregation(txnCriteria.copy(accountId = Some(account.id.toUUIDLike), accountNumbers = Seq.empty).asDao(), account.isLiability)
            .asServiceResponse
            .map { optionalTxnFlow ⇒

              val (inflow, outflow, net) = optionalTxnFlow.fold((BigDecimal(0), BigDecimal(0), BigDecimal(0)))(identity)
              FloatAccountAggregation(
                userUuid = account.customerId,
                userName = account.customerName.getOrElse("UNKNOWN"),
                accountUuid = account.id,
                accountNumber = account.accountNumber,
                accountType = account.accountType,
                accountMainType = account.mainType,
                currency = account.currency,
                internalBalance = account.balance,
                externalBalance = None,
                inflow = inflow,
                outflow = outflow,
                net = net,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt)
            }
        }.sequence[ServiceResponse, FloatAccountAggregation]

      } yield txnsOnFlyAggregations
    }
  }

  private def updateAccountStatus(id: String, status: String, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Account]] = {
    Future {
      val accountToUpdate = AccountToUpdate(
        status = Option(status),
        updatedAt = doneAt,
        updatedBy = doneBy)

      accountDao.updateAccount(id, accountToUpdate).fold(
        _.asDomainError.toLeft,
        account ⇒ {
          account.fold[ServiceResponse[Account]](Left(notFoundError(s"Cannot update status of account ${id}. Account not found."))) {
            acc ⇒
              acc.asDomain.toEither
                .leftMap { throwable ⇒
                  logger.error(s"Error in updateAccountStatus", throwable)
                  dtoMappingError(s"Failed to convert account entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
                }
          }
        })
    }
  }

}

