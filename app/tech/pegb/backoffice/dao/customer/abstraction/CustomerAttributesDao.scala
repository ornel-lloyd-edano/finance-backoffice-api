package tech.pegb.backoffice.dao.customer.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.customer.entity.CustomerAttributes._
import tech.pegb.backoffice.dao.customer.entity._
import tech.pegb.backoffice.dao.customer.sql.CustomerAttributesSqlDao

@ImplementedBy(classOf[CustomerAttributesSqlDao])
trait CustomerAttributesDao extends Dao {

  def getUserAndBusinessUserJoinWithMainAccountJoin(id: String): DaoResponse[UserAndBusinessUserJoinWithAccountJoin]

  def getUserAndBusinessUserJoinWithAllAccountsJoin(id: String): DaoResponse[(UserAndBusinessUserJoinWithAccountJoin, Set[Account])]

  def getCustomerSegments: DaoResponse[Set[CustomerSegment]]

  def getCustomerSubscriptions: DaoResponse[Set[CustomerSubscription]]

  def getCustomerTiers: DaoResponse[Set[CustomerTier]]

  def getCustomerStatuses: DaoResponse[Set[CustomerStatus]]

  def getCustomerTypes: DaoResponse[Set[CustomerType]]

  def getBusinessUserTypes: DaoResponse[Set[BusinessUserType]]
}

