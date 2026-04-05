package com.mouli.habittracker.data.repository

import com.mouli.habittracker.data.local.dao.BookDao
import com.mouli.habittracker.data.local.entity.BookEntryEntity
import com.mouli.habittracker.model.BookStatus
import com.mouli.habittracker.model.BookSuggestionUiModel
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class BookRepository(
    private val bookDao: BookDao
) {
    fun observeBooks(): Flow<List<BookEntryEntity>> = bookDao.observeBooks()

    suspend fun searchBooks(query: String): List<BookSuggestionUiModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val googleBooks = runCatching { searchGoogleBooks(query) }.getOrDefault(emptyList())
        if (googleBooks.isNotEmpty()) {
            return@withContext googleBooks
        }

        runCatching { searchOpenLibrary(query) }.getOrDefault(emptyList())
    }

    private fun searchGoogleBooks(query: String): List<BookSuggestionUiModel> {
        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val json = getJson(
            "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=8&printType=books&projection=lite"
        )
        val items = json.optJSONArray("items") ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val volumeInfo = item.optJSONObject("volumeInfo") ?: continue
                val title = volumeInfo.optString("title").trim()
                if (title.isBlank()) continue

                add(
                    BookSuggestionUiModel(
                        id = item.optString("id"),
                        title = title,
                        author = joinTextArray(volumeInfo.optJSONArray("authors")).ifBlank { "Unknown author" },
                        totalPages = volumeInfo.optInt("pageCount", 0),
                        publishedYear = volumeInfo.optString("publishedDate")
                            .takeIf { it.isNotBlank() }
                            ?.substringBefore("-")
                            .orEmpty(),
                        coverUrl = volumeInfo.optJSONObject("imageLinks")
                            ?.optString("thumbnail")
                            .orEmpty()
                    )
                )
            }
        }
    }

    private fun searchOpenLibrary(query: String): List<BookSuggestionUiModel> {
        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val json = getJson("https://openlibrary.org/search.json?q=$encodedQuery&limit=8")
        val docs = json.optJSONArray("docs") ?: return emptyList()
        return buildList {
            for (index in 0 until docs.length()) {
                val doc = docs.optJSONObject(index) ?: continue
                val title = doc.optString("title").trim()
                if (title.isBlank()) continue

                val author = joinTextArray(doc.optJSONArray("author_name")).ifBlank { "Unknown author" }
                val coverId = doc.optInt("cover_i", 0)
                add(
                    BookSuggestionUiModel(
                        id = doc.optString("key").ifBlank { "$title-$author" },
                        title = title,
                        author = author,
                        totalPages = doc.optInt("number_of_pages_median", 0),
                        publishedYear = doc.optString("first_publish_year"),
                        coverUrl = if (coverId > 0) {
                            "https://covers.openlibrary.org/b/id/$coverId-M.jpg"
                        } else {
                            ""
                        }
                    )
                )
            }
        }.dedupeSuggestions()
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Hab-Android/1.0")
        }
        return try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(statusCode in 200..299 && body.isNotBlank()) {
                "Request failed with code $statusCode"
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun manualAddBook(title: String, author: String, totalPages: Int) {
        val entity = BookEntryEntity(
            title = title.trim(),
            author = author.trim(),
            totalPages = totalPages.coerceAtLeast(0),
            updatedAt = System.currentTimeMillis()
        )
        bookDao.upsert(entity)
        pushBookToRemote(entity)
    }

    private fun joinTextArray(values: JSONArray?): String =
        buildList {
            if (values != null) {
                for (index in 0 until values.length()) {
                    values.optString(index)
                        .takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
            }
        }.joinToString(", ")

    suspend fun addBookSuggestion(suggestion: BookSuggestionUiModel) {
        val existing = if (suggestion.id.isNotBlank()) {
            bookDao.getBySourceId(suggestion.id)
        } else {
            null
        }
        val updatedBook = if (existing != null) {
            existing.copy(
                title = suggestion.title,
                author = suggestion.author,
                totalPages = suggestion.totalPages,
                coverUrl = suggestion.coverUrl,
                status = BookStatus.READING,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            BookEntryEntity(
                sourceId = suggestion.id.takeIf { it.isNotBlank() },
                title = suggestion.title,
                author = suggestion.author,
                totalPages = suggestion.totalPages,
                coverUrl = suggestion.coverUrl
            )
        }
        bookDao.upsert(updatedBook)
        pushBookToRemote(updatedBook)
    }

    suspend fun deleteBook(id: String) {
        bookDao.deleteById(id)
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("books").document(id)
            .delete()
    }

    suspend fun setPagesRead(id: String, pages: Int) {
        val existing = bookDao.getById(id) ?: return
        val nextPages = pages.coerceAtLeast(0)
        val cappedPages = if (existing.totalPages > 0) nextPages.coerceAtMost(existing.totalPages) else nextPages
        val nextStatus = if (existing.totalPages > 0 && cappedPages >= existing.totalPages) {
            BookStatus.COMPLETED
        } else {
            existing.status
        }
        val updated = existing.copy(
            pagesRead = cappedPages,
            status = nextStatus,
            updatedAt = System.currentTimeMillis()
        )
        bookDao.upsert(updated)
        pushBookToRemote(updated)
    }

    suspend fun incrementProgress(id: String, delta: Int) {
        val existing = bookDao.getById(id) ?: return
        val nextPages = (existing.pagesRead + delta).coerceAtLeast(0)
        val cappedPages = if (existing.totalPages > 0) nextPages.coerceAtMost(existing.totalPages) else nextPages
        val nextStatus = if (existing.totalPages > 0 && cappedPages >= existing.totalPages) {
            BookStatus.COMPLETED
        } else {
            existing.status
        }
        val updated = existing.copy(
            pagesRead = cappedPages,
            status = nextStatus,
            updatedAt = System.currentTimeMillis()
        )
        bookDao.upsert(updated)
        pushBookToRemote(updated)
    }

    suspend fun setStatus(id: String, status: BookStatus, rating: Int = 0) {
        val existing = bookDao.getById(id) ?: return
        val updatedPages = when (status) {
            BookStatus.READING -> existing.pagesRead.coerceAtMost((existing.totalPages - 1).coerceAtLeast(existing.pagesRead))
            BookStatus.COMPLETED -> if (existing.totalPages > 0) existing.totalPages else existing.pagesRead
            BookStatus.ON_HOLD -> existing.pagesRead
        }
        val updated = existing.copy(
            pagesRead = updatedPages,
            status = status,
            rating = if (status == BookStatus.COMPLETED) rating else 0,
            updatedAt = System.currentTimeMillis()
        )
        bookDao.upsert(updated)
        pushBookToRemote(updated)
    }

    private suspend fun pushBookToRemote(book: BookEntryEntity) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("books").document(book.id)
            .set(book)
    }

    suspend fun syncBooksFromRemote(userId: String) {
        android.util.Log.d("BookRepo", "Starting book sync for user $userId")
        val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("books")
            .get()
            .await()
        
        val remoteBooks = snapshot.documents.mapNotNull { doc ->
            doc.toObject(BookEntryEntity::class.java)
        }
        if (remoteBooks.isNotEmpty()) {
            android.util.Log.d("BookRepo", "Restored ${remoteBooks.size} books from cloud")
            bookDao.upsertAll(remoteBooks)
        }
    }
}

private fun List<BookSuggestionUiModel>.dedupeSuggestions(): List<BookSuggestionUiModel> {
    val seen = LinkedHashSet<String>()
    return filter { suggestion ->
        val key = "${suggestion.title.trim().lowercase()}|${suggestion.author.trim().lowercase()}"
        seen.add(key)
    }
}
