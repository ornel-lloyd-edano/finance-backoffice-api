package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

/*
 Workaround for swagger documentation,
 examples field doesn't work for ApiResponses

 @ApiOperation(value = "Returns i18n dictionary for certain platform and locale")
  @ApiResponses(
    Array(
      new ApiResponse(
        code = 200,
        message = "example",
        examples = new Example(
          value = Array(new ExampleProperty(value = """{"key1":"value in text 1","key2","value in text 2"}""", mediaType = "application/json"))))))

 */
case class I18nDictionaryResponse(
    @ApiModelProperty(name = "welcome_key", example = "welcome", required = true) welcomKey: String,
    @ApiModelProperty(name = "logout_button_key", example = "logout") logoutButtonKey: String,
    @ApiModelProperty(name = "hello_key", example = "hola") helloKey: String)
