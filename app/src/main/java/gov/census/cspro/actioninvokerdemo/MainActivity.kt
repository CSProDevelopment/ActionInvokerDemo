package gov.census.cspro.actioninvokerdemo

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.StringBuilder
import java.util.Random


@Suppress("UNUSED_PARAMETER")
class MainActivity : ComponentActivity() {
    private val actionInvokerWrapper = ActionInvokerWrapper()
    private val actionInvokerLauncherDefault = registerForActivityResult(actionInvokerWrapper, ::onActionInvokerResults)
    private val actionInvokerLauncherForSharingFiles = registerForActivityResult(ActionInvokerWrapperForSharingFiles(actionInvokerWrapper), ::onActionInvokerResults)
    private val actionInvokerLauncherForCopyFileInto = registerForActivityResult(actionInvokerWrapper, ::onActionInvokerResultsForCopyFileInto)
    private val actionInvokerLauncherForCopyFileFromPathSelectFile  = registerForActivityResult(actionInvokerWrapper, ::onActionInvokerResultsForCopyFileFromPathSelectFile)
    private val actionInvokerLauncherForCopyFileFromFileGetSharableUri  = registerForActivityResult(actionInvokerWrapper, ::onActionInvokerResultsForCopyFileFromFileGetSharableUri)

    private lateinit var checkBoxDisplayResultsAsReturned: CheckBox
    private lateinit var checkBoxAbortOnFirstException: CheckBox
    private lateinit var textViewResults: TextView

