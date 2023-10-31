package gov.census.cspro.actioninvokerdemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract


class ActionInvokerWrapperForSharingFiles(private val baseActionInvokerWrapper: ActionInvokerWrapper) : ActivityResultContract<Pair<String, ArrayList<Uri>>, String>() {
     override fun createIntent(context: Context, input: Pair<String, ArrayList<Uri>>): Intent {
          // use the base wrapper to create the intent
          val intent = baseActionInvokerWrapper.createIntent(context, input.first)

          // adding the URIs to the intent will give other applications the permission to access the data
          intent.action = Intent.ACTION_SEND_MULTIPLE
          intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, input.second)
          intent.type = "*/*"

          return intent
     }

     override fun parseResult(resultCode: Int, intent: Intent?): String {
          // use the base wrapper to parse the result
          return baseActionInvokerWrapper.parseResult(resultCode, intent)
     }
}
