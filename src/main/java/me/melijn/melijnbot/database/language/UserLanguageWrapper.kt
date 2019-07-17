package me.melijn.melijnbot.database.language

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class UserLanguageWrapper(val taskManager: TaskManager, val userLanguageDao: UserLanguageDao) {
    val languageCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Long, String>() { key, executor -> getLanguage(key, executor) }

    fun getLanguage(userId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<String> {
        val languageFuture = CompletableFuture<String>()
        executor.execute {
            userLanguageDao.get(userId, Consumer { language ->
                languageFuture.complete(language)
            })
        }
        return languageFuture
    }
}