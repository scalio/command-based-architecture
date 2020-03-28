package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view

import android.view.View
import android.widget.TextView
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem

fun UIItem?.handleProgressErrorState(progressErrorView: View) {
    when (this) {
        UIProgressErrorItem.Progress ->
            showProgressState(progressErrorView)
        is UIProgressErrorItem.Error ->
            showErrorState(progressErrorView)
        else ->
            showNoProgressErrorState(progressErrorView)
    }
}

fun UIProgressErrorItem.Error.showErrorState(progressErrorView: View) {
    progressErrorView.visibility = View.VISIBLE
    progressErrorView.findViewById<View>(R.id.cProgress).visibility = View.GONE
    progressErrorView.findViewById<View>(R.id.cError).visibility = View.VISIBLE
    progressErrorView.findViewById<TextView>(R.id.tvError).text = error

    if (null == retryFunction) {
        progressErrorView.findViewById<View>(R.id.bError).visibility = View.GONE
    } else {
        progressErrorView.findViewById<View>(R.id.bError).visibility = View.VISIBLE
        progressErrorView.findViewById<View>(R.id.bError)
            .setOnClickListener { retryFunction.invoke() }
    }
}

fun showProgressState(progressErrorView: View) {
    progressErrorView.visibility = View.VISIBLE
    progressErrorView.findViewById<View>(R.id.cProgress).visibility = View.VISIBLE
    progressErrorView.findViewById<View>(R.id.cError).visibility = View.GONE
}

fun showNoProgressErrorState(progressErrorView: View) {
    progressErrorView.visibility = View.GONE
}