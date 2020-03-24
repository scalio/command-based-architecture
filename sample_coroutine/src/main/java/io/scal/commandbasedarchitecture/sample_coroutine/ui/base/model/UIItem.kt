package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model

import java.io.Serializable

abstract class UIItem(val key: String) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UIItem

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int = key.hashCode()
}