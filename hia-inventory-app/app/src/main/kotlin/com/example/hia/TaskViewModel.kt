package com.example.hia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application.applicationContext)

    private val _taskList = MutableStateFlow<List<TaskRecord>>(emptyList())
    val taskList: StateFlow<List<TaskRecord>> = _taskList.asStateFlow()

    private val _currentTask = MutableStateFlow<TaskRecord?>(null)
    val currentTask: StateFlow<TaskRecord?> = _currentTask.asStateFlow()

    init {
        viewModelScope.launch {
            loadFromStorage()
        }
    }

    private suspend fun loadFromStorage() {
        val tasks = repository.loadTasks()
        // 约定：endTime == null 表示任务进行中（理论上只应有一条）
        val active = tasks.firstOrNull { it.endTime == null }
        _taskList.value = tasks.sortedByDescending { it.startTime }
        _currentTask.value = active
    }

    fun startNewTask() {
        viewModelScope.launch {
            val current = _currentTask.value
            if (current != null) {
                // 已有任务进行中，不重复创建
                return@launch
            }
            val now = System.currentTimeMillis()
            val list = _taskList.value
            val newId = (list.maxOfOrNull { it.id } ?: 0L) + 1L
            val newTask = TaskRecord(
                id = newId,
                startTime = now,
                endTime = null,
                photoCount = 0
            )
            val newList = listOf(newTask) + list
            _taskList.value = newList
            _currentTask.value = newTask
            repository.saveTasks(newList)
        }
    }

    fun endCurrentTask() {
        viewModelScope.launch {
            val current = _currentTask.value ?: return@launch
            val now = System.currentTimeMillis()
            val updated = current.copy(endTime = now)
            val newList = _taskList.value.map { if (it.id == current.id) updated else it }
            _taskList.value = newList
            _currentTask.value = null
            repository.saveTasks(newList)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val list = _taskList.value
            val newList = list.filterNot { it.id == taskId }
            _taskList.value = newList
            if (_currentTask.value?.id == taskId) {
                _currentTask.value = null
            }
            repository.saveTasks(newList)
        }
    }

    fun increasePhotoCountForCurrentTask() {
        viewModelScope.launch {
            val current = _currentTask.value ?: return@launch
            val updated = current.copy(photoCount = current.photoCount + 1)
            val newList = _taskList.value.map { if (it.id == current.id) updated else it }
            _taskList.value = newList
            _currentTask.value = updated
            repository.saveTasks(newList)
        }
    }
}