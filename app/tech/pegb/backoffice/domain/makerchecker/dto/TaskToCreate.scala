package tech.pegb.backoffice.domain.makerchecker.dto

import tech.pegb.backoffice.domain.makerchecker.model.{MakerDetails, MakerRequest}

case class TaskToCreate(
    maker: MakerDetails,
    makerRequest: MakerRequest,
    module: String,
    action: String) {

}
