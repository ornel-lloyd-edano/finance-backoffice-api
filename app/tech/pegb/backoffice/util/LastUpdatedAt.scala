package tech.pegb.backoffice.util

import java.time.LocalDateTime

trait LastUpdatedAt {

  def lastUpdatedAt: Option[LocalDateTime]

}
