package io.scal.commandbasedarchitecture.sample_coroutine.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.scal.commandbasedarchitecture.sample_coroutine.databinding.FragmentDetailsBinding
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.handleProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.showNoProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.root.RootActivity

class ItemDetailsFragment : Fragment() {

    private val simpleViewModelInstance: DetailsViewModel by viewModels<SimpleDetailsViewModel>()
    private val broadCastViewModelInstance: DetailsViewModel by viewModels<BroadcastDetailsViewModel>()

    @Suppress("ConstantConditionIf")
    private val viewModelInstance: DetailsViewModel by lazy {
        if (RootActivity.userBroadcastViewModels) broadCastViewModelInstance else simpleViewModelInstance
    }

    private var binding: FragmentDetailsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailsBinding.inflate(inflater, container!!, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemUid = arguments?.getString("itemUid")
            ?: throw IllegalStateException("please pass itemUid for this fragment")

        viewModelInstance.itemUid = itemUid
        viewModelInstance.reload()

        val realBinding = binding!!

        initSwipeToRefreshView(realBinding)
        initStateModel(realBinding)
    }

    private fun initSwipeToRefreshView(realBinding: FragmentDetailsBinding) {
        realBinding.srlData.setOnRefreshListener { viewModelInstance.reload() }
    }

    private fun initStateModel(realBinding: FragmentDetailsBinding) {
        viewModelInstance.screenState
            .observe(
                viewLifecycleOwner,
                Observer { dataState ->
                    if (null == dataState) return@Observer

                    when (val item = dataState.item) {
                        // we do not have item data yet -> it is loading or we have loading error
                        null -> {
                            if (dataState.refreshStatus is UIProgressErrorItem.Progress) {
                                showNoProgressErrorState(realBinding.progressError)
                            } else {
                                dataState.refreshStatus.handleProgressErrorState(realBinding.progressError)
                            }
                            realBinding.content.visibility = View.GONE
                        }
                        // we have some data -> lets show it
                        else -> {
                            showNoProgressErrorState(realBinding.progressError)
                            realBinding.content.visibility = View.VISIBLE

                            realBinding.tvTitle.text = item.title

                            if (item.favoriteState.favorite) {
                                realBinding.addToFavorite.visibility = View.GONE
                                realBinding.removeFromFavorite
                                    .apply {
                                        visibility = View.VISIBLE
                                        setOnClickListener {
                                            viewModelInstance.removeFromFavorite(item)
                                        }
                                    }
                            } else {
                                realBinding.removeFromFavorite.visibility = View.GONE
                                realBinding.addToFavorite
                                    .apply {
                                        visibility = View.VISIBLE
                                        setOnClickListener {
                                            viewModelInstance.addToFavorite(item)
                                        }
                                    }
                            }
                        }
                    }

                    realBinding.srlData.isRefreshing = dataState.refreshStatus is UIProgressErrorItem.Progress
                }
            )
    }

    companion object {

        fun createScreen(itemUid: String): Fragment =
            ItemDetailsFragment()
                .apply {
                    arguments = Bundle()
                        .apply { putString("itemUid", itemUid) }
                }
    }
}