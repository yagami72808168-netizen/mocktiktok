package com.light.mimictiktok.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.light.mimictiktok.data.db.AppDatabase
import com.light.mimictiktok.data.preferences.PreferencesManager
import com.light.mimictiktok.data.repository.LikeRepository
import com.light.mimictiktok.data.repository.VideoRepository
import com.light.mimictiktok.di.AppContainer
import com.light.mimictiktok.player.PlayerManager
import com.light.mimictiktok.player.PlayerPool
import com.light.mimictiktok.util.ListLooper
import com.light.mimictiktok.util.ThumbnailCache
import com.light.mimictiktok.util.ThumbnailGenerator
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private lateinit var playerManager: PlayerManager
    private lateinit var repository: VideoRepository
    private lateinit var likeRepository: LikeRepository
    private lateinit var viewModel: HomeViewModel
    private lateinit var thumbnailGenerator: ThumbnailGenerator
    private lateinit var thumbnailCache: ThumbnailCache
    
    private var currentPosition: Int = -1
    private var isInitialLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return RecyclerView(requireContext()).apply {
            recyclerView = this
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val context = requireContext()
        val appDatabase = AppDatabase.getInstance(context)
        repository = VideoRepository(appDatabase.appDao())
        likeRepository = LikeRepository(appDatabase.likeDao())
        
        val playerPool = PlayerPool(context, poolSize = 2)
        playerManager = PlayerManager(playerPool, repository)
        thumbnailGenerator = ThumbnailGenerator(context)
        thumbnailCache = ThumbnailCache(context)
        
        val preferencesManager = PreferencesManager(context)
        viewModel = HomeViewModel(repository, preferencesManager)
        
        adapter = VideoAdapter(
            playerManager = playerManager,
            thumbnailGenerator = thumbnailGenerator,
            thumbnailCache = thumbnailCache,
            likeRepository = likeRepository
        )
        
        setupRecyclerView()
        observeVideos()
        observeInitialPosition()
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (position != RecyclerView.NO_POSITION && position != currentPosition) {
                        currentPosition = position
                        adapter.playVideoAtPosition(position)
                    }
                }
            }
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val position = layoutManager.findFirstVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION) {
                    val videos = viewModel.videos.value
                    if (videos.isNotEmpty() && ListLooper.needsLoopReset(position, videos.size)) {
                        val direction = if (dy > 0) 1 else -1
                        val targetPosition = ListLooper.calculateSmoothLoopPosition(
                            position, 
                            videos.size, 
                            direction
                        )
                        if (targetPosition != position) {
                            recyclerView.scrollToPosition(targetPosition)
                        }
                    }
                }
            }
        })
    }

    private fun observeVideos() {
        lifecycleScope.launch {
            viewModel.videos.collect { videos ->
                adapter.updateData(videos)
            }
        }
    }

    private fun observeInitialPosition() {
        lifecycleScope.launch {
            viewModel.startPosition.collect { savedPosition ->
                val videos = viewModel.videos.value
                if (videos.isNotEmpty() && isInitialLoad) {
                    isInitialLoad = false
                    val startIndex = savedPosition?.let { position ->
                        if (position < Int.MAX_VALUE) position
                        else ListLooper.calculateStartVirtualPosition(videos.size)
                    } ?: ListLooper.calculateStartVirtualPosition(videos.size)
                    
                    recyclerView.scrollToPosition(startIndex)
                    
                    recyclerView.post {
                        currentPosition = startIndex
                        adapter.playVideoAtPosition(startIndex)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.resumeCurrentVideo()
    }

    override fun onPause() {
        super.onPause()
        adapter.pauseCurrentVideo()
    }

    override fun onDestroyView() {
        playerManager.releaseAll()
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
