export const consoleHtml = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Remote Coding Workstation</title>
  <style>
    :root { color-scheme: dark; }
    body { margin: 0; font-family: Segoe UI, sans-serif; background: #111418; color: #e8edf2; }
    main { max-width: 560px; margin: 40px auto; background: #1b2128; border: 1px solid #2c3540; border-radius: 12px; padding: 24px; }
    h1 { font-size: 20px; margin: 0 0 16px; }
    .row { margin: 14px 0; }
    .label { color: #93a1b0; font-size: 12px; text-transform: uppercase; letter-spacing: .06em; }
    .value { font-size: 22px; margin-top: 4px; font-variant-numeric: tabular-nums; }
    .code { font-size: 36px; letter-spacing: .2em; font-weight: 700; }
    button { background: #2d6cdf; color: white; border: 0; border-radius: 8px; padding: 10px 14px; margin-right: 8px; cursor: pointer; }
    button.secondary { background: #3a4450; }
    .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #3dd68c; margin-right: 6px; }
    ul { padding-left: 18px; }
  </style>
</head>
<body>
  <main>
    <h1>Remote Coding Workstation</h1>
    <div class="row"><div class="label">Status</div><div class="value"><span class="dot"></span><span id="status">Running</span></div></div>
    <div class="row"><div class="label">IP Address</div><div class="value" id="ips">...</div></div>
    <div class="row"><div class="label">Port</div><div class="value" id="port">8765</div></div>
    <div class="row"><div class="label">Pairing Code</div><div class="value code" id="code">------</div></div>
    <div class="row"><div class="label">Remote Control</div><div class="value" id="remote">ON</div></div>
    <div class="row"><div class="label">Connected Devices</div><ul id="devices"><li>None</li></ul></div>
    <div class="row"><div class="label">Internet (relay)</div><div class="value" id="relay">OFF</div></div>
    <div class="row"><div class="label">Node ID (phone)</div><div class="value" id="nodeId">—</div></div>
    <div class="row"><div class="label">Relay URL (phone)</div><div class="value" id="relayUrl" style="font-size:14px;word-break:break-all">—</div></div>
    <div class="row">
      <button id="toggle">Toggle remote control</button>
      <button class="secondary" id="regen">New pairing code</button>
    </div>
  </main>
  <script>
    async function refresh() {
      const res = await fetch('/api/console/state');
      const data = await res.json();
      document.getElementById('status').textContent = data.status;
      document.getElementById('ips').textContent = (data.ips || []).join(', ') || 'No private IPv4';
      document.getElementById('port').textContent = data.port;
      document.getElementById('code').textContent = data.pairingCode;
      document.getElementById('remote').textContent = data.remoteControl ? 'ON' : 'OFF';
      const relay = data.relay || {};
      const relayEl = document.getElementById('relay');
      if (!relay.enabled) {
        relayEl.textContent = 'OFF (enable in config.json)';
      } else {
        relayEl.textContent = relay.online ? 'ONLINE' : 'CONNECTING…';
      }
      document.getElementById('nodeId').textContent = relay.nodeId || '—';
      let base = relay.url || '';
      if (base.endsWith('/')) base = base.slice(0, -1);
      document.getElementById('relayUrl').textContent = relay.enabled && base
        ? base + (relay.phoneBasePath || '')
        : '—';
      const list = document.getElementById('devices');
      list.innerHTML = '';
      if (!data.connected?.length) {
        list.innerHTML = '<li>None</li>';
      } else {
        data.connected.forEach((c) => {
          const li = document.createElement('li');
          li.textContent = c.deviceName + ' · ' + c.address;
          list.appendChild(li);
        });
      }
    }
    document.getElementById('toggle').onclick = async () => {
      const current = document.getElementById('remote').textContent === 'ON';
      await fetch('/api/console/remote-control', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled: !current })
      });
      refresh();
    };
    document.getElementById('regen').onclick = async () => {
      await fetch('/api/console/regenerate-code', { method: 'POST' });
      refresh();
    };
    refresh();
    setInterval(refresh, 2000);
  </script>
</body>
</html>
`;
