package com.jspythonista.ide

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var codeEditor: WebView
    private lateinit var previewWebView: WebView
    private lateinit var consoleOutput: TextView
    private lateinit var fileList: RecyclerView
    private lateinit var tabContainer: LinearLayout
    private lateinit var fileAdapter: FileAdapter
    private lateinit var loadingOverlay: LinearLayout
    
    private var currentFile: File? = null
    private val openTabs = mutableListOf<File>()
    private val projectDir by lazy { File(filesDir, "projects") }
    private val examplesDir by lazy { File(filesDir, "examples") }
    
    private var isEditorReady = false
    private var currentPanel = "console"
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupDrawer()
        setupEditor()
        setupFileSystem()
        createExampleProjects()
        setupBottomPanel()
    }
    
    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        codeEditor = findViewById(R.id.codeEditor)
        previewWebView = findViewById(R.id.previewWebView)
        consoleOutput = findViewById(R.id.consoleOutput)
        fileList = findViewById(R.id.fileList)
        tabContainer = findViewById(R.id.tabContainer)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { 
            drawerLayout.openDrawer(GravityCompat.START) 
        }
        findViewById<ImageButton>(R.id.btnRun).setOnClickListener { runCode() }
        findViewById<ImageButton>(R.id.btnStop).setOnClickListener { stopExecution() }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { showSettings() }
        
        findViewById<ImageButton>(R.id.btnNewFile).setOnClickListener { createNewFile() }
        findViewById<ImageButton>(R.id.btnNewFolder).setOnClickListener { createNewFolder() }
        
        findViewById<ImageButton>(R.id.btnClearConsole).setOnClickListener { clearConsole() }
        findViewById<ImageButton>(R.id.btnClosePanel).setOnClickListener { toggleBottomPanel() }
    }
    
    private fun setupDrawer() {
        findViewById<TextView>(R.id.menuNewProject).setOnClickListener { 
            drawerLayout.closeDrawers()
            createNewProject()
        }
        findViewById<TextView>(R.id.menuOpenProject).setOnClickListener { 
            drawerLayout.closeDrawers()
            openProject()
        }
        findViewById<TextView>(R.id.menuExamples).setOnClickListener { 
            drawerLayout.closeDrawers()
            showExamples()
        }
        findViewById<TextView>(R.id.menuSettings).setOnClickListener { 
            drawerLayout.closeDrawers()
            showSettings()
        }
        findViewById<TextView>(R.id.menuAbout).setOnClickListener { 
            drawerLayout.closeDrawers()
            showAbout()
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupEditor() {
        loadingOverlay.visibility = View.VISIBLE
        
        codeEditor.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        
        codeEditor.addJavascriptInterface(EditorInterface(), "Android")
        codeEditor.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isEditorReady = true
                loadingOverlay.visibility = View.GONE
                addConsoleOutput("✓ Editor ready")
            }
        }
        
        val editorHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { 
                        margin: 0; 
                        padding: 0; 
                        background: #1e1e1e; 
                        color: #d4d4d4;
                        font-family: 'Consolas', monospace;
                    }
                    textarea { 
                        width: 100%; 
                        height: 100vh; 
                        background: #1e1e1e; 
                        color: #d4d4d4; 
                        border: none; 
                        padding: 10px;
                        font-family: 'Consolas', monospace;
                        font-size: 14px;
                        resize: none;
                        outline: none;
                    }
                </style>
            </head>
            <body>
                <textarea id="editor" placeholder="// Welcome to JS Pythonista!">// Welcome to JS Pythonista! 🚀
// Complete JavaScript/TypeScript/React IDE

// Try this React component:
function WelcomeApp() {
    const [count, setCount] = React.useState(0);
    
    return React.createElement('div', {
        style: { 
            padding: '30px', 
            fontFamily: 'Arial, sans-serif',
            textAlign: 'center',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            borderRadius: '15px',
            maxWidth: '500px',
            margin: '20px auto'
        }
    }, [
        React.createElement('h1', { key: 'title' }, '🚀 JS Pythonista'),
        React.createElement('p', { key: 'count' }, 'Clicked: ' + count + ' times'),
        React.createElement('button', {
            key: 'btn',
            onClick: () => setCount(count + 1),
            style: {
                padding: '12px 24px',
                background: '#ff6b6b',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: '16px',
                cursor: 'pointer'
            }
        }, 'Click me! 🎉')
    ]);
}

ReactDOM.render(React.createElement(WelcomeApp), document.getElementById('root'));</textarea>
                <script>
                    function setEditorContent(content) {
                        document.getElementById('editor').value = content;
                    }
                    
                    function getEditorContent() {
                        return document.getElementById('editor').value;
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        
        codeEditor.loadDataWithBaseURL("file:///android_asset/", editorHtml, "text/html", "UTF-8", null)
        previewWebView.settings.javaScriptEnabled = true
    }
    
    private fun setupFileSystem() {
        fileAdapter = FileAdapter(
            onFileClick = { file -> openFileInTab(file) },
            onFileLongClick = { file -> showFileContextMenu(file) }
        )
        fileList.layoutManager = LinearLayoutManager(this)
        fileList.adapter = fileAdapter
    }
    
    private fun setupBottomPanel() {
        findViewById<Button>(R.id.btnConsoleTab).setOnClickListener { switchToPanel("console") }
        findViewById<Button>(R.id.btnPreviewTab).setOnClickListener { switchToPanel("preview") }
        findViewById<Button>(R.id.btnProblemsTab).setOnClickListener { switchToPanel("problems") }
    }
    
    private fun createExampleProjects() {
        projectDir.mkdirs()
        examplesDir.mkdirs()
        
        val reactExample = File(examplesDir, "ReactCounter.js")
        if (!reactExample.exists()) {
            reactExample.writeText("""
// Interactive React Counter
function Counter() {
    const [count, setCount] = React.useState(0);
    
    return React.createElement('div', {
        style: { 
            padding: '30px', 
            fontFamily: 'Arial, sans-serif',
            textAlign: 'center',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            borderRadius: '15px',
            maxWidth: '500px',
            margin: '20px auto'
        }
    }, [
        React.createElement('h1', { key: 'title' }, '🚀 React Counter'),
        React.createElement('p', { key: 'count' }, 'Count: ' + count),
        React.createElement('button', {
            key: 'inc',
            onClick: () => setCount(count + 1),
            style: {
                padding: '12px 20px',
                background: '#51cf66',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                margin: '5px'
            }
        }, '+ 1'),
        React.createElement('button', {
            key: 'dec',
            onClick: () => setCount(count - 1),
            style: {
                padding: '12px 20px',
                background: '#ff6b6b',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                margin: '5px'
            }
        }, '- 1'),
        React.createElement('button', {
            key: 'reset',
            onClick: () => setCount(0),
            style: {
                padding: '12px 20px',
                background: '#95a5a6',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                margin: '5px'
            }
        }, 'Reset')
    ]);
}

ReactDOM.render(React.createElement(Counter), document.getElementById('root'));
            """.trimIndent())
        }
        
        refreshFileList()
    }
    
    private fun refreshFileList() {
        val allFiles = mutableListOf<File>()
        projectDir.listFiles()?.let { allFiles.addAll(it) }
        examplesDir.listFiles()?.let { allFiles.addAll(it) }
        fileAdapter.updateFiles(allFiles)
    }
    
    private fun openFileInTab(file: File) {
        if (!openTabs.contains(file)) {
            openTabs.add(file)
            addTab(file)
        }
        switchToFile(file)
    }
    
    private fun addTab(file: File) {
        val tabView = layoutInflater.inflate(R.layout.item_tab, tabContainer, false)
        val tabIcon = tabView.findViewById<TextView>(R.id.tabIcon)
        val tabName = tabView.findViewById<TextView>(R.id.tabName)
        val closeBtn = tabView.findViewById<ImageButton>(R.id.btnCloseTab)
        
        tabIcon.text = getFileIcon(file)
        tabName.text = file.name
        
        tabView.setOnClickListener { switchToFile(file) }
        closeBtn.setOnClickListener { closeTab(file) }
        
        tabContainer.addView(tabView)
        updateTabStyles()
    }
    
    private fun closeTab(file: File) {
        val index = openTabs.indexOf(file)
        if (index >= 0) {
            openTabs.removeAt(index)
            tabContainer.removeViewAt(index)
            
            if (currentFile == file) {
                if (openTabs.isNotEmpty()) {
                    switchToFile(openTabs.last())
                } else {
                    currentFile = null
                    if (isEditorReady) {
                        codeEditor.evaluateJavascript("setEditorContent('')", null)
                    }
                }
            }
            updateTabStyles()
        }
    }
    
    private fun switchToFile(file: File) {
        currentFile = file
        val content = file.readText()
        
        if (isEditorReady) {
            val escapedContent = content.replace("\\", "\\\\").replace("`", "\\`")
            codeEditor.evaluateJavascript("setEditorContent(`$escapedContent`)", null)
        }
        
        updateTabStyles()
        addConsoleOutput("📂 Opened: ${file.name}")
    }
    
    private fun updateTabStyles() {
        for (i in 0 until tabContainer.childCount) {
            val tabView = tabContainer.getChildAt(i)
            val isActive = i < openTabs.size && openTabs[i] == currentFile
            
            tabView.background = if (isActive) {
                resources.getDrawable(R.drawable.tab_selected_bg, null)
            } else {
                resources.getDrawable(R.drawable.tab_unselected_bg, null)
            }
        }
    }
    
    private fun createNewFile() {
        val input = EditText(this)
        input.hint = "Enter filename (e.g., app.js)"
        
        AlertDialog.Builder(this)
            .setTitle("Create New File")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    val newFile = File(projectDir, fileName)
                    newFile.writeText("// New file: $fileName\nconsole.log('Hello World!');")
                    refreshFileList()
                    openFileInTab(newFile)
                    addConsoleOutput("✓ Created: $fileName")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createNewFolder() {
        val input = EditText(this)
        input.hint = "Enter folder name"
        
        AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val folderName = input.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    val newFolder = File(projectDir, folderName)
                    newFolder.mkdirs()
                    refreshFileList()
                    addConsoleOutput("✓ Created folder: $folderName")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun runCode() {
        if (!isEditorReady) {
            addConsoleOutput("❌ Editor not ready")
            return
        }
        
        addConsoleOutput("🚀 Running code...")
        
        codeEditor.evaluateJavascript("getEditorContent()") { content ->
            val code = content.removeSurrounding("\"")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
            
            currentFile?.let { file ->
                file.writeText(code)
                addConsoleOutput("💾 Saved: ${file.name}")
            }
            
            if (code.contains("React.createElement") || code.contains("ReactDOM")) {
                runReactCode(code)
            } else {
                runJavaScript(code)
            }
        }
    }
    
    private fun runReactCode(code: String) {
        val reactHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <script crossorigin src="https://unpkg.com/react@18/umd/react.development.js"></script>
                <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
                <style>
                    body { margin: 0; padding: 20px; font-family: Arial, sans-serif; }
                </style>
            </head>
            <body>
                <div id="root"></div>
                <script>
                    try {
                        $code
                    } catch (error) {
                        document.getElementById('root').innerHTML = 
                            '<div style="color: red; padding: 20px;">Error: ' + error.message + '</div>';
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        
        previewWebView.loadDataWithBaseURL("https://localhost/", reactHTML, "text/html", "UTF-8", null)
        switchToPanel("preview")
        addConsoleOutput("⚛️ React component rendered")
    }
    
    private fun runJavaScript(code: String) {
        val jsHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { margin: 0; padding: 20px; font-family: monospace; background: #f5f5f5; }
                    #output { background: white; padding: 20px; border-radius: 4px; white-space: pre-wrap; }
                </style>
            </head>
            <body>
                <div id="output"></div>
                <script>
                    const output = document.getElementById('output');
                    const originalLog = console.log;
                    
                    console.log = function(...args) {
                        output.innerHTML += args.join(' ') + '\\n';
                    };
                    
                    try {
                        $code
                    } catch (error) {
                        output.innerHTML += 'Error: ' + error.message + '\\n';
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        
        previewWebView.loadDataWithBaseURL("https://localhost/", jsHTML, "text/html", "UTF-8", null)
        switchToPanel("preview")
        addConsoleOutput("✅ JavaScript executed")
    }
    
    private fun stopExecution() {
        previewWebView.loadUrl("about:blank")
        addConsoleOutput("⏹️ Execution stopped")
    }
    
    private fun switchToPanel(panel: String) {
        currentPanel = panel
        
        findViewById<Button>(R.id.btnConsoleTab).background = 
            if (panel == "console") resources.getDrawable(R.drawable.tab_selected_bg, null)
            else resources.getDrawable(R.drawable.tab_unselected_bg, null)
        
        findViewById<Button>(R.id.btnPreviewTab).background = 
            if (panel == "preview") resources.getDrawable(R.drawable.tab_selected_bg, null)
            else resources.getDrawable(R.drawable.tab_unselected_bg, null)
        
        findViewById<Button>(R.id.btnProblemsTab).background = 
            if (panel == "problems") resources.getDrawable(R.drawable.tab_selected_bg, null)
            else resources.getDrawable(R.drawable.tab_unselected_bg, null)
        
        findViewById<LinearLayout>(R.id.consolePanel).visibility = 
            if (panel == "console") View.VISIBLE else View.GONE
        
        previewWebView.visibility = 
            if (panel == "preview") View.VISIBLE else View.GONE
        
        findViewById<LinearLayout>(R.id.problemsPanel).visibility = 
            if (panel == "problems") View.VISIBLE else View.GONE
    }
    
    private fun toggleBottomPanel() {
        val bottomPanel = findViewById<LinearLayout>(R.id.bottomPanel)
        bottomPanel.visibility = if (bottomPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
    
    private fun clearConsole() {
        consoleOutput.text = "JS Pythonista Console v1.0\nReady for JavaScript/TypeScript/React development\n\n"
    }
    
    private fun addConsoleOutput(message: String) {
        runOnUiThread {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            consoleOutput.append("[$timestamp] $message\n")
            
            findViewById<ScrollView>(R.id.consoleScrollView).post {
                findViewById<ScrollView>(R.id.consoleScrollView).fullScroll(View.FOCUS_DOWN)
            }
        }
    }
    
    private fun showFileContextMenu(file: File) {
        val options = arrayOf("Rename", "Delete", "Properties")
        
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> renameFile(file)
                    1 -> deleteFile(file)
                    2 -> showFileProperties(file)
                }
            }
            .show()
    }
    
    private fun renameFile(file: File) {
        val input = EditText(this)
        input.setText(file.name)
        
        AlertDialog.Builder(this)
            .setTitle("Rename File")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != file.name) {
                    val newFile = File(file.parent, newName)
                    if (file.renameTo(newFile)) {
                        refreshFileList()
                        addConsoleOutput("✓ Renamed: ${file.name} → $newName")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteFile(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Delete '${file.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                if (file.delete()) {
                    refreshFileList()
                    closeTab(file)
                    addConsoleOutput("🗑️ Deleted: ${file.name}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showFileProperties(file: File) {
        val size = if (file.isFile) "${file.length()} bytes" else "Directory"
        val modified = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
        
        AlertDialog.Builder(this)
            .setTitle("Properties")
            .setMessage("Name: ${file.name}\nSize: $size\nModified: $modified")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun createNewProject() {
        addConsoleOutput("📁 New project feature coming soon")
    }
    
    private fun openProject() {
        addConsoleOutput("📂 Open project feature coming soon")
    }
    
    private fun showExamples() {
        val examples = examplesDir.listFiles() ?: arrayOf()
        if (examples.isEmpty()) {
            addConsoleOutput("📚 No examples available")
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Examples")
            .setItems(examples.map { it.name }.toTypedArray()) { _, which ->
                openFileInTab(examples[which])
            }
            .show()
    }
    
    private fun showSettings() {
        addConsoleOutput("⚙️ Settings feature coming soon")
    }
    
    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("About JS Pythonista")
            .setMessage("JS Pythonista v1.0\n\nComplete JavaScript/TypeScript/React IDE for Android\n\nBuilt with ❤️")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun getFileIcon(file: File): String {
        return when {
            file.isDirectory -> "📁"
            file.extension.lowercase() == "js" -> "📄"
            file.extension.lowercase() == "jsx" -> "⚛️"
            file.extension.lowercase() == "ts" -> "📘"
            file.extension.lowercase() == "tsx" -> "⚛️"
            else -> "📄"
        }
    }
    
    inner class EditorInterface {
        @JavascriptInterface
        fun getContent(): String {
            return currentFile?.readText() ?: ""
        }
    }
}
