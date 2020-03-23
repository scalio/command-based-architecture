package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates3.AdapterDelegate

internal abstract class AdapterDelegateBase<T> : AdapterDelegate<T>() {

    protected abstract val layoutId: Int

    final override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        initView(view)
        return BindingViewHolder(view)
    }

    protected open fun initView(view: View) {}

    open class BindingViewHolder(root: View) : RecyclerView.ViewHolder(root)
}