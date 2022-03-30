package tech.pegb.backoffice.api.auth.model

case class ProxyRequest[T](
    httpMethod: String,
    url: String,
    body: Option[T],
    queryParameters: Seq[(String, String)] = Seq.empty,
    headers: Set[(String, String)] = Set.empty)
