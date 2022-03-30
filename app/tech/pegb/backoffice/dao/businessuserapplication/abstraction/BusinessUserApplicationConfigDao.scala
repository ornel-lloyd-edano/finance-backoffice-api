package tech.pegb.backoffice.dao.businessuserapplication.abstraction

import java.sql.Connection
import java.time.LocalDateTime

import cats.data.NonEmptyList
import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.businessuserapplication.dto.{AccountConfigToInsert, ExternalAccountToInsert, TransactionConfigToInsert}
import tech.pegb.backoffice.dao.businessuserapplication.entity.{AccountConfig, ExternalAccount, TransactionConfig}
import tech.pegb.backoffice.dao.businessuserapplication.sql.BusinessUserApplicationConfigSqlDao

@ImplementedBy(classOf[BusinessUserApplicationConfigSqlDao])
trait BusinessUserApplicationConfigDao extends Dao {

  def insertTxnConfig(
    applicationId: Int,
    txnConfigToInsert: NonEmptyList[TransactionConfigToInsert],
    createdBy: String,
    createdAt: LocalDateTime)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Seq[TransactionConfig]]

  def insertAccountConfig(
    applicationId: Int,
    accountConfigToInsert: NonEmptyList[AccountConfigToInsert],
    createdBy: String,
    createdAt: LocalDateTime)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Seq[AccountConfig]]

  def insertExternalAccount(
    applicationId: Int,
    externalAccountToInsert: NonEmptyList[ExternalAccountToInsert],
    createdBy: String,
    createdAt: LocalDateTime)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Seq[ExternalAccount]]

  def deleteTxnConfig(applicationId: Int)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Unit]

  def deleteAccountConfig(applicationId: Int)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Unit]

  def deleteExternalAccount(applicationId: Int)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Unit]

  def getTxnConfig(applicationId: Int): DaoResponse[Seq[TransactionConfig]]

  def getAccountConfig(applicationId: Int): DaoResponse[Seq[AccountConfig]]

  def getExternalAccount(applicationId: Int): DaoResponse[Seq[ExternalAccount]]
}
