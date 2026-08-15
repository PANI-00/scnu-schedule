package com.scnu.schedule.data.jwxt

/** 教务导入统一异常基类，便于上层按类分派错误态。 */
open class JwxtException(message: String, cause: Throwable? = null) : Exception(message, cause)

class JwxtLoginExpiredException(message: String) : JwxtException(message)

class JwxtNetworkException(message: String, cause: Throwable? = null) : JwxtException(message, cause)

class JwxtParseException(message: String, cause: Throwable? = null) : JwxtException(message, cause)
