/**
 * Kleiner RCON-Client für Tests.
 *   node rcon.cjs host:port passwort-datei "befehl" ["befehl" …]
 * Als Modul: const { rcon } = require('./rcon.cjs'); await rcon('127.0.0.1:25820', 'pw', ['list']);
 */
const net = require('node:net');
const fs = require('node:fs');

function packet(id, type, body) {
  const data = Buffer.from(body, 'utf8');
  const buf = Buffer.alloc(14 + data.length);
  buf.writeInt32LE(10 + data.length, 0);
  buf.writeInt32LE(id, 4);
  buf.writeInt32LE(type, 8);
  data.copy(buf, 12);
  return buf;
}

function rcon(address, password, commands) {
  const [host, port] = address.split(':');
  return new Promise((resolve, reject) => {
    const socket = net.createConnection({ host, port: Number(port) });
    let buffer = Buffer.alloc(0);
    const out = [];
    let index = -1;
    const next = () => {
      index++;
      if (index >= commands.length) {
        socket.end();
        resolve(out);
        return;
      }
      socket.write(packet(index + 2, 2, commands[index]));
    };
    socket.setTimeout(15000, () => reject(new Error('RCON: Zeitüberschreitung')));
    socket.on('error', reject);
    socket.on('connect', () => socket.write(packet(1, 3, password)));
    socket.on('data', (chunk) => {
      buffer = Buffer.concat([buffer, chunk]);
      while (buffer.length >= 4) {
        const size = buffer.readInt32LE(0);
        if (buffer.length < size + 4) return;
        const id = buffer.readInt32LE(4);
        const body = buffer.subarray(12, size + 2).toString('utf8');
        buffer = buffer.subarray(size + 4);
        if (id === -1) {
          reject(new Error('RCON: falsches Passwort'));
          socket.destroy();
          return;
        }
        if (id === 1) {
          next();
        } else {
          out.push(body);
          next();
        }
      }
    });
  });
}

module.exports = { rcon };

if (require.main === module) {
  const [address, passFile, ...commands] = process.argv.slice(2);
  const password = fs.readFileSync(passFile, 'utf8').trim();
  rcon(address, password, commands).then(
    (lines) => lines.forEach((l) => console.log(l)),
    (err) => {
      console.error(err.message);
      process.exit(1);
    },
  );
}
