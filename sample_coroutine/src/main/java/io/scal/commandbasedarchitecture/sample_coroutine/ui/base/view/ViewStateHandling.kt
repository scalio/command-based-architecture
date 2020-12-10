package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view

import android.view.View
import android.widget.TextView
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.databinding.ItemProgressErrorBinding
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem

fun UIItem?.handleProgressErrorState(progressErrorView: ItemProgressErrorBinding) {
    when (this) {
        UIProgressErrorItem.Progress ->
            showProgressState(progressErrorView)
        is UIProgressErrorItem.Error ->
            showErrorState(progressErrorView)
        else ->
            showNoProgressErrorState(progressErrorView)
    }
}

fun UIProgressErrorItem.Error.showErrorState(progressErrorView: ItemProgressErrorBinding) {
    progressErrorView.root.visibility = View.VISIBLE
    progressErrorView.cProgress.visibility = View.GONE
    progressErrorView.cError.visibility = View.VISIBLE
    progressErrorView.tvError.text = this@showErrorState.error

    if (null == retryFunction) {
        progressErrorView.bError.visibility = View.GONE
    } else {
        progressErrorView.bError.visibility = View.VISIBLE
        progressErrorView.bError
            .setOnClickListener { retryFunction.invoke() }
    }
}

fun showProgressState(progressErrorView: ItemProgressErrorBinding) {
    progressErrorView.root.visibility = View.VISIBLE
    progressErrorView.cProgress.visibility = View.VISIBLE
    progressErrorView.cError.visibility = View.GONE
}

fun showNoProgressErrorState(progressErrorView: ItemProgressErrorBinding) {
    progressErrorView.root.visibility = View.GONE
}