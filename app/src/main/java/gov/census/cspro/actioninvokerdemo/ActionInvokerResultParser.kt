package gov.census.cspro.actioninvokerdemo

import org.json.JSONArray
import org.json.JSONObject


class ActionInvokerResultParser(var throwExceptions: Boolean = true) {
     fun parse(result: JSONObject): Any? {
          assert(result.has("type"))

          val type = result.get("type")

          if( type == "undefined" ) {
               return null
          }

          val value = result.get("value")

          return when( type ) {
               "exception" -> {
                    assert(value is String)
                    val exception = Exception(value as String)
                    if( throwExceptions ) {
                         throw exception
                    }
                    exception
               }

               "boolean", "number", "string" -> {
                    assert(( type == "boolean" && value is Boolean ) ||
                           ( type == "number" && ( value is Int || value is Double || value is String ) ) ||
                           ( type == "string" && value is String ))
                    value
               }

               else -> {
                    assert(type == "json")
                    assert(value is JSONObject || value is JSONArray)
                    value
               }
          }
     }
}