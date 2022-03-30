package tech.pegb.backoffice.domain.makerchecker.implementation

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.i18n.abstraction.I18nStringDao
import tech.pegb.backoffice.dao.makerchecker.abstraction.{GetBackofficeUsersContactsDao, TasksDao}
import tech.pegb.backoffice.domain._
import tech.pegb.backoffice.domain.i18n.dto.I18nStringCriteria
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform}
import tech.pegb.backoffice.domain.makerchecker.abstraction.{MakerCheckerService, RequestCreator}
import tech.pegb.backoffice.domain.makerchecker.dto.{MakerCheckerCriteria, TaskToApprove, TaskToCreate, TaskToReject}
import tech.pegb.backoffice.domain.makerchecker.model.{HttpVerbs, MakerCheckerTask, RoleLevel}
import tech.pegb.backoffice.mapping.domain.dao.i18n.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.makerchecker.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.makerchecker.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future
import scala.util.Try

@Singleton
class MakerCheckerServiceImpl @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    tasksDao: TasksDao,
    backofficeUsersDao: GetBackofficeUsersContactsDao,
    i18nStringDao: I18nStringDao,
    requestCreator: RequestCreator,
    httpClient: HttpClient,
    emailClient: EmailClient) extends MakerCheckerService with BaseService {

  import MakerCheckerServiceImpl._

  implicit val ec = executionContexts.genericOperations

  def getTasksByCriteria(
    criteria: MakerCheckerCriteria,
    requesterLevel: RoleLevel,
    requesterBusinessUnit: String,
    orderBy: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int])(implicit requestId: UUID): Future[ServiceResponse[Seq[MakerCheckerTask]]] = Future {

    val daoCriteria = getValidDaoCriteria(criteria, requesterLevel, requesterBusinessUnit)
    for {
      selectRes ← tasksDao.selectTasksByCriteria(
        daoCriteria, orderBy.asDao, limit, offset).fold(
        _.asDomainError.toLeft,
        tasks ⇒ tasks.toList.map(_.asDomain()).sequence[Try, MakerCheckerTask]
          .toEither
          .leftMap({ t ⇒
            logger.error("Error in getTasksByCriteria", t)
            dtoMappingError("MakerCheckerTask")
          }))
    } yield {
      selectRes
    }
  }

  def getTaskById(uuid: UUID)(implicit requestId: UUID): Future[ServiceResponse[MakerCheckerTask]] = {
    (for {
      taskOption ← EitherT.fromEither[Future](tasksDao.selectTaskByUUID(uuid.toString).asServiceResponse)
      task ← EitherT.fromOption[Future](
        taskOption,
        ServiceError.notFoundError(s"Task id [$uuid] not found", requestId.toOption))
      currentValue ← EitherT({
        val getResponse: Future[ServiceResponse[Option[String]]] = if (task.verb === HttpVerbs.PUT.underlying) {

          //TODO: Move this to backoffice-auth and store this to database
          val getUrl = conf.RemoveFromRequest.foldLeft(task.url.replace(
            conf.Hosts.MainBackofficeApiPlaceholder, conf.Hosts.MainBackofficeApi)) {
            case (acc, str) ⇒ acc.replace(str, "")
          }
          httpClient.request(HttpVerbs.GET.underlying, getUrl, None)
            .map(resp ⇒ if (resp.success) resp.body.asRight[ServiceError] else {
              logger.warn(s"GET url for task current value was either not found or returned error")
              Right(None)
            })
        } else
          Future.successful(none[String].asRight[ServiceError])

        getResponse
      })
      taskDomain ← EitherT.fromEither[Future](task.asDomain(currentValue).toEither.leftMap({ t ⇒
        logger.error("Error in getTasksByCriteria", t)
        dtoMappingError("MakerCheckerTask")
      }))
    } yield {
      taskDomain
    }).value
  }

  def countTasksByCriteria(
    criteria: MakerCheckerCriteria,
    requesterLevel: RoleLevel,
    requesterBusinessUnit: String): Future[ServiceResponse[Int]] = Future {

    val daoCriteria = getValidDaoCriteria(criteria, requesterLevel, requesterBusinessUnit)
    tasksDao.countTasks(daoCriteria).leftMap(_.asDomainError)
  }

  def createPendingTask(dto: TaskToCreate, requestId: UUID): Future[ServiceResponse[MakerCheckerTask]] = {
    //validations:
    //-highest role level or CEO equivalent (level 0 by convention) can create task as of PWBOAPI-770
    //-role level 0 can only approve or reject
    //-he/she must delegate task creation to a lower level (ex. department head role level)

    (for {

      validUrl ← EitherT.fromEither[Future](dto.makerRequest.validateURL(
        actualHost = conf.Hosts.MainBackofficeApi,
        placeHolder = conf.Hosts.MainBackofficeApiPlaceholder))
        .leftMap(error ⇒ {
          logger.warn("Error in createPendingTask", error)
          unknownError(s"Request URL in the task is invalid.")
        })

      currentValue ← EitherT(
        if (dto.makerRequest.verb == HttpVerbs.PUT || dto.makerRequest.verb == HttpVerbs.DELETE ||
          dto.module == "currency_exchanges") {

          //TODO: Move this to backoffice-auth and store this to database
          val tempUrl = conf.RemoveFromRequest.foldLeft(validUrl) {
            case (acc, str) ⇒ acc.replace(str, "")
          }

          val getUrl = if (dto.module == "currency_exchanges" &&
            dto.makerRequest.verb == HttpVerbs.POST &&
            tempUrl.endsWith("/spreads"))
            tempUrl.replace("/spreads", "")
          else
            tempUrl

          httpClient.request(HttpVerbs.GET.underlying, getUrl, Map.empty, Map.empty, None, requestId)
            .map(resp ⇒
              resp.success.toEither(
                ServiceError.externalServiceError(resp.body.getOrElse(s"Unexpected error from $getUrl")),
                resp.body)).recover {
              case error ⇒
                ServiceError.externalServiceError(s"Unexpected error from $getUrl").toLeft
            }

        } else {
          none[String].asRight[ServiceError].toFuture
        })

      insertedTask ← EitherT.fromEither[Future](tasksDao.insertTask(dto.asDao(requestId, MakerCheckerTask.statusOnCreate, currentValue)))
        .leftMap(_.asDomainError)

      domainDto ← EitherT.fromEither[Future](insertedTask.asDomain().toEither.fold(mappingError(_), Right(_)))

    } yield {
      if (conf.Mailer.Enabled) notifyCheckers(domainDto)
      domainDto
    }).value
  }

  def validateCheckerProcess(taskId: String, checkerUsername: String, checkerLevel: RoleLevel, checkerBusinessUnit: String, process: String): ServiceResponse[MakerCheckerTask] = {
    //validations:
    //approver/rejecter can only approve/reject if level is higher (numeric value lower) and within the same business unit
    //ceo can approve/reject anything regardless of business unit
    //department head/approver (next to the highest, aka. level 1) can approve/reject other user's of level 1 as long as same department

    for {
      maybeTask ← tasksDao.selectTaskByUUID(taskId).leftMap(_.asDomainError)
      taskAllowedToCheck ← maybeTask.map(_.asDomain().toEither) match {
        case None ⇒ notFoundTaskId(taskId)

        case Some(Left(error)) ⇒ mappingError(error)

        case Some(Right(task)) if task.status.isChecked ⇒ validationErrors(process, taskId, NotPending)

        case Some(Right(task)) if task.maker.createdBy == checkerUsername ⇒ validationErrors(process, taskId, OwnTaskChecker) //maker and checker cannot be the same

        case Some(Right(task)) if (!checkerLevel.isHighestLevel) && (checkerBusinessUnit !== task.maker.businessUnit) ⇒ validationErrors(process, taskId, DiffBusinessUnit) //only CA can approve tasks regardless of businessUnit

        case Some(Right(task)) if (!checkerLevel.isHighestLevel) //CA level is not checked
          && (checkerLevel.isDepartmentApproverLevel && !checkerLevel.isSameOrHigherThan(task.maker.level)) //IF DA, checkerLevel should be >= makerLevel
          && !checkerLevel.isHigherThan(task.maker.level) ⇒
          validationErrors(process, taskId, LowerLevel) //checkerLevel should be HIGHER if checker not CA or DA

        case Some(Right(task)) ⇒
          Right(task)
      }

    } yield {
      taskAllowedToCheck
    }
  }

  def approvePendingTask(dto: TaskToApprove): Future[ServiceResponse[MakerCheckerTask]] = {

    (for {
      taskAllowedToApprove ← EitherT(Future.successful(
        validateCheckerProcess(dto.id.toString, dto.approvedBy, dto.checkerLevel, dto.checkerBusinessUnit, "approve")))

      _ ← EitherT(requestCreator.createRequest(
        taskAllowedToApprove,
        conf.Hosts.MainBackofficeApiPlaceholder,
        conf.Hosts.MainBackofficeApi))

      approvedTask ← EitherT(Future.successful(tasksDao.updateTask(dto.id.toString, dto.asDao)
        .bimap(_.asDomainError, _.asDomain())))

      result ← EitherT(Future.successful(approvedTask.toEither.fold(mappingError(_), Right(_))))

    } yield {
      result
    }).value

  }

  def rejectPendingTask(dto: TaskToReject): Future[ServiceResponse[MakerCheckerTask]] = {

    (for {
      _ ← EitherT(Future.successful(validateCheckerProcess(
        dto.id.toString, dto.rejectedBy, dto.checkerLevel,
        dto.checkerBusinessUnit, "reject")))
      rejectedTask ← EitherT(Future.successful(tasksDao.updateTask(dto.id.toString, dto.asDao)
        .bimap(_.asDomainError, _.asDomain())))

      result ← EitherT(Future.successful(rejectedTask.toEither.fold(mappingError(_), Right(_))))

    } yield {
      result
    }).value

  }

  def notifyCheckers(dto: MakerCheckerTask): Future[ServiceResponse[Unit]] = Future {

    val actionPlaceHolder = "{{ action }}"
    val modulePlaceHolder = "{{ module }}"

    for {
      validUrl ← dto.makerRequest.validateURL(
        actualHost = conf.Hosts.MainBackofficeApi,
        placeHolder = conf.Hosts.MainBackofficeApiPlaceholder)
        .leftMap(error ⇒ {
          logger.warn("Error in notifyCheckers", error)
          unknownError(s"Aborting to send notification email to checker. Request URL in the task is invalid.")
        })

      contacts ← backofficeUsersDao
        .getBackofficeUsersContactsByRoleLvlAndBusinessUnit(dto.maker.level.underlying, dto.maker.businessUnit)
        .leftMap(_.asDomainError)

      i18nSubjectTemplate ← i18nStringDao.getStringByCriteria(
        criteria = I18nStringCriteria(
          key = I18nKey(conf.MakerChecker.Notification.i18nSubjectKey).some,
          locale = I18nLocale(conf.DefaultLocale).some,
          platform = I18nPlatform(conf.DefaultPlatform).some).asDao,
        ordering = None,
        limit = None,
        offset = None).leftMap(_.asDomainError)

      i18nBodyTemplate ← i18nStringDao.getStringByCriteria(
        criteria = I18nStringCriteria(
          key = I18nKey(conf.MakerChecker.Notification.i18nBodyKey).some,
          locale = I18nLocale(conf.DefaultLocale).some,
          platform = I18nPlatform(conf.DefaultPlatform).some).asDao,
        ordering = None,
        limit = None,
        offset = None).leftMap(_.asDomainError)

      body ← {
        val bodyTemplate = i18nBodyTemplate.headOption.fold(conf.MakerChecker.Notification.defaultBody)(_.text)
        Either.cond(
          bodyTemplate.contains(actionPlaceHolder) && bodyTemplate.contains(modulePlaceHolder),
          bodyTemplate,
          {
            logger.warn(s"notification body template should contain the following place holders: $actionPlaceHolder and $modulePlaceHolder")
            validationError(s"notification body template should contain the following place holders: $actionPlaceHolder and $modulePlaceHolder")
          })
      }

      result ← {
        val recipients = contacts.map(_.email)
        val subject = i18nSubjectTemplate.headOption.fold(conf.MakerChecker.Notification.defaultSubject)(_.text)
        emailClient.sendEmail(
          recipient = recipients,
          subject = subject,
          content = body.replace(actionPlaceHolder, dto.actionRequired).replace(modulePlaceHolder, dto.module))
          .leftMap(error ⇒ {
            logger.warn(s"Failed while sending email notification for task id ${dto.id}", error)
            unknownError(s"Failed while sending email notification for task id ${dto.id}")
          })
      }
    } yield {
      if (contacts.isEmpty) {
        logger.warn("No available backoffice user/s was found to check the task. No notification was sent.")
      } else {
        logger.info(s"Sent email notification for task id ${dto.id}")
      }
      result
    }
  }

  def notifyMaker: Future[ServiceResponse[Unit]] = {
    ???
  }

}

