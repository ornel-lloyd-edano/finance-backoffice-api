package tech.pegb.backoffice.dao.i18n.abstraction

import java.sql.Connection
import java.time.LocalDateTime

import cats.data.NonEmptyList
import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.i18n.dto.{I18nStringCriteria, I18nStringToInsert, I18nStringToUpdate}
import tech.pegb.backoffice.dao.i18n.entity.{I18nPair, I18nString}
import tech.pegb.backoffice.dao.i18n.sql.I18nStringSqlDao
import tech.pegb.backoffice.dao.model._

@ImplementedBy(classOf[I18nStringSqlDao])
trait I18nStringDao extends Dao {

  def insertString(dto: I18nStringToInsert): DaoResponse[I18nString]

  def bulkInsertString(rows: NonEmptyList[I18nStringToInsert])(implicit maybeConnection: Option[Connection] = None): DaoResponse[Int]

  def getStringById(id: Int): DaoResponse[Option[I18nString]]

  def getStringByCriteria(
    criteria: I18nStringCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit maybeConnection: Option[Connection] = None): DaoResponse[Seq[I18nString]]

  def getI18nPairsByCriteria(criteria: I18nStringCriteria): DaoResponse[Seq[I18nPair]]

  def countStringByCriteria(criteria: I18nStringCriteria): DaoResponse[Int]

  def updateString(
    id: Int,
    dto: I18nStringToUpdate,
    disableOptimisticLock: Boolean = false)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Option[I18nString]]

  def deleteString(id: Int, updatedAt: Option[LocalDateTime]): DaoResponse[Option[Int]]
}
