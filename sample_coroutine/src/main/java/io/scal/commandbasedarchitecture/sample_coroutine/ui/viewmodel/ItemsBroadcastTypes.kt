package io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel

sealed class ItemsBroadcastTypes {

    object AllItems : ItemsBroadcastTypes()
    data class OneItem(val uid: String) : ItemsBroadcastTypes()
}