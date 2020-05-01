package io.scal.commandbasedarchitecture.sample_coroutine.repository

import io.scal.commandbasedarchitecture.sample_coroutine.model.MainItem
import kotlinx.coroutines.delay
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object HardCodeRepository {

    private val random = SecureRandom()
    private const val allowedCharacters = "0123456789qwertyuiopasdfghjklzxcvbnm"

    suspend fun loadNextMainPage(page: Int, pageSize: Int): List<MainItem> {
        delay(
            TimeUnit.MILLISECONDS
                .convert(
                    random.nextInt(9).toLong(),
                    TimeUnit.SECONDS
                )
        )

        if (random.nextInt(100) < 90) {
            val result = (0 until pageSize)
                .map {
                    val uid = nextUid(page, it, pageSize)
                    MainItem(uid, uid, nextTitle())
                }
            return if (random.nextInt(100) < 80) result else emptyList()
        } else {
            throw IllegalStateException("page loading error")
        }
    }

    suspend fun loadItemDetails(itemUid: String): MainItem {
        delay(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS))

        return MainItem(
            itemUid,
            itemUid,
            "if you use Broadcast - title and any favorite changes will be shown on the list screen too"
        )
    }

    private fun nextUid(page: Int, it: Int, pageSize: Int): String =
        (page * pageSize + it).toString()

    private fun nextTitle(): String {
        val sizeOfRandomString = random.nextInt(5) + 4
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString)
            sb.append(allowedCharacters[random.nextInt(allowedCharacters.length)])
        return sb.toString()
    }

    suspend fun changeFavoriteStatus(itemUid: String, newFavoriteState: Boolean) {
        delay(random.nextInt(5000).toLong())

        if (random.nextBoolean()) throw IllegalStateException("random favorite change error for: $itemUid")
    }
}