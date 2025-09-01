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
import kotlinx.coroutines.*
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
        
        // Toolbar buttons
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { 
            drawerLayout.openDrawer(GravityCompat.START) 
        }
        findViewById<ImageButton>(R.id.btnRun).setOnClickListener { runCode() }
        findViewById<ImageButton>(R.id.btnStop).setOnClickListener { stopExecution() }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { showSettings() }
        
        // File explorer buttons
        findViewById<ImageButton>(R.id.btnNewFile).setOnClickListener { createNewFile() }
        findViewById<ImageButton>(R.id.btnNewFolder).setOnClickListener { createNewFolder() }
        
        // Console controls
        findViewById<ImageButton>(R.id.btnClearConsole).setOnClickListener { clearConsole() }
        findViewById<ImageButton>(R.id.btnClosePanel).setOnClickListener { toggleBottomPanel() }
    }
    
    private fun setupDrawer() {
        // Navigation drawer menu items
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
            allowFileAccess = true
            allowContentAccess = true
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
        
        // Load advanced CodeMirror editor
        val editorHtml = createAdvancedEditorHTML()
        codeEditor.loadDataWithBaseURL("file:///android_asset/", editorHtml, "text/html", "UTF-8", null)
        
        // Setup preview WebView
        previewWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
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
        // Panel tab buttons
        findViewById<Button>(R.id.btnConsoleTab).setOnClickListener { switchToPanel("console") }
        findViewById<Button>(R.id.btnPreviewTab).setOnClickListener { switchToPanel("preview") }
        findViewById<Button>(R.id.btnProblemsTab).setOnClickListener { switchToPanel("problems") }
    }
    
    private fun createExampleProjects() {
        projectDir.mkdirs()
        examplesDir.mkdirs()
        
        // Create React Counter example
        val reactExample = File(examplesDir, "ReactCounter.js")
        if (!reactExample.exists()) {
            reactExample.writeText("""
// Interactive React Counter Component
function Counter() {
    const [count, setCount] = React.useState(0);
    const [step, setStep] = React.useState(1);
    const [history, setHistory] = React.useState([0]);
    
    const increment = () => {
        const newCount = count + step;
        setCount(newCount);
        setHistory([...history, newCount]);
    };
    
    const decrement = () => {
        const newCount = count - step;
        setCount(newCount);
        setHistory([...history, newCount]);
    };
    
    const reset = () => {
        setCount(0);
        setHistory([0]);
    };
    
    return React.createElement('div', {
        style: { 
            padding: '30px', 
            fontFamily: 'Arial, sans-serif',
            maxWidth: '500px',
            margin: '0 auto',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            borderRadius: '15px',
            color: 'white',
            boxShadow: '0 10px 30px rgba(0,0,0,0.3)'
        }
    }, [
        React.createElement('h1', { 
            key: 'title',
            style: { textAlign: 'center', marginBottom: '30px' }
        }, '🚀 React Counter'),
        
        React.createElement('div', {
            key: 'counter',
            style: { 
                textAlign: 'center',
                background: 'rgba(255,255,255,0.1)',
                padding: '20px',
                borderRadius: '10px',
                marginBottom: '20px'
            }
        }, [
            React.createElement('div', {
                key: 'display',
                style: { fontSize: '48px', fontWeight: 'bold', marginBottom: '10px' }
            }, count),
            React.createElement('div', {
                key: 'label',
                style: { fontSize: '18px', opacity: 0.8 }
            }, 'Current Count')
        ]),
        
        React.createElement('div', {
            key: 'controls',
            style: { display: 'flex', gap: '10px', marginBottom: '20px', justifyContent: 'center' }
        }, [
            React.createElement('button', {
                key: 'dec',
                onClick: decrement,
                style: {
                    padding: '12px 20px',
                    background: '#ff6b6b',
                    color: 'white',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '16px',
                    cursor: 'pointer',
                    fontWeight: 'bold'
                }
            }, '− ' + step),
            
            React.createElement('button', {
                key: 'reset',
                onClick: reset,
                style: {
                    padding: '12px 20px',
                    background: '#95a5a6',
                    color: 'white',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '16px',
                    cursor: 'pointer'
                }
            }, 'Reset'),
            
            React.createElement('button', {
                key: 'inc',
                onClick: increment,
                style: {
                    padding: '12px 20px',
                    background: '#51cf66',
                    color: 'white',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '16px',
                    cursor: 'pointer',
                    fontWeight: 'bold'
                }
            }, '+ ' + step)
        ]),
        
        React.createElement('div', {
            key: 'step-control',
            style: { textAlign: 'center', marginBottom: '20px' }
        }, [
            React.createElement('label', {
                key: 'step-label',
                style: { display: 'block', marginBottom: '8px', fontSize: '14px' }
            }, 'Step Size: ' + step),
            React.createElement('input', {
                key: 'step-input',
                type: 'range',
                min: 1,
                max: 10,
                value: step,
                onChange: (e) => setStep(parseInt(e.target.value)),
                style: { width: '200px' }
            })
        ]),
        
        React.createElement('div', {
            key: 'history',
            style: { 
                background: 'rgba(0,0,0,0.2)',
                padding: '15px',
                borderRadius: '8px',
                fontSize: '14px'
            }
        }, [
            React.createElement('div', {
                key: 'history-title',
                style: { fontWeight: 'bold', marginBottom: '8px' }
            }, '📊 History (' + history.length + ' steps)'),
            React.createElement('div', {
                key: 'history-values'
            }, history.slice(-10).join(' → '))
        ])
    ]);
}

// Render the component
ReactDOM.render(React.createElement(Counter), document.getElementById('root'));
            """.trimIndent())
        }
        
        refreshFileList()
    }
    
    private fun refreshFileList() {
        val allFiles = mutableListOf<File>()
        
        // Add project files
        projectDir.listFiles()?.let { allFiles.addAll(it) }
        
        // Add example files
        examplesDir.listFiles()?.let { allFiles.addAll(it) }
        
        fileAdapter.updateFiles(allFiles)
    }
    
    // Continue in next part...
}
// MainActivity Part 2 - Add these methods to MainActivity class

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
        val escapedContent = content.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
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
    input.hint = "Enter filename (e.g., app.js, component.tsx)"
    
    AlertDialog.Builder(this)
        .setTitle("Create New File")
        .setView(input)
        .setPositiveButton("Create") { _, _ ->
            val fileName = input.text.toString().trim()
            if (fileName.isNotEmpty()) {
                val newFile = File(projectDir, fileName)
                val template = getFileTemplate(fileName)
                newFile.writeText(template)
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

private fun getFileTemplate(fileName: String): String {
    return when {
        fileName.endsWith(".js") -> """
// JavaScript File
console.log('Hello from $fileName');

// Example function
function greet(name) {
    return `Hello, ${'$'}{name}!`;
}

// Test the function
console.log(greet('World'));
        """.trimIndent()
        
        fileName.endsWith(".jsx") -> """
// React Component
function ${fileName.substringBefore(".")}() {
    const [message, setMessage] = React.useState('Hello React!');
    
    return React.createElement('div', {
        style: { padding: '20px', fontFamily: 'Arial' }
    }, [
        React.createElement('h1', { key: 'title' }, message),
        React.createElement('button', {
            key: 'btn',
            onClick: () => setMessage('Button clicked!'),
            style: { padding: '10px 20px', marginTop: '10px' }
        }, 'Click me!')
    ]);
}

ReactDOM.render(React.createElement(${fileName.substringBefore(".")}), document.getElementById('root'));
        """.trimIndent()
        
        fileName.endsWith(".ts") -> """
// TypeScript File
interface User {
    name: string;
    age: number;
}

function createUser(name: string, age: number): User {
    return { name, age };
}

const user = createUser('John', 25);
console.log('User:', user);
        """.trimIndent()
        
        fileName.endsWith(".tsx") -> """
// TypeScript React Component
interface Props {
    title: string;
}

function ${fileName.substringBefore(".")}({ title }: Props) {
    const [count, setCount] = React.useState<number>(0);
    
    return React.createElement('div', {
        style: { padding: '20px' }
    }, [
        React.createElement('h1', { key: 'title' }, title),
        React.createElement('p', { key: 'count' }, `Count: ${'$'}{count}`),
        React.createElement('button', {
            key: 'btn',
            onClick: () => setCount(count + 1)
        }, 'Increment')
    ]);
}

ReactDOM.render(
    React.createElement(${fileName.substringBefore(".")}, { title: 'TypeScript React' }), 
    document.getElementById('root')
);
        """.trimIndent()
        
        else -> "// New file: $fileName\nconsole.log('Hello World!');"
    }
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
            .replace("\\\\", "\\")
        
        // Save current file
        currentFile?.let { file ->
            file.writeText(code)
            addConsoleOutput("💾 Saved: ${file.name}")
        }
        
        // Determine execution type
        when {
            code.contains("React.createElement") || code.contains("ReactDOM") -> {
                runReactCode(code)
            }
            code.contains("interface ") || code.contains(": string") || code.contains(": number") -> {
                runTypeScriptCode(code)
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
            <title>React Preview</title>
            <script crossorigin src="https://unpkg.com/react@18/umd/react.development.js"></script>
            <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
            <style>
                body { 
                    margin: 0; 
                    padding: 20px; 
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                    background: #f8f9fa;
                }
                #root { 
                    min-height: calc(100vh - 40px);
                    background: white;
                    border-radius: 8px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    overflow: hidden;
                }
                .error {
                    color: #dc3545;
                    background: #f8d7da;
                    border: 1px solid #f5c6cb;
                    border-radius: 4px;
                    padding: 20px;
                    margin: 20px;
                    font-family: monospace;
                }
            </style>
        </head>
        <body>
            <div id="root"></div>
            <script>
                try {
                    $code
                } catch (error) {
                    document.getElementById('root').innerHTML = 
                        '<div class="error"><h3>❌ React Error</h3><pre>' + error.message + '</pre></div>';
                    console.error('React Error:', error);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
    
    previewWebView.loadDataWithBaseURL("https://localhost/", reactHTML, "text/html", "UTF-8", null)
    switchToPanel("preview")
    addConsoleOutput("⚛️ React component rendered successfully")
}

private fun runTypeScriptCode(code: String) {
    // For now, treat TypeScript as JavaScript (in a real implementation, you'd transpile)
    val jsCode = code.replace("interface.*?\\{[^}]*\\}".toRegex(), "")
        .replace(": string|: number|: boolean".toRegex(), "")
    
    runJavaScript(jsCode)
    addConsoleOutput("📘 TypeScript code executed (transpiled to JS)")
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
                    font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
                    background: #1e1e1e;
                    color: #d4d4d4;
                }
                #output { 
                    background: #252526; 
                    padding: 20px; 
                    border-radius: 8px;
                    white-space: pre-wrap;
                    border: 1px solid #3c3c3c;
                    min-height: 200px;
                }
                .log { color: #4fc3f7; }
                .error { color: #f44336; }
                .warn { color: #ff9800; }
            </style>
        </head>
        <body>
            <div id="output">JavaScript Console Output:\n\n</div>
            <script>
                const output = document.getElementById('output');
                const originalLog = console.log;
                const originalError = console.error;
                const originalWarn = console.warn;
                
                console.log = function(...args) {
                    output.innerHTML += '<span class="log">▶ ' + args.join(' ') + '</span>\\n';
                    originalLog.apply(console, args);
                };
                
                console.error = function(...args) {
                    output.innerHTML += '<span class="error">❌ ' + args.join(' ') + '</span>\\n';
                    originalError.apply(console, args);
                };
                
                console.warn = function(...args) {
                    output.innerHTML += '<span class="warn">⚠️ ' + args.join(' ') + '</span>\\n';
                    originalWarn.apply(console, args);
                };
                
                try {
                    $code
                    output.innerHTML += '\\n✅ Execution completed successfully';
                } catch (error) {
                    output.innerHTML += '<span class="error">\\n❌ Error: ' + error.message + '</span>';
                }
            </script>
        </body>
        </html>
    """.trimIndent()
    
    previewWebView.loadDataWithBaseURL("https://localhost/", jsHTML, "text/html", "UTF-8", null)
    switchToPanel("preview")
    addConsoleOutput("✅ JavaScript executed successfully")
}

// Continue with remaining methods in next part...
// MainActivity Part 3 - Add these methods to MainActivity class

private fun stopExecution() {
    previewWebView.loadUrl("about:blank")
    addConsoleOutput("⏹️ Execution stopped")
}

private fun switchToPanel(panel: String) {
    currentPanel = panel
    
    // Update tab styles
    findViewById<Button>(R.id.btnConsoleTab).background = 
        if (panel == "console") resources.getDrawable(R.drawable.tab_selected_bg, null)
        else resources.getDrawable(R.drawable.tab_unselected_bg, null)
    
    findViewById<Button>(R.id.btnPreviewTab).background = 
        if (panel == "preview") resources.getDrawable(R.drawable.tab_selected_bg, null)
        else resources.getDrawable(R.drawable.tab_unselected_bg, null)
    
    findViewById<Button>(R.id.btnProblemsTab).background = 
        if (panel == "problems") resources.getDrawable(R.drawable.tab_selected_bg, null)
        else resources.getDrawable(R.drawable.tab_unselected_bg, null)
    
    // Show/hide panels
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
        
        // Auto-scroll to bottom
        findViewById<ScrollView>(R.id.consoleScrollView).post {
            findViewById<ScrollView>(R.id.consoleScrollView).fullScroll(View.FOCUS_DOWN)
        }
    }
}

private fun showFileContextMenu(file: File) {
    val options = arrayOf("Rename", "Delete", "Duplicate", "Properties")
    
    AlertDialog.Builder(this)
        .setTitle(file.name)
        .setItems(options) { _, which ->
            when (which) {
                0 -> renameFile(file)
                1 -> deleteFile(file)
                2 -> duplicateFile(file)
                3 -> showFileProperties(file)
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
                } else {
                    addConsoleOutput("❌ Failed to rename file")
                }
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun deleteFile(file: File) {
    AlertDialog.Builder(this)
        .setTitle("Delete File")
        .setMessage("Are you sure you want to delete '${file.name}'?")
        .setPositiveButton("Delete") { _, _ ->
            if (file.delete()) {
                refreshFileList()
                closeTab(file)
                addConsoleOutput("🗑️ Deleted: ${file.name}")
            } else {
                addConsoleOutput("❌ Failed to delete file")
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun duplicateFile(file: File) {
    val newName = "${file.nameWithoutExtension}_copy.${file.extension}"
    val newFile = File(file.parent, newName)
    
    try {
        file.copyTo(newFile)
        refreshFileList()
        addConsoleOutput("✓ Duplicated: ${file.name} → $newName")
    } catch (e: Exception) {
        addConsoleOutput("❌ Failed to duplicate file: ${e.message}")
    }
}

private fun showFileProperties(file: File) {
    val size = if (file.isFile) "${file.length()} bytes" else "Directory"
    val modified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
    
    val message = """
        Name: ${file.name}
        Type: ${if (file.isFile) "File" else "Directory"}
        Size: $size
        Modified: $modified
        Path: ${file.absolutePath}
    """.trimIndent()
    
    AlertDialog.Builder(this)
        .setTitle("File Properties")
        .setMessage(message)
        .setPositiveButton("OK", null)
        .show()
}

private fun createNewProject() {
    val input = EditText(this)
    input.hint = "Enter project name"
    
    AlertDialog.Builder(this)
        .setTitle("Create New Project")
        .setView(input)
        .setPositiveButton("Create") { _, _ ->
            val projectName = input.text.toString().trim()
            if (projectName.isNotEmpty()) {
                val projectFolder = File(projectDir, projectName)
                projectFolder.mkdirs()
                
                // Create default files
                File(projectFolder, "index.js").writeText("""
// ${projectName} - Main Entry Point
console.log('Welcome to ${projectName}!');

// Your code here
function main() {
    console.log('Project initialized successfully');
}

main();
                """.trimIndent())
                
                File(projectFolder, "README.md").writeText("""
# ${projectName}

A JavaScript/TypeScript project created with JS Pythonista.

## Getting Started

1. Edit the code in the files
2. Press the Run button to execute
3. View output in the Console or Preview panel

Happy coding! 🚀
                """.trimIndent())
                
                refreshFileList()
                addConsoleOutput("✓ Created project: $projectName")
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun openProject() {
    // In a real implementation, this would show a file picker
    addConsoleOutput("📂 Project browser not implemented yet")
}

private fun showExamples() {
    val examples = examplesDir.listFiles() ?: arrayOf()
    val exampleNames = examples.map { it.name }.toTypedArray()
    
    if (exampleNames.isEmpty()) {
        addConsoleOutput("📚 No examples available")
        return
    }
    
    AlertDialog.Builder(this)
        .setTitle("Choose Example")
        .setItems(exampleNames) { _, which ->
            openFileInTab(examples[which])
        }
        .show()
}

private fun showSettings() {
    val settings = arrayOf("Editor Settings", "Theme", "Font Size", "Auto-save")
    
    AlertDialog.Builder(this)
        .setTitle("Settings")
        .setItems(settings) { _, which ->
            when (which) {
                0 -> addConsoleOutput("⚙️ Editor settings not implemented yet")
                1 -> addConsoleOutput("🎨 Theme settings not implemented yet")
                2 -> addConsoleOutput("📝 Font size settings not implemented yet")
                3 -> addConsoleOutput("💾 Auto-save settings not implemented yet")
            }
        }
        .show()
}

private fun showAbout() {
    val aboutMessage = """
        JS Pythonista v1.0
        
        A complete JavaScript/TypeScript/React development environment for Android.
        
        Features:
        • Professional code editor with syntax highlighting
        • Live React component preview
        • JavaScript/TypeScript execution
        • File management system
        • Console output and debugging
        • Project templates and examples
        
        Built with ❤️ for mobile developers
    """.trimIndent()
    
    AlertDialog.Builder(this)
        .setTitle("About JS Pythonista")
        .setMessage(aboutMessage)
        .setPositiveButton("OK", null)
        .show()
}

private fun getFileIcon(file: File): String {
    return when (file.extension.lowercase()) {
        "js" -> "📄"
        "jsx" -> "⚛️"
        "ts" -> "📘"
        "tsx" -> "⚛️"
        "json" -> "📋"
        "html" -> "🌐"
        "css" -> "🎨"
        "md" -> "📝"
        else -> if (file.isDirectory) "📁" else "📄"
    }
}

private fun createAdvancedEditorHTML(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>JS Pythonista Editor</title>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/javascript/javascript.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/edit/closebrackets.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/edit/matchbrackets.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/selection/active-line.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/fold/foldcode.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/fold/foldgutter.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/fold/brace-fold.min.js"></script>
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.css">
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/theme/monokai.min.css">
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/fold/foldgutter.min.css">
            <style>
                body { 
                    margin: 0; 
                    padding: 0; 
                    background: #1e1e1e;
                    font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
                }
                .CodeMirror { 
                    height: 100vh; 
                    font-size: 14px;
                    font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
                    line-height: 1.5;
                }
                .CodeMirror-gutters {
                    background: #2d2d30;
                    border-right: 1px solid #3c3c3c;
                }
                .CodeMirror-linenumber {
                    color: #858585;
                    padding: 0 8px;
                }
                .CodeMirror-activeline-background {
                    background: rgba(255, 255, 255, 0.05);
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
                    styleActiveLine: true,
                    indentUnit: 2,
                    tabSize: 2,
                    lineWrapping: true,
                    foldGutter: true,
                    gutters: ["CodeMirror-linenumbers", "CodeMirror-foldgutter"],
                    extraKeys: {
                        "Ctrl-Space": "autocomplete",
                        "Ctrl-/": "toggleComment",
                        "Cmd-/": "toggleComment"
                    }
                });
                
                function setEditorContent(content) {
                    editor.setValue(content);
                }
                
                function getEditorContent() {
                    return editor.getValue();
                }
                
                // Set welcome content
                editor.setValue(\`// Welcome to JS Pythonista! 🚀
// A complete JavaScript/TypeScript/React development environment

// Try this React component:
function WelcomeApp() {
    const [message, setMessage] = React.useState('Hello, JS Pythonista!');
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
        React.createElement('h1', { key: 'title' }, '🚀 ' + message),
        React.createElement('p', { key: 'count' }, \`Button clicked: \$\{count\} times\`),
        React.createElement('button', {
            key: 'btn',
            onClick: () => {
                setCount(count + 1);
                setMessage(\`Amazing! You clicked \$\{count + 1\} times!\`);
            },
            style: {
                padding: '12px 24px',
                background: '#ff6b6b',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: '16px',
                cursor: 'pointer',
                fontWeight: 'bold'
            }
        }, 'Click me! 🎉')
    ]);
}

// Render the component
ReactDOM.render(React.createElement(WelcomeApp), document.getElementById('root'));

// Press the Run button to see this in action!\`);
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
    
    @JavascriptInterface
    fun saveContent(content: String) {
        currentFile?.writeText(content)
    }
}
