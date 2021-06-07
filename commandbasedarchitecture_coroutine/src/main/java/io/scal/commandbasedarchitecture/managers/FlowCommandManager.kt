package io.scal.commandbasedarchitecture.managers

import kotlinx.coroutines.flow.MutableStateFlow

class FlowCommandManager<State>(
    private val dataState: MutableStateFlow<State>,
    executionController: ExecutionController<State>,
    infoLoggerCallback: ((message: String) -> Unit)? = null,
    errorLoggerCallback: ((message: String, error: Throwable) -> Unit)? = null
) : CommandManager<State>(executionController, infoLoggerCallback, errorLoggerCallback) {

    override fun getCurrentDataState(): State =
        dataState.value ?: throw Exception("nullable state is not supported")

    override fun setValueIfNotTheSame(newState: State) {
        if (dataState.value != newState) {
            dataState.value = newState
        }
    }
}