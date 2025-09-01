package com.jspythonista.ide

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : Activity() {
    
    private lateinit var codeEditor: WebView
    private lateinit var previewWebView: WebView
    private lateinit var consoleOutput: TextView
    private lateinit var fileList: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    
    private var currentFile: File? = null
    private val projectDir by lazy { File(filesDir, "projects") }
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupEditor()
        setupFileSystem()
        createDefaultProject()
    }
    
    private fun initViews() {
        codeEditor = findViewById(R.id.codeEditor)
        previewWebView = findViewById(R.id.previewWebView)
        consoleOutput = findViewById(R.id.consoleOutput)
        fileList = findViewById(R.id.fileList)
        
        findViewById<Button>(R.id.btnRun).setOnClickListener { runCode() }
        findViewById<Button>(R.id.btnNew).setOnClickListener { createNewFile() }
        findViewById<Button>(R.id.btnConsoleTab).setOnClickListener { showConsole() }
        findViewById<Button>(R.id.btnPreviewTab).setOnClickListener { showPreview() }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupEditor() {
        codeEditor.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        
        codeEditor.addJavascriptInterface(EditorInterface(), "Android")
        codeEditor.webViewClient = WebViewClient()
        
        // Load simple editor
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
                <textarea id="editor" placeholder="// Welcome to JS Pythonista!
// Write JavaScript, TypeScript, or React code here

console.log('Hello, World!');">// Welcome to JS Pythonista!
// Write JavaScript, TypeScript, or React code here

console.log('Hello, World!');</textarea>
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
        
        // Setup preview WebView
        previewWebView.settings.javaScriptEnabled = true
    }
    
    private fun setupFileSystem() {
        fileAdapter = FileAdapter { file -> openFile(file) }
        fileList.layoutManager = LinearLayoutManager(this)
        fileList.adapter = fileAdapter
    }
    
    private fun createDefaultProject() {
        projectDir.mkdirs()
        
        // Create sample React component
        val appJs = File(projectDir, "App.js")
        if (!appJs.exists()) {
            appJs.writeText("""
// React Component Example
function App() {
    const [count, setCount] = React.useState(0);
    
    return React.createElement('div', {
        style: { 
            padding: '20px', 
            fontFamily: 'Arial, sans-serif',
            textAlign: 'center'
        }
    }, [
        React.createElement('h1', { key: 'title' }, 'Hello React!'),
        React.createElement('p', { key: 'count' }, 'Counter: ' + count),
        React.createElement('button', {
            key: 'btn',
            onClick: () => setCount(count + 1),
            style: {
                padding: '10px 20px',
                background: '#007acc',
                color: 'white',
                border: 'none',
                borderRadius: '4px'
            }
        }, 'Click me!')
    ]);
}

ReactDOM.render(React.createElement(App), document.getElementById('root'));
            """.trimIndent())
        }
        
        // Create utility file
        val utilsJs = File(projectDir, "utils.js")
        if (!utilsJs.exists()) {
            utilsJs.writeText("""
// Utility Functions
function greet(name) {
    return 'Hello, ' + name + '!';
}

function add(a, b) {
    return a + b;
}

console.log('Utils loaded!');
console.log(greet('World'));
console.log('2 + 3 =', add(2, 3));
            """.trimIndent())
        }
        
        refreshFileList()
    }
    
    private fun refreshFileList() {
        val files = projectDir.listFiles()?.toList() ?: emptyList()
        fileAdapter.updateFiles(files)
    }
    
    private fun openFile(file: File) {
        currentFile = file
        val content = file.readText()
        codeEditor.evaluateJavascript("setEditorContent(`${content.replace("`", "\\`")}`)", null)
        addConsoleOutput("Opened: ${file.name}")
    }
    
    private fun createNewFile() {
        val fileName = "new_file_${System.currentTimeMillis()}.js"
        val newFile = File(projectDir, fileName)
        newFile.writeText("// New JavaScript file\nconsole.log('Hello from $fileName');")
        refreshFileList()
        openFile(newFile)
    }
    
    private fun runCode() {
        codeEditor.evaluateJavascript("getEditorContent()") { content ->
            val code = content.removeSurrounding("\"").replace("\\n", "\n").replace("\\\"", "\"")
            
            currentFile?.let { file ->
                file.writeText(code)
                addConsoleOutput("Saved: ${file.name}")
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
        showPreview()
        addConsoleOutput("✓ React component rendered")
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
        showPreview()
        addConsoleOutput("✓ JavaScript executed")
    }
    
    private fun showConsole() {
        findViewById<ScrollView>(R.id.consoleContainer).visibility = View.VISIBLE
        previewWebView.visibility = View.GONE
    }
    
    private fun showPreview() {
        findViewById<ScrollView>(R.id.consoleContainer).visibility = View.GONE
        previewWebView.visibility = View.VISIBLE
    }
    
    private fun addConsoleOutput(message: String) {
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            consoleOutput.append("[$timestamp] $message\n")
        }
    }
    
    inner class EditorInterface {
        @JavascriptInterface
        fun getContent(): String {
            return currentFile?.readText() ?: ""
        }
    }
}

class FileAdapter(private val onFileClick: (File) -> Unit) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
    
    private var files = listOf<File>()
    
    fun updateFiles(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FileViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }
    
    override fun getItemCount() = files.size
    
    inner class FileViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val fileIcon: TextView = itemView.findViewById(R.id.fileIcon)
        private val fileName: TextView = itemView.findViewById(R.id.fileName)
        
        fun bind(file: File) {
            fileName.text = file.name
            fileIcon.text = when (file.extension) {
                "js" -> "📄"
                "jsx" -> "⚛️"
                "ts" -> "📘"
                "tsx" -> "⚛️"
                else -> "📄"
            }
            
            itemView.setOnClickListener { onFileClick(file) }
        }
    }
}
