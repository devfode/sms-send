package com.smsrelay.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smsrelay.core.model.SmsRecord
import com.smsrelay.databinding.ItemSmsRecordBinding

class SmsRecordsAdapter : ListAdapter<SmsRecord, SmsRecordsAdapter.RecordViewHolder>(RecordDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemSmsRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecordViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class RecordViewHolder(private val binding: ItemSmsRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(record: SmsRecord) {
            binding.textFrom.text = record.from
            binding.textTime.text = record.getFormattedTime()
            binding.textStatus.text = record.getStatusText()
            binding.textStatus.setTextColor(record.getStatusColor())
            
            // 显示处理后的短信内容（脱敏后的）
            binding.textBody.text = if (record.body.length > 100) {
                record.body.take(100) + "..."
            } else {
                record.body
            }
            
            // 显示失败原因或过滤原因
            if (record.reason != null) {
                binding.textReason.text = "原因: ${record.reason}"
                binding.textReason.visibility = android.view.View.VISIBLE
            } else {
                binding.textReason.visibility = android.view.View.GONE
            }
            
            // 显示邮件发送错误
            if (record.emailError != null) {
                binding.textError.text = "错误: ${record.emailError}"
                binding.textError.visibility = android.view.View.VISIBLE
            } else {
                binding.textError.visibility = android.view.View.GONE
            }
            
            // 点击展开详细信息
            binding.root.setOnClickListener {
                // TODO: 可以添加详情页面或展开/收起功能
            }
        }
    }
    
    private class RecordDiffCallback : DiffUtil.ItemCallback<SmsRecord>() {
        override fun areItemsTheSame(oldItem: SmsRecord, newItem: SmsRecord): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: SmsRecord, newItem: SmsRecord): Boolean {
            return oldItem == newItem
        }
    }
}