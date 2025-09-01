package com.jspythonista.ide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val onFileClick: (File) -> Unit,
    private val onFileLongClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
    
    private var files = listOf<File>()
    
    fun updateFiles(newFiles: List<File>) {
        files = newFiles.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }
    
    override fun getItemCount() = files.size
    
    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileIcon: TextView = itemView.findViewById(R.id.fileIcon)
        private val fileName: TextView = itemView.findViewById(R.id.fileName)
        private val fileSize: TextView = itemView.findViewById(R.id.fileSize)
        private val fileMenu: ImageButton = itemView.findViewById(R.id.btnFileMenu)
        
        fun bind(file: File) {
            fileName.text = file.name
            fileIcon.text = getFileIcon(file)
            
            // Show file size for files
            if (file.isFile) {
                fileSize.visibility = View.VISIBLE
                fileSize.text = formatFileSize(file.length())
            } else {
                fileSize.visibility = View.GONE
            }
            
            // Set click listeners
            itemView.setOnClickListener { onFileClick(file) }
            itemView.setOnLongClickListener { 
                onFileLongClick(file)
                true
            }
            
            // File menu button
            fileMenu.visibility = View.VISIBLE
            fileMenu.setOnClickListener { onFileLongClick(file) }
            
            // Different styling for directories
            if (file.isDirectory) {
                fileName.setTextColor(itemView.context.getColor(R.color.ide_accent))
            } else {
                fileName.setTextColor(itemView.context.getColor(R.color.ide_text_bright))
            }
        }
        
        private fun getFileIcon(file: File): String {
            return when {
                file.isDirectory -> "📁"
                file.extension.lowercase() == "js" -> "📄"
                file.extension.lowercase() == "jsx" -> "⚛️"
                file.extension.lowercase() == "ts" -> "📘"
                file.extension.lowercase() == "tsx" -> "⚛️"
                file.extension.lowercase() == "json" -> "📋"
                file.extension.lowercase() == "html" -> "🌐"
                file.extension.lowercase() == "css" -> "🎨"
                file.extension.lowercase() == "md" -> "📝"
                file.extension.lowercase() == "txt" -> "📃"
                file.extension.lowercase() in listOf("png", "jpg", "jpeg", "gif", "svg") -> "🖼️"
                else -> "📄"
            }
        }
        
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> "${bytes / (1024 * 1024)} MB"
            }
        }
    }
}
