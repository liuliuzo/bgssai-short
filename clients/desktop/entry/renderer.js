'use strict';

(async function boot() {
  const root = document.getElementById('entries');
  const meta = document.getElementById('meta');
  if (!window.bgssaiClients) {
    meta.textContent = 'preload bridge missing';
    return;
  }

  const bootstrap = await window.bgssaiClients.getBootstrap();
  meta.textContent = 'profile=' + bootstrap.profile + ' · installable=single-app dual-entry';

  bootstrap.entries.forEach((entry) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'entry entry--' + entry.id;
    btn.innerHTML =
      '<span class="entry__label">' +
      escapeHtml(entry.label) +
      '</span><span class="entry__hint">' +
      escapeHtml(entry.url) +
      '</span>';
    btn.addEventListener('click', async () => {
      btn.disabled = true;
      try {
        await window.bgssaiClients.openEntry(entry.id);
      } catch (err) {
        btn.disabled = false;
        meta.textContent = String(err && err.message ? err.message : err);
      }
    });
    root.appendChild(btn);
  });
})();

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
