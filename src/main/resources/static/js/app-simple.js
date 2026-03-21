const API_BASE = '/api/admin';

let currentTab = 'datasources';
let datasources = [];
let chats = [];
let tasks = [];
let editingDatasourceId = null;
let editingTaskId = null;

document.addEventListener('DOMContentLoaded', function() {
    initTabs();
    initModals();
    loadDatasources();
    loadChats();
    loadTasks();
});

function initTabs() {
    document.getElementById('tab-datasources').addEventListener('click', () => switchTab('datasources'));
    document.getElementById('tab-chats').addEventListener('click', () => switchTab('chats'));
    document.getElementById('tab-tasks').addEventListener('click', () => switchTab('tasks'));
}

function getDatasourceName(groupId) {
    const datasource = datasources.find(ds => ds.groupId === groupId);
    return datasource ? datasource.groupName : '未匹配数据源';
}

function initModals() {
    document.getElementById('btn-add-datasource').addEventListener('click', () => openDatasourceModal());
    document.getElementById('btn-add-task').addEventListener('click', () => openTaskModal());
    document.getElementById('btn-refresh-chats').addEventListener('click', () => loadChats());
    document.getElementById('btn-search-chats').addEventListener('click', () => loadChats());
    document.getElementById('btn-clear-chat-search').addEventListener('click', () => {
        document.getElementById('chat-keyword').value = '';
        loadChats();
    });
    document.getElementById('chat-keyword').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            loadChats();
        }
    });
    document.getElementById('btn-pick-datasource-chat').addEventListener('click', () => toggleChatPicker('datasource'));
    document.getElementById('btn-pick-task-chat').addEventListener('click', () => toggleChatPicker('task'));
    document.getElementById('btn-search-datasource-chat').addEventListener('click', () => searchModalChats('datasource'));
    document.getElementById('btn-search-task-chat').addEventListener('click', () => searchModalChats('task'));
    document.getElementById('datasource-chat-keyword').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            searchModalChats('datasource');
        }
    });
    document.getElementById('task-chat-keyword').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            searchModalChats('task');
        }
    });
    
    document.getElementById('close-datasource-modal').addEventListener('click', () => closeDatasourceModal());
    document.getElementById('cancel-datasource-modal').addEventListener('click', () => closeDatasourceModal());
    document.getElementById('close-task-modal').addEventListener('click', () => closeTaskModal());
    document.getElementById('cancel-task-modal').addEventListener('click', () => closeTaskModal());
    
    document.getElementById('datasource-form').addEventListener('submit', handleDatasourceSubmit);
    document.getElementById('task-form').addEventListener('submit', handleTaskSubmit);
    
    document.getElementById('datasource-modal').addEventListener('click', (e) => {
        if (e.target.id === 'datasource-modal') closeDatasourceModal();
    });
    document.getElementById('task-modal').addEventListener('click', (e) => {
        if (e.target.id === 'task-modal') closeTaskModal();
    });
}

function switchTab(tab) {
    currentTab = tab;
    document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active'));
    document.getElementById(`tab-${tab}`).classList.add('active');
    document.querySelectorAll('.content-section').forEach(sec => sec.style.display = 'none');
    document.getElementById(`content-${tab}`).style.display = 'block';
}

async function apiGet(endpoint) {
    try {
        const res = await fetch(`${API_BASE}${endpoint}`);
        if (!res.ok) throw new Error('请求失败');
        return await res.json();
    } catch (e) {
        console.error('接口请求异常:', e);
        return [];
    }
}

