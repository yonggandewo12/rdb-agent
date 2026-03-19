import React, { useState, useEffect } from 'https://esm.sh/react@18';
import ReactDOM from 'https://esm.sh/react-dom@18';

const API_BASE = '/api/admin';

const api = {
    async get(endpoint) {
        const res = await fetch(`${API_BASE}${endpoint}`);
        if (!res.ok) throw new Error('请求失败');
        return res.json();
    },
    async post(endpoint, data) {
        const res = await fetch(`${API_BASE}${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error('请求失败');
        return res;
    },
    async put(endpoint, data) {
        const res = await fetch(`${API_BASE}${endpoint}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error('请求失败');
        return res;
    },
    async delete(endpoint) {
        const res = await fetch(`${API_BASE}${endpoint}`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error('请求失败');
        return res;
    }
};

function App() {
    const [activeTab, setActiveTab] = useState('datasources');
    return (
        <div>
            <div className="header">
                <div className="header-content">
                    <h1>RDB-Agent Admin</h1>
                    <div className="nav-tabs">
                        <button 
                            className={`nav-tab ${activeTab === 'datasources' ? 'active' : ''}`}
                            onClick={() => setActiveTab('datasources')}
                        >Datasources</button>
                        <button 
                            className={`nav-tab ${activeTab === 'tasks' ? 'active' : ''}`}
                            onClick={() => setActiveTab('tasks')}
                        >Scheduled Tasks</button>
                    </div>
                </div>
            </div>
            <div className="container">
                {activeTab === 'datasources' ? <DatasourceManagement /> : <ScheduledTaskManagement />}
            </div>
        </div>
    );
}

function DatasourceManagement() {
    const [datasources, setDatasources] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [formData, setFormData] = useState({
        groupId: '',
        groupName: '',
        redisHost: '',
        redisPort: 6379,
        redisPassword: '',
        redisDatabase: 0,
        description: ''
    });
    const [alert, setAlert] = useState(null);

    const loadDatasources = async () => {
        try {
            const data = await api.get('/datasources');
            setDatasources(data || []);
        } catch (e) {
            console.error('Failed to load datasources');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadDatasources(); }, []);

    const showAlertMsg = (msg, type) => {
        setAlert({ msg, type });
        setTimeout(() => setAlert(null), 3000);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingId) {
                await api.put(`/datasources/${editingId}`, formData);
                showAlertMsg('Datasource updated successfully', 'success');
            } else {
                await api.post('/datasources', formData);
                showAlertMsg('Datasource created successfully', 'success');
            }
            setShowModal(false);
            setEditingId(null);
            resetForm();
            loadDatasources();
        } catch (e) {
            showAlertMsg('Operation failed', 'error');
        }
    };

    const resetForm = () => {
        setFormData({
            groupId: '',
            groupName: '',
            redisHost: '',
            redisPort: 6379,
            redisPassword: '',
            redisDatabase: 0,
            description: ''
        });
    };

    const handleEdit = (ds) => {
        setEditingId(ds.id);
        setFormData(ds);
        setShowModal(true);
    };

    const handleDelete = async (id) => {
        if (!confirm('Are you sure you want to delete this datasource?')) return;
        try {
            await api.delete(`/datasources/${id}`);
            showAlertMsg('Deleted successfully', 'success');
            loadDatasources();
        } catch (e) {
            showAlertMsg('Delete failed', 'error');
        }
    };

    const handleTest = async (groupId) => {
        try {
            await api.post(`/datasources/${groupId}/test`);
            showAlertMsg('Connection successful', 'success');
        } catch (e) {
            showAlertMsg('Connection failed', 'error');
        }
    };

    return (
        <div>
            {alert && <div className={`alert alert-${alert.type}`}>{alert.msg}</div>}
            
            <div className="card">
                <div className="card-header">
                    <h2>Redis Datasources</h2>
                    <button className="btn btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
                        + Add Datasource
                    </button>
                </div>

                {loading ? <div className="loading">Loading...</div> : (
                    <table>
                        <thead><tr><th>Group ID</th><th>Group Name</th><th>Redis Host</th><th>Database</th><th>Actions</th></tr></thead>
                        <tbody>
                            {datasources.length === 0 ? (
                                <tr><td colSpan="5" className="empty-state"><p>No datasources yet. Click "Add Datasource" to get started.</p></td></tr>
                            ) : (
                                datasources.map(ds => (
                                    <tr key={ds.id}>
                                        <td><code>{ds.groupId}</code></td>
                                        <td>{ds.groupName}</td>
                                        <td>{ds.redisHost}:{ds.redisPort}</td>
                                        <td>{ds.redisDatabase}</td>
                                        <td><div className="actions">
                                            <button className="btn btn-success btn-sm" onClick={() => handleTest(ds.groupId)}>Test</button>
                                            <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(ds)}>Edit</button>
                                            <button className="btn btn-danger btn-sm" onClick={() => handleDelete(ds.id)}>Delete</button>
                                        </div></td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                )}
            </div>

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>{editingId ? 'Edit Datasource' : 'Add Datasource'}</h3>
                            <button className="modal-close" onClick={() => setShowModal(false)}>&times;</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Group ID *</label>
                                    <input
                                        required
                                        value={formData.groupId}
                                        onChange={e => setFormData({...formData, groupId: e.target.value})}
                                        placeholder="e.g. group_001"
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Group Name *</label>
                                    <input
                                        required
                                        value={formData.groupName}
                                        onChange={e => setFormData({...formData, groupName: e.target.value})}
                                        placeholder="Group display name"
                                    />
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Redis Host *</label>
                                        <input
                                            required
                                            value={formData.redisHost}
                                            onChange={e => setFormData({...formData, redisHost: e.target.value})}
                                            placeholder="localhost"
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Redis Port *</label>
                                        <input
                                            required
                                            type="number"
                                            value={formData.redisPort}
                                            onChange={e => setFormData({...formData, redisPort: parseInt(e.target.value)})}
                                        />
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Redis Password</label>
                                        <input
                                            type="password"
                                            value={formData.redisPassword}
                                            onChange={e => setFormData({...formData, redisPassword: e.target.value})}
                                            placeholder="Leave empty if no password"
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Database # *</label>
                                        <input
                                            required
                                            type="number"
                                            value={formData.redisDatabase}
                                            onChange={e => setFormData({...formData, redisDatabase: parseInt(e.target.value)})}
                                        />
                                    </div>
                                </div>
                                <div className="form-group">
                                    <label>Description</label>
                                    <input
                                        value={formData.description}
                                        onChange={e => setFormData({...formData, description: e.target.value})}
                                        placeholder="Optional description"
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editingId ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

function ScheduledTaskManagement() {
    const [tasks, setTasks] = useState([]);
    const [datasources, setDatasources] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [formData, setFormData] = useState({
        taskName: '',
        groupId: '',
        taskType: 'all',
        cronExpression: '0 0 * * * ?',
        slowQueryThreshold: 10000,
        bigKeyMemoryThreshold: 10485760,
        bigKeyCountThreshold: 5000,
        notifyChatId: '',
        enabled: 1
    });
    const [alert, setAlert] = useState(null);

    const loadData = async () => {
        try {
            const [tasksData, dsData] = await Promise.all([
                api.get('/tasks'),
                api.get('/datasources')
            ]);
            setTasks(tasksData || []);
            setDatasources(dsData || []);
        } catch (e) {
            console.error('Failed to load data');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadData(); }, []);

    const showAlertMsg = (msg, type) => {
        setAlert({ msg, type });
        setTimeout(() => setAlert(null), 3000);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingId) {
                await api.put(`/tasks/${editingId}`, formData);
                showAlertMsg('Task updated successfully', 'success');
            } else {
                await api.post('/tasks', formData);
                showAlertMsg('Task created successfully', 'success');
            }
            setShowModal(false);
            setEditingId(null);
            resetForm();
            loadData();
        } catch (e) {
            showAlertMsg('Operation failed', 'error');
        }
    };

    const resetForm = () => {
        setFormData({
            taskName: '',
            groupId: '',
            taskType: 'all',
            cronExpression: '0 0 * * * ?',
            slowQueryThreshold: 10000,
            bigKeyMemoryThreshold: 10485760,
            bigKeyCountThreshold: 5000,
            notifyChatId: '',
            enabled: 1
        });
    };

    const handleEdit = (task) => {
        setEditingId(task.id);
        setFormData(task);
        setShowModal(true);
    };

    const handleDelete = async (id) => {
        if (!confirm('Are you sure you want to delete this task?')) return;
        try {
            await api.delete(`/tasks/${id}`);
            showAlertMsg('Deleted successfully', 'success');
            loadData();
        } catch (e) {
            showAlertMsg('Delete failed', 'error');
        }
    };

    const handleToggle = async (id, enabled) => {
        try {
            await api.post(`/tasks/${id}/toggle?enabled=${enabled}`);
            showAlertMsg('Status updated', 'success');
            loadData();
        } catch (e) {
            showAlertMsg('Status update failed', 'error');
        }
    };

    const handleRun = async (id) => {
        try {
            await api.post(`/tasks/${id}/run`);
            showAlertMsg('Task triggered', 'success');
        } catch (e) {
            showAlertMsg('Trigger failed', 'error');
        }
    };

    const getTaskTypeLabel = (type) => {
        const map = { slow_query: 'Slow Query', big_key: 'Big Key', all: 'All' };
        return map[type] || type;
    };

    return (
        <div>
            {alert && <div className={`alert alert-${alert.type}`}>{alert.msg}</div>}
            
            <div className="card">
                <div className="card-header">
                    <h2>Scheduled Tasks</h2>
                    <button className="btn btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
                        + Add Task
                    </button>
                </div>

                {loading ? <div className="loading">Loading...</div> : (
                    <table>
                        <thead><tr><th>Task Name</th><th>Group ID</th><th>Type</th><th>Cron</th><th>Status</th><th>Last Run</th><th>Actions</th></tr></thead>
                        <tbody>
                            {tasks.length === 0 ? (
                                <tr><td colSpan="7" className="empty-state"><p>No scheduled tasks yet. Click "Add Task" to get started.</p></td></tr>
                            ) : (
                                tasks.map(task => (
                                    <tr key={task.id}>
                                        <td><strong>{task.taskName}</strong></td>
                                        <td><code>{task.groupId}</code></td>
                                        <td>{getTaskTypeLabel(task.taskType)}</td>
                                        <td><code>{task.cronExpression}</code></td>
                                        <td><span className={`badge ${task.enabled === 1 ? 'badge-success' : 'badge-danger'}`}>{task.enabled === 1 ? 'Enabled' : 'Disabled'}</span></td>
                                        <td>{task.lastRunTime ? new Date(task.lastRunTime).toLocaleString() : '-'}</td>
                                        <td><div className="actions">
                                            <button className="btn btn-success btn-sm" onClick={() => handleRun(task.id)}>Run</button>
                                            <button className="btn btn-warning btn-sm" onClick={() => handleToggle(task.id, task.enabled === 1 ? 0 : 1)}>
                                                {task.enabled === 1 ? 'Disable' : 'Enable'}
                                            </button>
                                            <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(task)}>Edit</button>
                                            <button className="btn btn-danger btn-sm" onClick={() => handleDelete(task.id)}>Delete</button>
                                        </div></td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                )}
            </div>

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>{editingId ? 'Edit Task' : 'Add Task'}</h3>
                            <button className="modal-close" onClick={() => setShowModal(false)}>&times;</button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Task Name *</label>
                                    <input
                                        required
                                        value={formData.taskName}
                                        onChange={e => setFormData({...formData, taskName: e.target.value})}
                                        placeholder="Task display name"
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Group ID *</label>
                                    <select
                                        required
                                        value={formData.groupId}
                                        onChange={e => setFormData({...formData, groupId: e.target.value})}
                                    >
                                        <option value="">Select a datasource...</option>
                                        {datasources.map(ds => (
                                            <option key={ds.id} value={ds.groupId}>{ds.groupName} ({ds.groupId})</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Task Type *</label>
                                        <select
                                            required
                                            value={formData.taskType}
                                            onChange={e => setFormData({...formData, taskType: e.target.value})}
                                        >
                                            <option value="all">All (Slow Query + Big Key)</option>
                                            <option value="slow_query">Slow Query Only</option>
                                            <option value="big_key">Big Key Only</option>
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label>Cron Expression *</label>
                                        <input
                                            required
                                            value={formData.cronExpression}
                                            onChange={e => setFormData({...formData, cronExpression: e.target.value})}
                                            placeholder="0 0 * * * ?"
                                        />
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Slow Query Threshold (μs)</label>
                                        <input
                                            type="number"
                                            value={formData.slowQueryThreshold}
                                            onChange={e => setFormData({...formData, slowQueryThreshold: parseInt(e.target.value)})}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Big Key Memory Threshold (bytes)</label>
                                        <input
                                            type="number"
                                            value={formData.bigKeyMemoryThreshold}
                                            onChange={e => setFormData({...formData, bigKeyMemoryThreshold: parseInt(e.target.value)})}
                                        />
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Big Key Count Threshold</label>
                                        <input
                                            type="number"
                                            value={formData.bigKeyCountThreshold}
                                            onChange={e => setFormData({...formData, bigKeyCountThreshold: parseInt(e.target.value)})}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Feishu Chat ID *</label>
                                        <input
                                            required
                                            value={formData.notifyChatId}
                                            onChange={e => setFormData({...formData, notifyChatId: e.target.value})}
                                            placeholder="Feishu chat ID"
                                        />
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editingId ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

ReactDOM.render(<App />, document.getElementById('root'));
