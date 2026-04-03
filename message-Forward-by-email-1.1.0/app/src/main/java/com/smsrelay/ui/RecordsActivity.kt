package com.smsrelay.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smsrelay.core.store.SmsRecordStore
import com.smsrelay.databinding.ActivityRecordsBinding
import com.smsrelay.ui.adapter.SmsRecordsAdapter
import kotlinx.coroutines.launch

class RecordsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRecordsBinding
    private lateinit var recordStore: SmsRecordStore
    private lateinit var adapter: SmsRecordsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        recordStore = SmsRecordStore(this)
        setupUI()
        loadRecords()
        loadStatistics()
    }
    
    private fun setupUI() {
        supportActionBar?.title = "短信记录"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        adapter = SmsRecordsAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        binding.swipeRefresh.setOnRefreshListener {
            loadRecords()
            loadStatistics()
        }
        
        binding.btnClearRecords.setOnClickListener {
            clearAllRecords()
        }
    }
    
    private fun loadRecords() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                val records = recordStore.getRecentRecords(200)
                
                adapter.submitList(records)
                
                if (records.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val stats = recordStore.getStatistics()
                
                binding.textStatistics.text = buildString {
                    append("总计: ${stats.totalRecords}")
                    append("  今日: ${stats.todayRecords}")
                    append("  已转发: ${stats.sentRecords}")
                    append("  失败: ${stats.failedRecords}")
                    append("  发送中: ${stats.queuedRecords}")
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                binding.textStatistics.text = "统计数据加载失败"
            }
        }
    }
    
    private fun clearAllRecords() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("清空记录")
            .setMessage("确定要删除所有短信记录吗？此操作不可撤销。")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    recordStore.clearAllRecords()
                    loadRecords()
                    loadStatistics()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onResume() {
        super.onResume()
        loadRecords()
        loadStatistics()
    }
}