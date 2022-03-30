package tech.pegb.backoffice.dao.fee.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.fee.dto._
import tech.pegb.backoffice.dao.fee.entity._
import tech.pegb.backoffice.dao.fee.sql.ThirdPartyFeeProfileSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[ThirdPartyFeeProfileSqlDao])
trait ThirdPartyFeeProfileDao extends Dao {

  def createThirdPartyFeeProfile(thirdPartyFeeProfileToInsert: ThirdPartyFeeProfileToInsert): DaoResponse[ThirdPartyFeeProfile]

  def createThirdPartyFeeProfileRange(
    thirdPartyFeeProfileId: String,
    thirdPartyFeeProfileRangeToInsert: Seq[ThirdPartyFeeProfileRangeToInsert])(implicit connectionOption: Option[Connection] = None): DaoResponse[Seq[ThirdPartyFeeProfileRange]]

  def getThirdPartyFeeProfile(id: String): DaoResponse[Option[ThirdPartyFeeProfile]]

  def getThirdPartyFeeProfileByCriteria(criteria: ThirdPartyFeeProfileCriteria, ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[ThirdPartyFeeProfile]]

  def getThirdPartyFeeProfileRangesByFeeProfileId(thirdPartyFeeProfileId: String): DaoResponse[Seq[ThirdPartyFeeProfileRange]]
}
