package com.example.hia

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** 任务记录数据模型 */
data class TaskRecord(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val photoCount: Int
)

/** 简单基于 SharedPreferences + JSON 的任务记录持久化实现 */
class TaskRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("task_records", Context.MODE_PRIVATE)

    private val KEY_TASKS = "tasks_json"

    suspend fun loadTasks(): List<TaskRecord> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_TASKS, null) ?: return@withContext emptyList()
        runCatching {
            val arr = JSONArray(json)
            val list = ArrayList<TaskRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optLong("id", 0L)
                val start = obj.optLong("startTime", 0L)
                val end = if (obj.has("endTime") && !obj.isNull("endTime")) obj.optLong("endTime") else null
                val count = obj.optInt("photoCount", 0)
                if (id != 0L && start != 0L) {
                    list.add(TaskRecord(id = id, startTime = start, endTime = end, photoCount = count))
                }
            }
            list.toList()
        }.getOrElse {
            emptyList()
        }
    }

    suspend fun saveTasks(tasks: List<TaskRecord>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("startTime", task.startTime)
            if (task.endTime != null) {
                obj.put("endTime", task.endTime)
            } else {
                obj.put("endTime", JSONObject.NULL)
            }
            obj.put("photoCount", task.photoCount)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }
}