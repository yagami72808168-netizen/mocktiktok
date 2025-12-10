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
import com.light.mimictiktok.data.repository.VideoRepository
import com.light.mimictiktok.player.PlayerPool
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private lateinit var playerPool: PlayerPool
    private lateinit var repository: VideoRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return RecyclerView(requireContext()).apply {
            recyclerView = this
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val context = requireContext()
        playerPool = PlayerPool(context, poolSize = 2)
        val appDatabase = AppDatabase.getInstance(context)
        repository = VideoRepository(appDatabase.appDao())
        
        adapter = VideoAdapter(context, playerPool, repository)
        
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        
        lifecycleScope.launch {
            repository.getAllVideosFlow().collect { videos ->
                adapter.updateData(videos)
                if (videos.isNotEmpty()) {
                    val startIndex = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % videos.size)
                    recyclerView.scrollToPosition(startIndex)
                }
            }
        }
    }

    override fun onDestroyView() {
        playerPool.releaseAll()
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
