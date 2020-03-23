package io.scal.commandbasedarchitecture.sample_coroutine.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.scal.commandbasedarchitecture.pagination.dataAndNextPageLoadingStatus
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.handleProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.listenForEndScroll
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.showNoProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.adapter.MainAdapter
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    private val viewModelInstance: MainViewModel by activityViewModels()

    private val adapter by lazy { MainAdapter(viewModelInstance) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_main, container!!, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initStateModel()
    }

    private fun initRecyclerView() {
        rvData.setHasFixedSize(true)
        rvData.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rvData.adapter = adapter

        srlData.setOnRefreshListener { viewModelInstance.reload() }

        rvData.listenForEndScroll(4) { viewModelInstance.loadNextPage() }
    }

    private fun initStateModel() {
        viewModelInstance.dataState
            .observe(
                viewLifecycleOwner,
                Observer { dataState ->
                    if (null == dataState) return@Observer

                    val items = dataState.dataAndNextPageLoadingStatus
                    when {
                        items == null -> {
                            emptyData.visibility = View.GONE
                            if (dataState.refreshStatus is UIProgressErrorItem.Progress) {
                                showNoProgressErrorState(progressError)
                            } else {
                                dataState.refreshStatus.handleProgressErrorState(progressError)
                            }
                            adapter.releaseData()
                        }
                        items.isEmpty() -> {
                            emptyData.visibility = View.VISIBLE
                            showNoProgressErrorState(progressError)
                            adapter.releaseData()
                        }
                        else -> {
                            emptyData.visibility = View.GONE
                            showNoProgressErrorState(progressError)
                            adapter.setupData(items)
                        }
                    }

                    srlData.isRefreshing = dataState.refreshStatus is UIProgressErrorItem.Progress
                }
            )
    }
}