async function apiPost(endpoint, data) {
    try {
        const res = await fetch(`${API_BASE}${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error('请求失败');
        return res;
    } catch (e) {
        console.error('接口请求异常:', e);
        throw e;
    }
}

async function apiPut(endpoint, data) {
    try {
        const res = await fetch(`${API_BASE}${endpoint}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error('请求失败');
        return res;
    } catch (e) {
        console.error('接口请求异常:', e);
        throw e;
    }
}

async function apiDelete(endpoint) {
    try {
        const res = await fetch(`${API_BASE}${endpoint}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('请求失败');
        return res;
    } catch (e) {
        console.error('接口请求异常:', e);
        throw e;
    }
}

async function feishuGet(endpoint) {
    try {
        const res = await fetch(endpoint);
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || '请求失败');
        return data;
    } catch (e) {
        console.error('飞书接口请求异常:', e);
        throw e;
    }
}

async function loadDatasources() {
    const container = document.getElementById('datasources-table-container');
    datasources = await apiGet('/datasources');
    
    if (datasources.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>暂无数据源，请点击“新增数据源”开始配置。</p></div>';
        return;
    }
    
    let html = '<table><thead><tr><th>群聊 ID</th><th>数据源名称</th><th>Redis 地址</th><th>数据库</th><th>操作</th></tr></thead><tbody>';
    datasources.forEach(ds => {
        html += `<tr>
            <td><code>${ds.chatId || '-'}</code></td>
            <td>${ds.groupName}</td>
            <td>${ds.redisHost}:${ds.redisPort}</td>
            <td>${ds.redisDatabase}</td>
            <td><div class="actions">
                <button class="btn btn-success btn-sm" onclick="testDatasource('${ds.groupId}')">测试</button>
                <button class="btn btn-secondary btn-sm" onclick="editDatasource(${ds.id})">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="deleteDatasource(${ds.id})">删除</button>
            </div></td>
        </tr>`;
    });
    html += '</tbody></table>';
    container.innerHTML = html;
    updateGroupIdSelect();
}

async function loadTasks() {
    const container = document.getElementById('tasks-table-container');
    tasks = await apiGet('/tasks');
    
    if (tasks.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>暂无定时任务，请点击“新增任务”开始配置。</p></div>';
        return;
    }
    
    let html = '<table><thead><tr><th>任务名称</th><th>数据源</th><th>类型</th><th>Cron</th><th>状态</th><th>上次运行</th><th>操作</th></tr></thead><tbody>';
    tasks.forEach(task => {
        const typeLabel = {slow_query: '慢查询', big_key: '大 Key', all: '全部'}[task.taskType] || task.taskType;
        const statusBadge = task.enabled === 1 
            ? '<span class="badge badge-success">启用</span>' 
            : '<span class="badge badge-danger">禁用</span>';
        const lastRun = task.lastRunTime ? new Date(task.lastRunTime).toLocaleString() : '-';
        
        html += `<tr>
            <td><strong>${task.taskName}</strong></td>
            <td>${getDatasourceName(task.groupId)}</td>
            <td>${typeLabel}</td>
            <td><code>${task.cronExpression}</code></td>
            <td>${statusBadge}</td>
            <td>${lastRun}</td>
            <td><div class="actions">
                <button class="btn btn-success btn-sm" onclick="runTask(${task.id})">执行</button>
                <button class="btn btn-warning btn-sm" onclick="toggleTask(${task.id}, ${task.enabled === 1 ? 0 : 1})">${task.enabled === 1 ? '禁用' : '启用'}</button>
                <button class="btn btn-secondary btn-sm" onclick="editTask(${task.id})">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="deleteTask(${task.id})">删除</button>
            </div></td>
        </tr>`;
    });
    html += '</tbody></table>';
    container.innerHTML = html;
}

async function loadChats() {
    const container = document.getElementById('chats-table-container');
    const summary = document.getElementById('chats-summary');
    const keyword = document.getElementById('chat-keyword').value.trim();
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';

    container.innerHTML = '<div class="loading">加载中...</div>';
    summary.textContent = '正在同步机器人可见群聊...';

    try {
        const data = await feishuGet(`/api/feishu/chats${query}`);
        chats = Array.isArray(data.chats) ? data.chats : [];

        if (chats.length === 0) {
            summary.textContent = keyword ? `未找到与“${keyword}”匹配的群聊` : '当前没有可见群聊';
            container.innerHTML = '<div class="empty-state"><p>请确认机器人已经加入目标群，或调整关键字后重试。</p></div>';
            return;
        }

        summary.textContent = keyword
            ? `共找到 ${chats.length} 个与“${keyword}”匹配的群聊`
            : `当前共找到 ${chats.length} 个机器人可见群聊`;

        let html = '<table><thead><tr><th>群聊名称</th><th>Chat ID</th><th>快捷操作</th></tr></thead><tbody>';
        chats.forEach(chat => {
            const name = escapeHtml(chat.name || '未命名群聊');
            const chatId = escapeHtml(chat.chat_id || chat.chatId || '-');
            html += `<tr>
                <td><strong>${name}</strong></td>
                <td><code>${chatId}</code></td>
                <td><div class="actions">
                    <button class="btn btn-secondary btn-sm" onclick="copyChatId('${chatId}')">复制</button>
                    <button class="btn btn-success btn-sm" onclick="useChatIdForDatasource('${chatId}')">用于数据源</button>
                    <button class="btn btn-warning btn-sm" onclick="useChatIdForTask('${chatId}')">用于通知群</button>
                </div></td>
            </tr>`;
        });
        html += '</tbody></table>';
        container.innerHTML = html;
    } catch (e) {
        summary.textContent = '群聊查询失败';
        container.innerHTML = `<div class="empty-state"><p>${escapeHtml(e.message || '请稍后重试')}</p></div>`;
    }
}

function updateGroupIdSelect() {
    const select = document.getElementById('task-groupId');
    select.innerHTML = '<option value="">请选择数据源...</option>';
    datasources.forEach(ds => {
        const chatInfo = ds.chatId ? ` / ${ds.chatId}` : '';
        select.innerHTML += `<option value="${ds.groupId}">${ds.groupName}${chatInfo}</option>`;
    });
}

function useChatIdForDatasource(chatId) {
    switchTab('datasources');
    openDatasourceModal();
    document.getElementById('datasource-chatId').value = chatId;
    document.getElementById('datasource-groupName').focus();
}

function useChatIdForTask(chatId) {
    switchTab('tasks');
    openTaskModal();
    document.getElementById('task-notifyChatId').value = chatId;
    document.getElementById('task-taskName').focus();
}

function copyChatId(chatId) {
    navigator.clipboard.writeText(chatId).then(() => {
        alert('Chat ID 已复制');
    }).catch(() => {
        alert('复制失败，请手动复制');
    });
}

function toggleChatPicker(type) {
    const picker = document.getElementById(`${type}-chat-picker`);
    const nextVisible = picker.style.display === 'none' || picker.style.display === '';
    picker.style.display = nextVisible ? 'block' : 'none';
    if (nextVisible) {
        searchModalChats(type);
    }
}

async function searchModalChats(type) {
    const input = document.getElementById(`${type}-chat-keyword`);
    const resultBox = document.getElementById(`${type}-chat-results`);
    const keyword = input.value.trim();
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';

    resultBox.innerHTML = '<div class="loading" style="padding: 20px;">加载中...</div>';

    try {
        const data = await feishuGet(`/api/feishu/chats${query}`);
        const items = Array.isArray(data.chats) ? data.chats : [];

        if (items.length === 0) {
            resultBox.innerHTML = '<div class="empty-state" style="padding: 20px 10px;"><p>未找到可选群聊</p></div>';
            return;
        }

        resultBox.innerHTML = items.map(chat => {
            const name = escapeHtml(chat.name || '未命名群聊');
            const chatId = escapeHtml(chat.chat_id || chat.chatId || '');
            return `<div class="picker-item">
                <div class="picker-item-main">
                    <div class="picker-item-title">${name}</div>
                    <div class="picker-item-id">${chatId}</div>
                </div>
                <button type="button" class="btn btn-success btn-sm" onclick="selectModalChat('${type}', '${chatId}')">选择</button>
            </div>`;
        }).join('');
    } catch (e) {
        resultBox.innerHTML = `<div class="empty-state" style="padding: 20px 10px;"><p>${escapeHtml(e.message || '查询失败')}</p></div>`;
    }
}

function selectModalChat(type, chatId) {
    const target = type === 'datasource' ? 'datasource-chatId' : 'task-notifyChatId';
    document.getElementById(target).value = chatId;
    document.getElementById(`${type}-chat-picker`).style.display = 'none';
}

function resetChatPicker(type) {
    document.getElementById(`${type}-chat-picker`).style.display = 'none';
    document.getElementById(`${type}-chat-keyword`).value = '';
    document.getElementById(`${type}-chat-results`).innerHTML = '';
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function openDatasourceModal(datasource = null) {
    editingDatasourceId = datasource ? datasource.id : null;
    document.getElementById('datasource-modal-title').textContent = datasource ? '编辑数据源' : '新增数据源';
    
    if (datasource) {
        document.getElementById('datasource-id').value = datasource.id;
        document.getElementById('datasource-chatId').value = datasource.chatId || '';
        document.getElementById('datasource-groupName').value = datasource.groupName;
        document.getElementById('datasource-redisHost').value = datasource.redisHost;
        document.getElementById('datasource-redisPort').value = datasource.redisPort;
        document.getElementById('datasource-redisPassword').value = datasource.redisPassword || '';
        document.getElementById('datasource-redisDatabase').value = datasource.redisDatabase;
        document.getElementById('datasource-description').value = datasource.description || '';
    } else {
        document.getElementById('datasource-form').reset();
        document.getElementById('datasource-id').value = '';
        document.getElementById('datasource-chatId').value = '';
        document.getElementById('datasource-redisPort').value = 6379;
        document.getElementById('datasource-redisDatabase').value = 0;
    }
    resetChatPicker('datasource');
    
    document.getElementById('datasource-modal').style.display = 'flex';
}

function closeDatasourceModal() {
    document.getElementById('datasource-modal').style.display = 'none';
    editingDatasourceId = null;
}

function openTaskModal(task = null) {
    editingTaskId = task ? task.id : null;
    document.getElementById('task-modal-title').textContent = task ? '编辑任务' : '新增任务';
    updateGroupIdSelect();
    
    if (task) {
        document.getElementById('task-id').value = task.id;
        document.getElementById('task-taskName').value = task.taskName;
        document.getElementById('task-groupId').value = task.groupId;
        document.getElementById('task-taskType').value = task.taskType;
        document.getElementById('task-cronExpression').value = task.cronExpression;
        document.getElementById('task-slowQueryThreshold').value = task.slowQueryThreshold;
        document.getElementById('task-bigKeyMemoryThreshold').value = task.bigKeyMemoryThreshold;
        document.getElementById('task-bigKeyCountThreshold').value = task.bigKeyCountThreshold;
        document.getElementById('task-notifyChatId').value = task.notifyChatId;
        document.getElementById('task-enabled').value = task.enabled.toString();
    } else {
        document.getElementById('task-form').reset();
        document.getElementById('task-id').value = '';
        document.getElementById('task-cronExpression').value = '0 0 * * * ?';
        document.getElementById('task-slowQueryThreshold').value = 10000;
        document.getElementById('task-bigKeyMemoryThreshold').value = 10485760;
        document.getElementById('task-bigKeyCountThreshold').value = 5000;
        document.getElementById('task-enabled').value = '1';
    }
    resetChatPicker('task');
    
    document.getElementById('task-modal').style.display = 'flex';
}

function closeTaskModal() {
    document.getElementById('task-modal').style.display = 'none';
    editingTaskId = null;
}

async function handleDatasourceSubmit(e) {
    e.preventDefault();
    
    const data = {
        chatId: document.getElementById('datasource-chatId').value,
        groupName: document.getElementById('datasource-groupName').value,
        redisHost: document.getElementById('datasource-redisHost').value,
        redisPort: parseInt(document.getElementById('datasource-redisPort').value),
        redisPassword: document.getElementById('datasource-redisPassword').value || null,
        redisDatabase: parseInt(document.getElementById('datasource-redisDatabase').value),
        description: document.getElementById('datasource-description').value || null
    };
    
    try {
        if (editingDatasourceId) {
            await apiPut(`/datasources/${editingDatasourceId}`, data);
            alert('数据源更新成功！');
        } else {
            await apiPost('/datasources', data);
            alert('数据源创建成功！');
        }
        closeDatasourceModal();
        loadDatasources();
    } catch (e) {
        alert('操作失败！');
    }
}

async function handleTaskSubmit(e) {
    e.preventDefault();
    
    const data = {
        taskName: document.getElementById('task-taskName').value,
        groupId: document.getElementById('task-groupId').value,
        taskType: document.getElementById('task-taskType').value,
        cronExpression: document.getElementById('task-cronExpression').value,
        slowQueryThreshold: parseInt(document.getElementById('task-slowQueryThreshold').value),
        bigKeyMemoryThreshold: parseInt(document.getElementById('task-bigKeyMemoryThreshold').value),
        bigKeyCountThreshold: parseInt(document.getElementById('task-bigKeyCountThreshold').value),
        notifyChatId: document.getElementById('task-notifyChatId').value,
        enabled: parseInt(document.getElementById('task-enabled').value)
    };
    
    try {
        if (editingTaskId) {
            await apiPut(`/tasks/${editingTaskId}`, data);
            alert('任务更新成功！');
        } else {
            await apiPost('/tasks', data);
            alert('任务创建成功！');
        }
        closeTaskModal();
        loadTasks();
    } catch (e) {
        alert('操作失败！');
    }
}

function editDatasource(id) {
    const ds = datasources.find(d => d.id === id);
    if (ds) openDatasourceModal(ds);
}

function editTask(id) {
    const task = tasks.find(t => t.id === id);
    if (task) openTaskModal(task);
}

async function testDatasource(groupId) {
    try {
        await apiPost(`/datasources/${groupId}/test`);
        alert('连接成功！');
    } catch (e) {
        alert('连接失败！');
    }
}

async function deleteDatasource(id) {
    if (!confirm('确认删除该数据源吗？')) return;
    try {
        await apiDelete(`/datasources/${id}`);
        loadDatasources();
    } catch (e) {
        alert('删除失败！');
    }
}

async function runTask(id) {
    try {
        await apiPost(`/tasks/${id}/run`);
        alert('任务已触发！');
    } catch (e) {
        alert('触发失败！');
    }
}

async function toggleTask(id, enabled) {
    try {
        await apiPost(`/tasks/${id}/toggle?enabled=${enabled}`);
        loadTasks();
    } catch (e) {
        alert('状态切换失败！');
    }
}

async function deleteTask(id) {
    if (!confirm('确认删除该任务吗？')) return;
    try {
        await apiDelete(`/tasks/${id}`);
        loadTasks();
    } catch (e) {
        alert('删除失败！');
    }
}
