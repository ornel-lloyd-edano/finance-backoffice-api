package tech.pegb.backoffice.dao.makerchecker.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.makerchecker.dto.{MakerCheckerCriteria, TaskToInsert, TaskToUpdate}
import tech.pegb.backoffice.dao.makerchecker.entity.MakerCheckerTask
import tech.pegb.backoffice.dao.makerchecker.sql.TasksSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[TasksSqlDao])
trait TasksDao extends Dao {

  def selectTasksByCriteria(
    criteria: MakerCheckerCriteria,
    ordering: Option[OrderingSet] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None): DaoResponse[Seq[MakerCheckerTask]]

  def selectTaskByUUID(uuid: String): DaoResponse[Option[MakerCheckerTask]]

  def countTasks(criteria: MakerCheckerCriteria): DaoResponse[Int]

  def insertTask(dto: TaskToInsert): DaoResponse[MakerCheckerTask]

  def updateTask(uuid: String, dto: TaskToUpdate): DaoResponse[MakerCheckerTask]

}
