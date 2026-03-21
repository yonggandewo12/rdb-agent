document.addEventListener('DOMContentLoaded', function() {
    const keywordInput = document.getElementById('keyword');
    const submitBtn = document.getElementById('btn-submit');
    const resultArea = document.getElementById('result-area');
    const resultSummary = document.getElementById('result-summary');
    const resultContent = document.getElementById('result-content');
    const errorArea = document.getElementById('error-area');
    const errorMessage = document.getElementById('error-message');

    submitBtn.addEventListener('click', loadChats);
    keywordInput.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            loadChats();
        }
    });

    async function loadChats() {
        const keyword = keywordInput.value.trim();
        const query = keyword ? '?keyword=' + encodeURIComponent(keyword) : '';

        submitBtn.disabled = true;
        submitBtn.textContent = '查询中...';
        hideError();
        hideResult();

        try {
            const response = await fetch('/api/feishu/chats' + query);
            const data = await response.json();

            if (!response.ok) {
                showError(data.message || '查询失败');
                return;
            }

            if (!Array.isArray(data.chats) || data.chats.length === 0) {
                showError('未查询到机器人可见的群聊，请确认机器人已加入目标群，或调整关键字后重试');
                return;
            }

            showResult(data.chats, keyword);
        } catch (e) {
            showError('网络错误，请稍后重试');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = '查询群聊列表';
        }
    }

    function showResult(chats, keyword) {
        const summary = keyword
            ? '共找到 ' + chats.length + ' 个与关键字“' + keyword + '”匹配的群聊'
            : '共找到 ' + chats.length + ' 个机器人可见群聊';

        resultSummary.textContent = summary;
        resultContent.innerHTML = chats.map(function(chat) {
            const name = escapeHtml(chat.name || '未命名群聊');
            const chatId = escapeHtml(chat.chatId || '');
            const description = escapeHtml(chat.description || '');
            const members = typeof chat.memberCount === 'number' ? chat.memberCount : '-';

            return '' +
                '<div style="border: 1px solid rgba(255,255,255,0.08); border-radius: 10px; padding: 16px; margin-bottom: 12px; background: rgba(255,255,255,0.03);">' +
                '  <div style="display: flex; justify-content: space-between; gap: 12px; align-items: start; flex-wrap: wrap;">' +
                '    <div style="flex: 1; min-width: 240px;">' +
                '      <div style="font-size: 16px; color: #fff; font-weight: 600; margin-bottom: 8px;">' + name + '</div>' +
                '      <div style="font-family: monospace; font-size: 13px; color: #b8c7ff; word-break: break-all; margin-bottom: 8px;">Chat ID: ' + chatId + '</div>' +
                '      <div style="font-size: 13px; color: #c7c7c7; margin-bottom: 4px;">成员数: ' + members + '</div>' +
                '      <div style="font-size: 13px; color: #9aa4c7;">描述: ' + (description || '无') + '</div>' +
                '    </div>' +
                '    <div>' +
                '      <button class="btn btn-secondary copy-chat-id" data-chat-id="' + chatId + '">复制 Chat ID</button>' +
                '    </div>' +
                '  </div>' +
                '</div>';
        }).join('');

        bindCopyButtons();
        resultArea.style.display = 'block';
        errorArea.style.display = 'none';
    }

    function bindCopyButtons() {
        document.querySelectorAll('.copy-chat-id').forEach(function(button) {
            button.addEventListener('click', function() {
                const chatId = button.getAttribute('data-chat-id') || '';
                const originalText = button.textContent;

                navigator.clipboard.writeText(chatId).then(function() {
                    button.textContent = '复制成功';
                    setTimeout(function() {
                        button.textContent = originalText;
                    }, 2000);
                }).catch(function() {
                    button.textContent = '复制失败';
                    setTimeout(function() {
                        button.textContent = originalText;
                    }, 2000);
                });
            });
        });
    }

    function showError(message) {
        errorMessage.textContent = message;
        errorArea.style.display = 'block';
        resultArea.style.display = 'none';
    }

    function hideError() {
        errorArea.style.display = 'none';
    }

    function hideResult() {
        resultArea.style.display = 'none';
        resultContent.innerHTML = '';
        resultSummary.textContent = '';
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
});
