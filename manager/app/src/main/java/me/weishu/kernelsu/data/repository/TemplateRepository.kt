package me.weishu.kernelsu.data.repository

import android.util.Log
import me.weishu.kernelsu.data.ksu.getAppProfileTemplate
import me.weishu.kernelsu.data.model.TemplateInfo
import org.json.JSONObject

interface TemplateRepository {
    suspend fun getTemplates(sync: Boolean): Result<List<TemplateInfo>>
    suspend fun importTemplates(jsonString: String): Result<Unit>
    suspend fun exportTemplates(): Result<String>
    suspend fun getTemplate(id: String): Result<TemplateInfo>
}

private const val TEMPLATE_TAG = "TemplateRepository"

/**
 * Reads one stored app profile template, or null when the id is unknown or its JSON is invalid.
 *
 * Lives here rather than in a screen so any page - the template list, the app profile editor -
 * resolves a template id without depending on another screen's package.
 */
fun getTemplateInfoById(id: String): TemplateInfo? {
    return runCatching {
        TemplateInfo.fromJSON(JSONObject(getAppProfileTemplate(id)))
    }.onFailure {
        Log.e(TEMPLATE_TAG, "ignore invalid template: $it", it)
    }.getOrNull()
}
