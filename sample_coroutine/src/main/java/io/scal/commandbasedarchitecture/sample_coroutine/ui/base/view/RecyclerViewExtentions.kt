package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.listenForEndScroll(
    visibleThreshold: Int,
    endAction: () -> Unit
) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val tmpLayoutManager = layoutManager
            val goodDirection = dy >= 0
            if (goodDirection && tmpLayoutManager is LinearLayoutManager) {
                val visibleItemCount = tmpLayoutManager.childCount
                val totalItemCount = tmpLayoutManager.itemCount
                val firstVisibleItem = tmpLayoutManager.findFirstVisibleItemPosition()

                if (totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
                    notifyEndAction(this@listenForEndScroll, endAction)
                }
            }
        }
    })
}

private fun notifyEndAction(recyclerView: RecyclerView, endAction: () -> Unit) {
    recyclerView.post { endAction() }
}