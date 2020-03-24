package io.scal.commandbasedarchitecture.sample_coroutine.repository

import io.scal.commandbasedarchitecture.sample_coroutine.model.MainItem
import kotlinx.coroutines.delay
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

object HardCodeRepository {

    private val random = SecureRandom()
    private const val allowedCharacters = "0123456789qwertyuiopasdfghjklzxcvbnm"

    suspend fun loadNextMainPage(page: Int, pageSize: Int): List<MainItem> {
        delay(
            TimeUnit.MILLISECONDS
                .convert(
                    random.nextInt(9).toLong() + 10,
                    TimeUnit.SECONDS
                )
        )

        if (random.nextInt(100) < 90) {
            val result = (0 until pageSize)
                .map {
                    val uid = nextUid()
                    MainItem(uid, uid, "${page * pageSize + it}\u00A0${nextTitle()}")
                }
            return if (random.nextBoolean()) result else emptyList()
        } else {
            throw IllegalStateException("page loading error")
        }
    }

    suspend fun changeFavoriteStatus(itemUid: String, newFavoriteState: Boolean) {
        delay(random.nextInt(5000).toLong())

        if (random.nextBoolean()) throw IllegalStateException("random favorite change error for: $itemUid")
    }

    private fun nextUid(): String = UUID.randomUUID().toString()

    private fun nextTitle(): String {
        val sizeOfRandomString = random.nextInt(5) + 4
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString)
            sb.append(allowedCharacters[random.nextInt(allowedCharacters.length)])
        return sb.toString()
    }
}