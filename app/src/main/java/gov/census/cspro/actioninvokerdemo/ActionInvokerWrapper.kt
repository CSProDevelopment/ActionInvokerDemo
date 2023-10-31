package gov.census.cspro.actioninvokerdemo

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract


class ActionInvokerWrapper(var accessToken: String? = null) : ActivityResultContract<String, String>() {
     var title: String = "Action Invoker execution from the Action Invoker Demo"
     var abortOnException: Boolean? = null

     override fun createIntent(context: Context, input: String): Intent {
          val intent = Intent()
          intent.component = ComponentName("gov.census.cspro.csentry", "gov.census.cspro.ActionInvokerActivity")

          // one or more actions are specified by defining the actions in JSON format
          intent.putExtra("ACTION", input)

          // the optional title is displayed by CSEntry when running the actions
          intent.putExtra("TITLE", title)

          // additional optional arguments; by default, CSEntry aborts on the first exception
          intent.putExtra("ACCESS_TOKEN", accessToken)
          intent.putExtra("ABORT_ON_EXCEPTION", abortOnException)

          return intent
     }

     override fun parseResult(resultCode: Int, intent: Intent?): String {
          if( resultCode == Activity.RESULT_OK ) {
               intent?.getStringExtra("RESULT")?.let {
                    return it
               }
          }

          return "{\"type\":\"exception\",\"value\":\"Unexpected result from the Action Invoker.\"}"
     }
}
