package tech.pegb.backoffice.util

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  implicit lazy val logger: Logger = LoggerFactory.getLogger(getClass)
}
