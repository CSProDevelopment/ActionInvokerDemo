package gov.census.cspro.actioninvokerdemo

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract


class ActionInvokerWrapper(var accessToken: String? = null) : ActivityResultContract<String, String>() {
     private var refreshToken: String? = null
     var title: String = "Action Invoker execution from the Action Invoker Demo"
     var abortOnException: Boolean? = null

     override fun createIntent(context: Context, input: String): Intent {
          val intent = Intent()
          intent.component = ComponentName("gov.census.cspro.csentry", "gov.census.cspro.ActionInvokerActivity")

          // one or more actions are specified by defining the actions in JSON format
          intent.putExtra(ACTION, input)

          // the optional title is displayed by CSEntry when running the actions
          intent.putExtra(TITLE, title)

          // additional optional arguments; by default, CSEntry aborts on the first exception
          intent.putExtra(ACCESS_TOKEN, accessToken)
          intent.putExtra(ABORT_ON_EXCEPTION, abortOnException)

          // add a refresh token if we have one
          intent.putExtra(REFRESH_TOKEN, refreshToken)

          return intent
     }

     override fun parseResult(resultCode: Int, intent: Intent?): String {
          if( resultCode == Activity.RESULT_OK && intent != null ) {
               // if the user manually approved access to the Action Invoker, a refresh token
               // is provided that can be used for future calls
               refreshToken = intent.getStringExtra(REFRESH_TOKEN)

               intent.getStringExtra(RESULT)?.let {
                    return it
               }
          }

          return "{\"type\":\"exception\",\"value\":\"Unexpected result from the Action Invoker.\"}"
     }

     private companion object {
          const val ABORT_ON_EXCEPTION = "ABORT_ON_EXCEPTION"
          const val ACCESS_TOKEN = "ACCESS_TOKEN"
          const val ACTION = "ACTION"
          const val REFRESH_TOKEN = "REFRESH_TOKEN"
          const val RESULT = "ABORT_ON_EXCEPTION"
          const val TITLE = "TITLE"
     }
}
