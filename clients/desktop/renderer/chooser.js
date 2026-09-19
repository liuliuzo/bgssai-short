'use strict';

async function init() {
  const envLabel = document.getElementById('env-label');
  try {
    const cfg = await window.bgssaiClients.getConfig();
    envLabel.textContent = `env: ${cfg.env} · user ${cfg.userUrl} · admin ${cfg.adminUrl}`;
  } catch (err) {
    envLabel.textContent = 'env: unavailable';
  }

  document.querySelectorAll('[data-entry]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const entry = btn.getAttribute('data-entry');
      btn.disabled = true;
      try {
        await window.bgssaiClients.openEntry(entry);
      } catch (err) {
        btn.disabled = false;
        envLabel.textContent = `打开失败: ${entry}`;
      }
    });
  });
}

init();
