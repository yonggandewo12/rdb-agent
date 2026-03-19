const API_BASE = '/api/admin';

let currentTab = 'datasources';
let datasources = [];
let tasks = [];
let editingDatasourceId = null;
let editingTaskId = null;

document.addEventListener('DOMContentLoaded', function() {
    initTabs();
    initModals();
    loadDatasources();
    loadTasks();
});

function initTabs() {
    document.getElementById('tab-datasources').addEventListener('click', () => switchTab('datasources'));
    document.getElementById('tab-tasks').addEventListener('click', () => switchTab('tasks'));
}

function initModals() {
    document.getElementById('btn-add-datasource').addEventListener('click', () => openDatasourceModal());
    document.getElementById('btn-add-task').addEventListener('click', () => openTaskModal());
    
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
        if (!res.ok) throw new Error('Request failed');
        return await res.json();
    } catch (e) {
        console.error('API Error:', e);
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
        if (!res.ok) throw new Error('Request failed');
        return res;
    } catch (e) {
        console.error('API Error:', e);
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
        if (!res.ok) throw new Error('Request failed');
        return res;
    } catch (e) {
        console.error('API Error:', e);
        throw e;
    }
}

async function apiDelete(endpoint) {
    try {
        const res = await fetch(`${API_BASE}${endpoint}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Request failed');
        return res;
    } catch (e) {
        console.error('API Error:', e);
        throw e;
    }
}

async function loadDatasources() {
    const container = document.getElementById('datasources-table-container');
    datasources = await apiGet('/datasources');
    
    if (datasources.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>No datasources yet. Click "Add Datasource" to get started.</p></div>';
        return;
    }
    
    let html = '<table><thead><tr><th>Group ID</th><th>Chat ID</th><th>Group Name</th><th>Redis Host</th><th>Database</th><th>Actions</th></tr></thead><tbody>';
    datasources.forEach(ds => {
        html += `<tr>
            <td><code>${ds.groupId}</code></td>
            <td><code>${ds.chatId || '-'}</code></td>
            <td>${ds.groupName}</td>
            <td>${ds.redisHost}:${ds.redisPort}</td>
            <td>${ds.redisDatabase}</td>
            <td><div class="actions">
                <button class="btn btn-success btn-sm" onclick="testDatasource('${ds.groupId}')">Test</button>
                <button class="btn btn-secondary btn-sm" onclick="editDatasource(${ds.id})">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="deleteDatasource(${ds.id})">Delete</button>
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
        container.innerHTML = '<div class="empty-state"><p>No scheduled tasks yet. Click "Add Task" to get started.</p></div>';
        return;
    }
    
    let html = '<table><thead><tr><th>Task Name</th><th>Group ID</th><th>Type</th><th>Cron</th><th>Status</th><th>Last Run</th><th>Actions</th></tr></thead><tbody>';
    tasks.forEach(task => {
        const typeLabel = {slow_query: 'Slow Query', big_key: 'Big Key', all: 'All'}[task.taskType] || task.taskType;
        const statusBadge = task.enabled === 1 
            ? '<span class="badge badge-success">Enabled</span>' 
            : '<span class="badge badge-danger">Disabled</span>';
        const lastRun = task.lastRunTime ? new Date(task.lastRunTime).toLocaleString() : '-';
        
        html += `<tr>
            <td><strong>${task.taskName}</strong></td>
            <td><code>${task.groupId}</code></td>
            <td>${typeLabel}</td>
            <td><code>${task.cronExpression}</code></td>
            <td>${statusBadge}</td>
            <td>${lastRun}</td>
            <td><div class="actions">
                <button class="btn btn-success btn-sm" onclick="runTask(${task.id})">Run</button>
                <button class="btn btn-warning btn-sm" onclick="toggleTask(${task.id}, ${task.enabled === 1 ? 0 : 1})">${task.enabled === 1 ? 'Disable' : 'Enable'}</button>
                <button class="btn btn-secondary btn-sm" onclick="editTask(${task.id})">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="deleteTask(${task.id})">Delete</button>
            </div></td>
        </tr>`;
    });
    html += '</tbody></table>';
    container.innerHTML = html;
}

function updateGroupIdSelect() {
    const select = document.getElementById('task-groupId');
    select.innerHTML = '<option value="">Select a datasource...</option>';
    datasources.forEach(ds => {
        const chatInfo = ds.chatId ? ` / ${ds.chatId}` : '';
        select.innerHTML += `<option value="${ds.groupId}">${ds.groupName} (${ds.groupId}${chatInfo})</option>`;
    });
}

function openDatasourceModal(datasource = null) {
    editingDatasourceId = datasource ? datasource.id : null;
    document.getElementById('datasource-modal-title').textContent = datasource ? 'Edit Datasource' : 'Add Datasource';
    
    if (datasource) {
        document.getElementById('datasource-id').value = datasource.id;
        document.getElementById('datasource-groupId').value = datasource.groupId;
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
    
    document.getElementById('datasource-modal').style.display = 'flex';
}

function closeDatasourceModal() {
    document.getElementById('datasource-modal').style.display = 'none';
    editingDatasourceId = null;
}

function openTaskModal(task = null) {
    editingTaskId = task ? task.id : null;
    document.getElementById('task-modal-title').textContent = task ? 'Edit Task' : 'Add Task';
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
    
    document.getElementById('task-modal').style.display = 'flex';
}

function closeTaskModal() {
    document.getElementById('task-modal').style.display = 'none';
    editingTaskId = null;
}

async function handleDatasourceSubmit(e) {
    e.preventDefault();
    
    const data = {
        groupId: document.getElementById('datasource-groupId').value,
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
            alert('Datasource updated successfully!');
        } else {
            await apiPost('/datasources', data);
            alert('Datasource created successfully!');
        }
        closeDatasourceModal();
        loadDatasources();
    } catch (e) {
        alert('Operation failed!');
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
            alert('Task updated successfully!');
        } else {
            await apiPost('/tasks', data);
            alert('Task created successfully!');
        }
        closeTaskModal();
        loadTasks();
    } catch (e) {
        alert('Operation failed!');
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
        alert('Connection successful!');
    } catch (e) {
        alert('Connection failed!');
    }
}

async function deleteDatasource(id) {
    if (!confirm('Are you sure you want to delete this datasource?')) return;
    try {
        await apiDelete(`/datasources/${id}`);
        loadDatasources();
    } catch (e) {
        alert('Delete failed!');
    }
}

async function runTask(id) {
    try {
        await apiPost(`/tasks/${id}/run`);
        alert('Task triggered!');
    } catch (e) {
        alert('Trigger failed!');
    }
}

async function toggleTask(id, enabled) {
    try {
        await apiPost(`/tasks/${id}/toggle?enabled=${enabled}`);
        loadTasks();
    } catch (e) {
        alert('Toggle failed!');
    }
}

async function deleteTask(id) {
    if (!confirm('Are you sure you want to delete this task?')) return;
    try {
        await apiDelete(`/tasks/${id}`);
        loadTasks();
    } catch (e) {
        alert('Delete failed!');
    }
}
