package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model

sealed class UIProgressErrorItem(key: String) : UIItem(key) {

    object Nothing : UIProgressErrorItem("UIProgressErrorItem.Nothing") {
        private fun readResolve(): Any = Nothing
    }

    object Progress : UIProgressErrorItem("UIProgressErrorItem.Progress") {
        private fun readResolve(): Any = Progress
    }

    data class Error(
        val error: String,
        val retryFunction: (() -> Unit)?
    ) : UIProgressErrorItem("UIProgressErrorItem.Error")
}