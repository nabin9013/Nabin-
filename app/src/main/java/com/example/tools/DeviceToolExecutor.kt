package com.example.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ToolExecutionEvent(
    val toolName: String,
    val summary: String,
    val success: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

class DeviceToolExecutor(private val context: Context) {

    companion object {
        private const val TAG = "DeviceToolExecutor"

        const val TOOL_OPEN_APP = "openApp"
        const val TOOL_SEARCH_AND_CALL = "searchAndCallContact"
        const val TOOL_SEND_WHATSAPP = "sendWhatsAppMessage"

        fun buildGeminiTools(): List<Tool> {
            val openAppDeclaration = FunctionDeclaration(
                name = TOOL_OPEN_APP,
                description = "Launches any installed Android application by package name or common app name (for example YouTube, Instagram, Calculator, WhatsApp, Chrome, Maps, Spotify, Camera).",
                parameters = mapOf(
                    "packageName" to Schema.string(
                        description = "Android package name (e.g. 'com.google.android.youtube') or human-readable app name (e.g. 'YouTube', 'Instagram', 'Calculator')."
                    )
                )
            )

            val searchAndCallDeclaration = FunctionDeclaration(
                name = TOOL_SEARCH_AND_CALL,
                description = "Searches the Android Contacts Provider for a contact by name and triggers a phone call via ACTION_CALL.",
                parameters = mapOf(
                    "contactName" to Schema.string(
                        description = "Name or partial name of the contact to call."
                    )
                )
            )

            val sendWhatsAppDeclaration = FunctionDeclaration(
                name = TOOL_SEND_WHATSAPP,
                description = "Finds a contact in Android Contacts and opens a WhatsApp message deep-link prefilled with the message text.",
                parameters = mapOf(
                    "contactName" to Schema.string(
                        description = "Name of the contact to message on WhatsApp."
                    ),
                    "message" to Schema.string(
                        description = "Message body to send to the contact."
                    )
                )
            )

            return listOf(
                Tool.functionDeclarations(
                    listOf(
                        openAppDeclaration,
                        searchAndCallDeclaration,
                        sendWhatsAppDeclaration
                    )
                )
            )
        }
    }

    private data class ContactMatch(
        val displayName: String,
        val phoneNumber: String
    )

    suspend fun execute(
        functionCall: FunctionCallPart,
        onToolEvent: (ToolExecutionEvent) -> Unit
    ): FunctionResponsePart = withContext(Dispatchers.IO) {
        val result = when (functionCall.name) {
            TOOL_OPEN_APP -> {
                val pkgOrName = functionCall.args["packageName"]
                    ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                handleOpenApp(pkgOrName, onToolEvent)
            }

            TOOL_SEARCH_AND_CALL -> {
                val contactName = functionCall.args["contactName"]
                    ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                handleSearchAndCallContact(contactName, onToolEvent)
            }

            TOOL_SEND_WHATSAPP -> {
                val contactName = functionCall.args["contactName"]
                    ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val message = functionCall.args["message"]
                    ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                handleSendWhatsAppMessage(contactName, message, onToolEvent)
            }

            else -> {
                val msg = "Unknown tool: ${functionCall.name}"
                onToolEvent(ToolExecutionEvent(functionCall.name, msg, success = false))
                errorJson(msg)
            }
        }

        FunctionResponsePart(
            name = functionCall.name,
            response = result,
            id = functionCall.id
        )
    }

    private fun handleOpenApp(
        packageOrName: String,
        onToolEvent: (ToolExecutionEvent) -> Unit
    ): JsonObject {
        if (packageOrName.isBlank()) {
            val msg = "No app name or packageName was provided."
            onToolEvent(ToolExecutionEvent(TOOL_OPEN_APP, msg, success = false))
            return errorJson(msg)
        }

        val pm = context.packageManager
        // 1. Direct package lookup first
        pm.getLaunchIntentForPackage(packageOrName)?.let { directIntent ->
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(directIntent)
            val label = resolveAppLabel(pm, packageOrName) ?: packageOrName
            onToolEvent(ToolExecutionEvent(TOOL_OPEN_APP, "Opened $label", success = true))
            return successJson(
                message = "Launched $label ($packageOrName).",
                extra = mapOf("packageName" to JsonPrimitive(packageOrName), "appName" to JsonPrimitive(label))
            )
        }

        // 2. Match against known aliases and installed launcher activities
        val normalizedQuery = normalizeQuery(packageOrName)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        val matched = activities.firstOrNull { resolve ->
            val label = resolve.loadLabel(pm).toString()
            normalizeQuery(label) == normalizedQuery ||
                resolve.activityInfo.packageName.equals(packageOrName, ignoreCase = true)
        } ?: activities.firstOrNull { resolve ->
            val label = normalizeQuery(resolve.loadLabel(pm).toString())
            val pkg = resolve.activityInfo.packageName.lowercase()
            label.contains(normalizedQuery) ||
                normalizedQuery.contains(label) ||
                pkg.contains(normalizedQuery)
        }

        if (matched != null) {
            val targetPkg = matched.activityInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
                ?: Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(targetPkg, matched.activityInfo.name)
                }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            val appLabel = matched.loadLabel(pm).toString()
            onToolEvent(ToolExecutionEvent(TOOL_OPEN_APP, "Opened $appLabel", success = true))
            return successJson(
                message = "Launched $appLabel ($targetPkg).",
                extra = mapOf("packageName" to JsonPrimitive(targetPkg), "appName" to JsonPrimitive(appLabel))
            )
        }

        val err = "Could not find an installed app matching '$packageOrName'."
        onToolEvent(ToolExecutionEvent(TOOL_OPEN_APP, err, success = false))
        return errorJson(err)
    }

    private fun handleSearchAndCallContact(
        contactName: String,
        onToolEvent: (ToolExecutionEvent) -> Unit
    ): JsonObject {
        if (contactName.isBlank()) {
            val msg = "Contact name is empty."
            onToolEvent(ToolExecutionEvent(TOOL_SEARCH_AND_CALL, msg, success = false))
            return errorJson(msg)
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            val msg = "READ_CONTACTS permission is not granted yet. Ask the user to grant Contacts permission in Mayra."
            onToolEvent(ToolExecutionEvent(TOOL_SEARCH_AND_CALL, "Contacts permission required", success = false))
            return errorJson(msg)
        }

        val contact = findContactByName(contactName)
            ?: run {
                val msg = "No contact found matching '$contactName'."
                onToolEvent(ToolExecutionEvent(TOOL_SEARCH_AND_CALL, msg, success = false))
                return errorJson(msg)
            }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val telUri = Uri.parse("tel:${Uri.encode(contact.phoneNumber)}")
        val callIntent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, telUri)
        } else {
            Intent(Intent.ACTION_DIAL, telUri)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(callIntent)
            val actionDesc = if (hasCallPermission) {
                "Calling ${contact.displayName}"
            } else {
                "Dialing ${contact.displayName} (grant Call permission for instant call)"
            }
            onToolEvent(ToolExecutionEvent(TOOL_SEARCH_AND_CALL, actionDesc, success = true))
            successJson(
                message = "$actionDesc (${contact.phoneNumber}).",
                extra = mapOf(
                    "contactName" to JsonPrimitive(contact.displayName),
                    "phoneNumber" to JsonPrimitive(contact.phoneNumber),
                    "directCall" to JsonPrimitive(hasCallPermission)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call", e)
            val msg = "Unable to place call: ${e.localizedMessage ?: "No dialer available"}"
            onToolEvent(ToolExecutionEvent(TOOL_SEARCH_AND_CALL, msg, success = false))
            errorJson(msg)
        }
    }

    private fun handleSendWhatsAppMessage(
        contactName: String,
        message: String,
        onToolEvent: (ToolExecutionEvent) -> Unit
    ): JsonObject {
        if (contactName.isBlank() || message.isBlank()) {
            val msg = "Both contactName and message are required to send a WhatsApp message."
            onToolEvent(ToolExecutionEvent(TOOL_SEND_WHATSAPP, msg, success = false))
            return errorJson(msg)
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            val msg = "READ_CONTACTS permission is required to look up '$contactName'."
            onToolEvent(ToolExecutionEvent(TOOL_SEND_WHATSAPP, "Contacts permission required", success = false))
            return errorJson(msg)
        }

        val contact = findContactByName(contactName)
            ?: run {
                val msg = "Could not find '$contactName' in your contacts."
                onToolEvent(ToolExecutionEvent(TOOL_SEND_WHATSAPP, msg, success = false))
                return errorJson(msg)
            }

        val digitsOnly = contact.phoneNumber.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            val msg = "Found '${contact.displayName}', but they don't have a valid phone number."
            onToolEvent(ToolExecutionEvent(TOOL_SEND_WHATSAPP, msg, success = false))
            return errorJson(msg)
        }

        val encodedText = Uri.encode(message)
        val deepLink = Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly&text=$encodedText")
        val pm = context.packageManager

        val whatsappPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (whatsappPackage != null) {
                setPackage(whatsappPackage)
            }
        }

        return try {
            context.startActivity(intent)
            val summary = "WhatsApp ready for ${contact.displayName}"
            onToolEvent(ToolExecutionEvent(TOOL_SEND_WHATSAPP, summary, success = true))
            successJson(
                message = "Opened WhatsApp chat with ${contact.displayName} prefilled with your message.",
                extra = mapOf(
                    "contactName" to JsonPrimitive(contact.displayName),
                    "phoneNumber" to JsonPrimitive(contact.phoneNumber)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WhatsApp deep link", e)
            val msg = "WhatsApp could not be opened: ${e.localizedMessage ?: "App unavailable"}"
            onToolEvent(ToolExecutionEvent(TOOL_SEND_WHATSAPP, msg, success = false))
            errorJson(msg)
        }
    }

    private fun findContactByName(query: String): ContactMatch? {
        val resolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        // Exact or SQL LIKE match first
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0).orEmpty()
                val number = cursor.getString(1).orEmpty()
                if (number.isNotBlank()) {
                    return ContactMatch(name.ifBlank { query }, number)
                }
            }
        }

        // Case-insensitive token fallback across phone contacts
        val queryTokens = query.lowercase().split(" ").filter { it.isNotBlank() }
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0).orEmpty()
                val number = cursor.getString(1).orEmpty()
                if (name.isBlank() || number.isBlank()) continue
                val lower = name.lowercase()
                if (queryTokens.all { lower.contains(it) }) {
                    return ContactMatch(name, number)
                }
            }
        }
        return null
    }

    private fun resolveAppLabel(pm: PackageManager, packageName: String): String? =
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            null
        }

    private fun normalizeQuery(raw: String): String =
        raw.lowercase()
            .replace("app", "")
            .replace("com.google.android.", "")
            .replace("com.android.", "")
            .replace(".", "")
            .replace(" ", "")
            .trim()

    private fun successJson(
        message: String,
        extra: Map<String, JsonPrimitive> = emptyMap()
    ): JsonObject = JsonObject(
        buildMap {
            put("success", JsonPrimitive(true))
            put("message", JsonPrimitive(message))
            putAll(extra)
        }
    )

    private fun errorJson(message: String): JsonObject = JsonObject(
        mapOf(
            "success" to JsonPrimitive(false),
            "error" to JsonPrimitive(message)
        )
    )
}
