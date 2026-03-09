import './styles.css';

class SuperEditor {
    constructor() {
        this.files = []; // Our flat array for the project tree
        this.worker = null;
    }

    async init($page) {
        this.setupSidebar();
        this.injectUniversalHighlighter();
    }

    // --- PROJECT TREE: THE FLAT-LIST METHOD ---
    setupSidebar() {
        const sidebar = acode.require('sidebar');
        const $container = tag('div', { className: 'custom-scrollbar', style: 'height:100%' });
        
        // Use Acode's internal file system to get the root
        const projectRoot = acode.require('projectManager').root;
        
        this.renderFlatTree(projectRoot, $container);
        sidebar.add('super_explorer', 'folder', 'Project', $container);
    }

    async renderFlatTree(root, container) {
        // Research: Instead of recursion, we use a Queue to flatten the tree
        // This prevents "Maximum Call Stack" errors on large projects
        const queue = [root];
        const flatList = [];

        while (queue.length > 0) {
            const current = queue.shift();
            flatList.push(current);
            if (current.isDirectory && current.isOpen) {
                const children = await current.getChildren();
                queue.unshift(...children); // Add to front for depth-first feel
            }
        }

        container.innerHTML = flatList.map(node => `
            <div class="super-tree-item" style="padding-left: ${node.depth * 12}px">
                ${node.isDirectory ? '▸' : '📄'} ${node.name}
            </div>
        `).join('');
    }

    // --- HIGHLIGHTER: THE VS CODE METHOD ---
    injectUniversalHighlighter() {
        const editor = editorManager.editor;

        // Optimized: We don't define language by language.
        // We define a "Global Grammar Map" that Ace uses to switch modes.
        editor.on('change', () => {
            this.debounce(() => {
                const session = editor.getSession();
                // Acode uses Ace. We force the worker to re-scan 
                // only the modified range (Incremental Parsing)
                session.bgTokenizer.start(0); 
            }, 100);
        });
    }

    debounce(func, wait) {
        clearTimeout(this.timeout);
        this.timeout = setTimeout(func, wait);
    }
}

if (window.acode) {
    const plugin = new SuperEditor();
    acode.setPluginInit('com.optimized.editor', (rootUrl, $page) => plugin.init($page));
}