object MakerCheckerServiceImpl extends BaseService {

  sealed trait CausedBy

  case object NotPending extends CausedBy

  case object LowerLevel extends CausedBy

  case object DiffBusinessUnit extends CausedBy

  case object OwnTaskChecker extends CausedBy

  def notFoundTaskId(taskId: String) = {

    Left(notFoundError(s"Task with id $taskId not found"))
  }

  def mappingError(error: Throwable) = {

    logger.error("error while mapping dao maker checker task to domain ", error)

    Left(dtoMappingError("MakerCheckerTask"))

  }

  def validationErrors(process: String, taskId: String, causedBy: CausedBy) = {

    causedBy match {
      case NotPending ⇒ Left(validationError(s"Cannot $process task with id $taskId because it is not pending"))
      case LowerLevel ⇒ Left(validationError(s"Unable to $process task with id $taskId because checker level is lower than the maker of this task"))
      case DiffBusinessUnit ⇒ Left(validationError(s"Unable to $process task with id $taskId because checker and maker of this task is not from the same department"))
      case OwnTaskChecker ⇒ Left(validationError(s"Cannot $process task with id $taskId because maker and checker are the same user"))
    }
  }

  def getValidDaoCriteria(
    criteria: MakerCheckerCriteria,
    requesterLevel: RoleLevel,
    requesterBusinessUnit: String) = {

    criteria.asDao(requesterLevel, if (requesterLevel.isHighestLevel) None else requesterBusinessUnit.toOption)
  }

}
