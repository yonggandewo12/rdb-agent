document.addEventListener('DOMContentLoaded', function() {
    const linkTokenInput = document.getElementById('link-token');
    const submitBtn = document.getElementById('btn-submit');
    const resultArea = document.getElementById('result-area');
    const resultContent = document.getElementById('result-content');
    const copyBtn = document.getElementById('btn-copy');
    const errorArea = document.getElementById('error-area');
    const errorMessage = document.getElementById('error-message');

    submitBtn.addEventListener('click', async function() {
        const linkToken = linkTokenInput.value.trim();
        if (!linkToken) {
            showError('请输入Link Token');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = '查询中...';
        hideError();
        hideResult();

        try {
            const response = await fetch('/api/feishu/get-chat-id', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    link_token: linkToken
                })
            });

            const data = await response.json();
            if (response.ok) {
                showResult(data.chat_id);
            } else {
                showError(data.message || '查询失败');
            }
        } catch (e) {
            showError('网络错误，请稍后重试');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = '获取ChatID';
        }
    });

    copyBtn.addEventListener('click', function() {
        const chatId = resultContent.textContent;
        navigator.clipboard.writeText(chatId).then(() => {
            copyBtn.textContent = '复制成功!';
            setTimeout(() => {
                copyBtn.textContent = '复制ChatID';
            }, 2000);
        }).catch(() => {
            copyBtn.textContent = '复制失败，请手动复制';
            setTimeout(() => {
                copyBtn.textContent = '复制ChatID';
            }, 2000);
        });
    });

    function showResult(chatId) {
        resultContent.textContent = chatId;
        resultArea.style.display = 'block';
        errorArea.style.display = 'none';
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
    }
});