    private var textFileForCopyFileInto: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        checkBoxDisplayResultsAsReturned = findViewById(R.id.check_display_results_as_returned)
        checkBoxAbortOnFirstException = findViewById(R.id.check_abort_on_first_exception)
        textViewResults = findViewById(R.id.text_results)
    }

    private fun displayExceptionInResults(exception: Exception) {
        textViewResults.text = "Unexpected error: ${exception.message}"
    }

    private fun onActionInvokerResults(result: String) {
        try {
            val jsonArrayOrObject = JSONTokener(result).nextValue()
            val sb = StringBuilder()

            if( jsonArrayOrObject is JSONObject ) {
                if( checkBoxDisplayResultsAsReturned.isChecked ) {
                    sb.append(jsonArrayOrObject.toString(2))
                }
                else {
                    formatResults(sb, jsonArrayOrObject)
                }
            }
            else {
                val jsonArray = jsonArrayOrObject as JSONArray

                if( checkBoxDisplayResultsAsReturned.isChecked ) {
                    sb.append(jsonArray.toString(2))
                }
                else {
                    for( i in 0 until jsonArray.length() ) {
                        formatResults(sb, jsonArray.getJSONObject(i))
                    }
                }
            }

            textViewResults.text = sb.toString()
        }
        catch( exception: Exception ) {
            displayExceptionInResults(exception)
        }
    }

    private fun formatResults(sb: StringBuilder, result: JSONObject) {
        assert(!checkBoxDisplayResultsAsReturned.isChecked)
        if( sb.isNotEmpty() ) {
            sb.append("\n\n")
        }

        when( val value = ActionInvokerResultParser(false).parse(result) ) {
            is JSONObject -> {
                sb.append(value.toString(2))
            }
            is JSONArray -> {
                sb.append(value.toString(2))
            }
            else -> {
                sb.append(value)
            }
        }
    }

    private fun<T> runAction(action: T, launchCallback: ( (String) -> Unit )? = null) {
        actionInvokerWrapper.abortOnException = checkBoxAbortOnFirstException.isChecked
        try {
            val actionString = action.toString()
            if( launchCallback != null ) {
                launchCallback(actionString)
            }
            else {
                actionInvokerLauncherDefault.launch(actionString)
            }
        }
        catch( exception: Exception ) {
            textViewResults.text = "There was an error trying to run the Action Invoker. Is CSEntry installed?\n\n" + exception.message
        }
    }

    fun runMessageFormatText(v: View) {
        val action = JSONObject()
            .put("action", "Message.formatText")
            .put("text", "%s, %s!")
            .put("arguments", JSONArray().put("Hello").put("World"))
        runAction(action)
    }

    private fun runMessageAction(valid1: Boolean, valid2: Boolean) {
        val actions = JSONArray()

        for( i in 1..2 ) {
            val useValidMessageNumber = if( i == 1 ) valid1 else valid2
            val action = JSONObject()
                .put("action", "Message.formatText")
                .put("number", if( useValidMessageNumber ) 94306 else -99999)
                .put("type", "system")
                .put("arguments", JSONArray().put("my_data$i.csdbe").put(4 + Random().nextInt(4)))
            actions.put(action)
        }

        runAction(actions)
    }

    fun runMessageActionValidValid(v: View) {
        runMessageAction(valid1 = true, valid2 = true)
    }

    fun runMessageActionValidInvalid(v: View) {
        runMessageAction(valid1 = true, valid2 = false)
    }

    fun runMessageActionInvalidValid(v: View) {
        runMessageAction(valid1 = false, valid2 = true)
    }

    fun runPathGetDirectoryListing(v: View) {
        val action = JSONObject()
            .put("action", "Path.getDirectoryListing")
            .put("path", ".")
            .put("recursive", true)
        runAction(action)
    }

    fun runFileReadLines(v: View) {
        val action = JSONObject()
            .put("action", "File.readLines")
            .put("path", "sync.log")
        runAction(action)
    }

    fun runCopyFileIntoCSEntry(v: View) {
        try {
            val timestamp = System.currentTimeMillis() / 1000.0
            textFileForCopyFileInto = File(getExternalFilesDir(null), "action-invoker-demo-${timestamp.toLong()}.txt")
            textFileForCopyFileInto!!.writeText("Hello from the Action Invoker Demo at ${"%.3f".format(timestamp)}!")

            val action = JSONObject()
                .put("action", "Path.showFileDialog")
                .put("type", "save")
                .put("title", "Select the path where you want to save ${textFileForCopyFileInto!!.name}")
                .put("name", textFileForCopyFileInto!!.name)
            runAction(action) { actionString ->
                actionInvokerLauncherForCopyFileInto.launch(actionString)
            }
        }
        catch( exception: Exception ) {
            displayExceptionInResults(exception)
        }
    }

    private fun onActionInvokerResultsForCopyFileInto(result: String) {
        assert(textFileForCopyFileInto != null)

        try {
            val jsonObject = JSONTokener(result).nextValue() as JSONObject
            val destinationPath = ActionInvokerResultParser(true).parse(jsonObject) as String?

            // quit out if the user didn't select a path
            if( destinationPath == null ) {
                textViewResults.text = "No path selected."
                return
            }

            val sourceUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), textFileForCopyFileInto!!)

            val intentUris = ArrayList<Uri>()
            intentUris.add(sourceUri)

            val action = JSONObject()
                .put("action", "File.copy")
                .put("source", sourceUri)
                .put("destination", destinationPath)
            runAction(action) { actionString ->
                actionInvokerLauncherForSharingFiles.launch(Pair(actionString, intentUris))
            }
        }
        catch( exception: Exception ) {
            displayExceptionInResults(exception)
        }
    }

    fun runCopyFileFromCSEntry(v: View) {
        val action = JSONObject()
            .put("action", "Path.selectFile")
            .put("title", "Select the file you want to send to the Action Invoker Demo")
        runAction(action) { actionString ->
            actionInvokerLauncherForCopyFileFromPathSelectFile.launch(actionString)
        }
    }

    private fun onActionInvokerResultsForCopyFileFromPathSelectFile(result: String) {
        try {
            val jsonObject = JSONTokener(result).nextValue() as JSONObject
            val sourcePath = ActionInvokerResultParser(true).parse(jsonObject) as String?

            // quit out if the user didn't select a file
            if( sourcePath == null ) {
                textViewResults.text = "No file selected."
                return
            }

            // get a sharable URI for the file
            val action = JSONObject()
                .put("action", "File.getSharableUri")
                .put("path", sourcePath)
            runAction(action) { actionString ->
                actionInvokerLauncherForCopyFileFromFileGetSharableUri.launch(actionString)
            }
        }
        catch( exception: Exception ) {
            displayExceptionInResults(exception)
        }
    }

    private fun onActionInvokerResultsForCopyFileFromFileGetSharableUri(result: String) {
        try {
            val jsonObject = JSONTokener(result).nextValue() as JSONObject
            val uriString = ActionInvokerResultParser(true).parse(jsonObject) as String
            val uri = Uri.parse(uriString)

            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IOException("Failed to open content URI '$uri'")
            inputStream.use {
                val outputFile = File(getExternalFilesDir(null), "copied-from-csentry-" + File(uri.path!!).name)
                FileOutputStream(outputFile, false).use {
                    inputStream.copyTo(it)
                }
                textViewResults.text = "File copied: ${outputFile.path}"
            }
        }
        catch( exception: Exception ) {
            displayExceptionInResults(exception)
        }
    }
}
