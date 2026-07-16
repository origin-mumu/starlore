package com.starlore.app.feature.screenextract

import com.starlore.app.data.todo.TodoRepository
import com.starlore.app.data.todo.reminder.TodoAlarmScheduler
import com.starlore.app.data.utils.EventItem

class ScreenExtractRepository(
    private val todoRepository: TodoRepository,
    private val alarmScheduler: TodoAlarmScheduler,
    private val screenshotCapturer: ShizukuScreenshotCapturer = ShizukuScreenshotCapturer(),
    private val aiTodoExtractor: AiTodoExtractor = AiTodoExtractor()
) {
    suspend fun captureExtractAndInsert(
        onProgress: (ScreenExtractPhase) -> Unit = {}
    ): Int {
        val items = captureAndExtract(onProgress)
        onProgress(ScreenExtractPhase.Importing)
        return insertExtractedTodos(items)
    }

    suspend fun captureAndExtract(
        onProgress: (ScreenExtractPhase) -> Unit = {}
    ): List<EventItem> {
        onProgress(ScreenExtractPhase.Capturing)
        val screenshot = screenshotCapturer.collapsePanelsAndCapturePng()
        val imageDataUrl = screenshot.toCompressedScreenshotDataUrl()

        onProgress(ScreenExtractPhase.Extracting)
        return aiTodoExtractor.extractFromImage(imageDataUrl).items
    }

    suspend fun insertExtractedTodos(items: List<EventItem>): Int {
        val insertedTodos = todoRepository.insertAiGeneratedTodosWithSubTasks(items)
        insertedTodos.forEach { todo ->
            if (!todo.isCompleted) {
                alarmScheduler.schedule(todo)
            }
        }
        return insertedTodos.size
    }
}
