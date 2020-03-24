package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem

data class UIMainItem(
    val uid: String,
    val title: String,
    val favoriteState: FavoriteState
) : UIItem(uid)

sealed class FavoriteState(open val favorite: Boolean) {
    abstract fun newSelection(changeToFavorite: Boolean): FavoriteState
    abstract fun newFinalState(changeToFavorite: Boolean): FavoriteState
    abstract fun revertState(fallBackState: Boolean): FavoriteState
    abstract fun hasChange(): Boolean

    data class NoProgress(override val favorite: Boolean) : FavoriteState(favorite) {
        override fun newSelection(changeToFavorite: Boolean): FavoriteState =
            PreSelectProgress(this, changeToFavorite)

        override fun newFinalState(changeToFavorite: Boolean): FavoriteState =
            NoProgress(changeToFavorite)

        override fun revertState(fallBackState: Boolean): FavoriteState =
            NoProgress(favorite)

        override fun hasChange(): Boolean =
            false
    }

    data class PreSelectProgress(val previous: FavoriteState, val newFavorite: Boolean) :
        FavoriteState(newFavorite) {

        override fun newSelection(changeToFavorite: Boolean): FavoriteState =
            PreSelectProgress(previous, changeToFavorite)

        override fun newFinalState(changeToFavorite: Boolean): FavoriteState =
            if (newFavorite == changeToFavorite) {
                NoProgress(changeToFavorite)
            } else {
                PreSelectProgress(NoProgress(changeToFavorite), newFavorite)
            }

        override fun revertState(fallBackState: Boolean): FavoriteState =
            previous

        override fun hasChange(): Boolean =
            previous.favorite != newFavorite
    }
}