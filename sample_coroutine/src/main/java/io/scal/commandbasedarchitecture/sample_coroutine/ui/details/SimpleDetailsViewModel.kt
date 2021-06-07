package io.scal.commandbasedarchitecture.sample_coroutine.ui.details

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.scal.commandbasedarchitecture.managers.ExecutionController
import io.scal.commandbasedarchitecture.managers.FlowCommandManager
import io.scal.commandbasedarchitecture.managers.ICommandManager
import io.scal.commandbasedarchitecture.sample_coroutine.repository.HardCodeRepository
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.toUIMainItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class SimpleDetailsViewModel(application: Application) : DetailsViewModel(application) {

    private val mutableScreenState = MutableStateFlow(
        DetailsScreenState(null, null)
    )

    override val screenState: LiveData<DetailsScreenState> by lazy {
        mutableScreenState
            .asLiveData(viewModelScope.coroutineContext)
    }

    private val commandManager: ICommandManager<DetailsScreenState> by lazy {
        FlowCommandManager(
            mutableScreenState,
            ExecutionController(viewModelScope),
            { Log.w("SimpleViewModel", it) },
            { message, error -> Log.w("SimpleViewModel", message, error) }
        )
    }


    override fun reload() {
        commandManager.postCommand(
            ReloadDetailsCommand(
                {
                    withContext(Dispatchers.IO) {
                        HardCodeRepository.loadItemDetails(itemUid).toUIMainItem()
                    }
                },
                { UIProgressErrorItem.Error(it.toString()) { reload() } }
            )
        )
    }

    override fun addToFavorite(item: UIMainItem) {
        executeFavoriteChangeAction(true)
    }

    override fun removeFromFavorite(item: UIMainItem) {
        executeFavoriteChangeAction(false)
    }

    private fun executeFavoriteChangeAction(newFavoriteStatus: Boolean) {
        commandManager.postCommand(
            ChangeFavoriteStatusCommand(
                newFavoriteStatus,
                {
                    withContext(Dispatchers.IO) {
                        HardCodeRepository.changeFavoriteStatus(itemUid, newFavoriteStatus)
                    }
                },
                { Toast.makeText(getApplication(), it.toString(), Toast.LENGTH_SHORT).show() }
            )
        )
    }
}