const http = require('http');
const fs = require('fs');
const path = require('path');

const webIndex = path.join(__dirname, 'index.html');
const apkPath = path.join(__dirname, '..', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

function requestHandler(req, res) {
  if (req.url && req.url.startsWith('/download/RestoOps-Enterprise.apk')) {
    if (fs.existsSync(apkPath)) {
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="RestoOps-Enterprise.apk"'
      });
      fs.createReadStream(apkPath).pipe(res);
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('APK file not found.');
      return;
    }
  }

  fs.readFile(webIndex, (err, content) => {
    if (err) {
      res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Error loading RestoOps Web Portal');
      return;
    }
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache'
    });
    res.end(content);
  });
}

const ports = [process.env.PORT || 3000, 8080, 5173];
const startedPorts = new Set();

ports.forEach((port) => {
  const p = Number(port);
  if (startedPorts.has(p)) return;
  startedPorts.add(p);
  const srv = http.createServer(requestHandler);
  srv.on('error', (err) => {
    console.log(`Port ${p} skipped: ${err.message}`);
  });
  srv.listen(p, '0.0.0.0', () => {
    console.log(`RestoOps Enterprise Web & Mobile Portal listening on http://0.0.0.0:${p}`);
  });
});
