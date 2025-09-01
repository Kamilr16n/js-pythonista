package com.jspythonista.ide

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
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
            allowFileAccess = true
        }
        
        codeEditor.addJavascriptInterface(EditorInterface(), "Android")
        codeEditor.webViewClient = WebViewClient()
        
        // Load CodeMirror editor
        val editorHtml = createEditorHTML()
        codeEditor.loadDataWithBaseURL("file:///android_asset/", editorHtml, "text/html", "UTF-8", null)
        
        // Setup preview WebView
        previewWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
    }
    
    private fun setupFileSystem() {
        fileAdapter = FileAdapter { file -> openFile(file) }
        fileList.layoutManager = LinearLayoutManager(this)
        fileList.adapter = fileAdapter
    }
    
    private fun createDefaultProject() {
        projectDir.mkdirs()
        
        // Create sample files if they don't exist
        val appJs = File(projectDir, "App.js")
        if (!appJs.exists()) {
            appJs.writeText("""
// React Component Example
function App() {
    const [count, setCount] = React.useState(0);
    const [message, setMessage] = React.useState('Hello React!');
    
    return React.createElement('div', {
        style: { 
            padding: '20px', 
            fontFamily: 'Arial, sans-serif',
            maxWidth: '400px',
            margin: '0 auto',
            textAlign: 'center'
        }
    }, [
        React.createElement('h1', { key: 'title' }, message),
        React.createElement('div', { 
            key: 'counter',
            style: { 
                padding: '20px', 
                background: '#f0f0f0', 
                borderRadius: '8px',
                margin: '20px 0'
            }
        }, [
            React.createElement('p', { key: 'count' }, `Counter: ${'$'}{count}`),
            React.createElement('button', {
                key: 'btn',
                onClick: () => setCount(count + 1),
                style: {
                    padding: '10px 20px',
                    background: '#007acc',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    marginRight: '10px'
                }
            }, 'Increment'),
            React.createElement('button', {
                key: 'reset',
                onClick: () => setCount(0),
                style: {
                    padding: '10px 20px',
                    background: '#666',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer'
                }
            }, 'Reset')
        ]),
        React.createElement('input', {
            key: 'input',
            type: 'text',
            value: message,
            onChange: (e) => setMessage(e.target.value),
            placeholder: 'Edit message...',
            style: {
                width: '100%',
                padding: '10px',
                border: '1px solid #ddd',
                borderRadius: '4px',
                fontSize: '16px',
                marginTop: '10px'
            }
        })
    ]);
}

// Render the component
ReactDOM.render(React.createElement(App), document.getElementById('root'));
            """.trimIndent())
        }
        
        val utilsJs = File(projectDir, "utils.js")
        if (!utilsJs.exists()) {
            utilsJs.writeText("""
// Utility Functions
function formatDate(date) {
    return new Date(date).toLocaleDateString();
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function fetchData(url) {
    return fetch(url)
        .then(response => response.json())
        .catch(error => console.error('Error:', error));
}

console.log('Utils loaded successfully!');
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
        codeEditor.evaluateJavascript("setEditorContent(`${'$'}{content.replace("`", "\\`")}`)", null)
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
            
            when {
                code.contains("React.createElement") || code.contains("ReactDOM") -> {
                    runReactCode(code)
                }
                else -> {
                    runJavaScript(code)
                }
            }
        }
    }
    
    private fun runReactCode(code: String) {
        val reactHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script crossorigin src="https://unpkg.com/react@18/umd/react.development.js"></script>
                <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
                <style>
                    body { 
                        margin: 0; 
                        padding: 20px; 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    }
                    #root { min-height: 100vh; }
                </style>
            </head>
            <body>
                <div id="root"></div>
                <script>
                    try {
                        $code
                    } catch (error) {
                        document.getElementById('root').innerHTML = 
                            '<div style="color: red; padding: 20px; border: 1px solid red; border-radius: 4px;">' +
                            '<h3>Error:</h3><pre>' + error.message + '</pre></div>';
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        
        previewWebView.loadDataWithBaseURL("https://localhost/", reactHTML, "text/html", "UTF-8", null)
        showPreview()
        addConsoleOutput("✓ React component rendered in preview")
    }
    
    private fun runJavaScript(code: String) {
        val jsHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { 
                        margin: 0; 
                        padding: 20px; 
                        font-family: monospace;
                        background: #f5f5f5;
                    }
                    #output { 
                        background: white; 
                        padding: 20px; 
                        border-radius: 4px;
                        white-space: pre-wrap;
                    }
                </style>
            </head>
            <body>
                <div id="output"></div>
                <script>
                    const output = document.getElementById('output');
                    const originalLog = console.log;
                    
                    console.log = function(...args) {
                        output.innerHTML += args.join(' ') + '\\n';
                        originalLog.apply(console, args);
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
        findViewById<ScrollView>(R.id.consoleContainer).visibility = android.view.View.VISIBLE
        previewWebView.visibility = android.view.View.GONE
    }
    
    private fun showPreview() {
        findViewById<ScrollView>(R.id.consoleContainer).visibility = android.view.View.GONE
        previewWebView.visibility = android.view.View.VISIBLE
    }
    
    private fun addConsoleOutput(message: String) {
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            consoleOutput.append("[$timestamp] $message\n")
            
            // Auto-scroll to bottom
            findViewById<ScrollView>(R.id.consoleContainer).post {
                findViewById<ScrollView>(R.id.consoleContainer).fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }
    
    private fun createEditorHTML(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.js"></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/javascript/javascript.min.js"></script>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.css">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/theme/monokai.min.css">
                <style>
                    body { margin: 0; padding: 0; }
                    .CodeMirror { 
                        height: 100vh; 
                        font-size: 14px;
                        font-family: 'Consolas', 'Monaco', monospace;
                    }
                </style>
            </head>
            <body>
                <textarea id="editor"></textarea>
                <script>
                    const editor = CodeMirror.fromTextArea(document.getElementById('editor'), {
                        mode: 'javascript',
                        theme: 'monokai',
                        lineNumbers: true,
                        autoCloseBrackets: true,
                        matchBrackets: true,
                        indentUnit: 2,
                        tabSize: 2,
                        lineWrapping: true
                    });
                    
                    function setEditorContent(content) {
                        editor.setValue(content);
                    }
                    
                    function getEditorContent() {
                        return editor.getValue();
                    }
                    
                    // Set initial content
                    editor.setValue('// Welcome to JS Pythonista!\\n// Write JavaScript, TypeScript, or React code here\\n\\nconsole.log("Hello, World!");');
                </script>
            </body>
            </html>
        """.trimIndent()
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
                "json" -> "📋"
                "html" -> "🌐"
                "css" -> "🎨"
                else -> "📄"
            }
            
            itemView.setOnClickListener { onFileClick(file) }
        }
    }
}
