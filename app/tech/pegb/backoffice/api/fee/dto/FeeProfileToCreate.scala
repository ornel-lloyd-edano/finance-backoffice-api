package tech.pegb.backoffice.api.fee.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class FeeProfileToCreate(
    @JsonProperty(required = true) feeType: String,
    @JsonProperty(required = true) userType: String,
    @JsonProperty(required = true) tier: String,
    @JsonProperty(required = true) subscriptionType: String,
    @JsonProperty(required = true) transactionType: String,
    @JsonProperty(required = true) channel: Option[String],
    @JsonProperty(required = true) otherParty: Option[String],
    @JsonProperty(required = true) instrument: Option[String],
    @JsonProperty(required = true) calculationMethod: String,
    @JsonProperty(required = true) currencyCode: String,
    @JsonProperty(required = true) feeMethod: String,
    @JsonProperty(required = true) taxIncluded: Option[Boolean],
    @JsonProperty(required = false) maxFee: Option[BigDecimal],
    @JsonProperty(required = false) minFee: Option[BigDecimal],
    @JsonProperty(required = false) feeAmount: Option[BigDecimal],
    @JsonProperty(required = false) feeRatio: Option[BigDecimal],
    @JsonProperty(required = false) ranges: Option[Seq[FeeProfileRangeToCreate]]) {

}

object FeeProfileToCreate {
  val empty = new FeeProfileToCreate(feeType = "", userType = "individual", tier = "basic", subscriptionType = "standard",
    transactionType = "p2p_domestic", channel = Some("mobile_money"), otherParty = None, instrument = None,
    calculationMethod = "", currencyCode = "AED", feeMethod = "add", taxIncluded = None,
    maxFee = None, minFee = None, feeAmount = None, feeRatio = None, ranges = None)
}
