/**
 * 端口归属核查：列出 41280-41299 每个监听端口对应的进程。
 *
 * 为什么需要它：用 `/dev/tcp` 之类的方式只能判断"有没有人监听"，判不出"是谁"，
 * 曾经因此误判"端口全满"（实际 41286 当时是空的）。这里从 /proc 反查 socket inode → pid。
 */
import { readFileSync, readdirSync, readlinkSync } from 'node:fs'

const START = Number(process.env.PORT_START || 41280)
const END = Number(process.env.PORT_END || 41299)

const listen = new Map()
for (const file of ['/proc/net/tcp', '/proc/net/tcp6']) {
  let text = ''
  try { text = readFileSync(file, 'utf8') } catch { continue }
  for (const line of text.split('\n').slice(1)) {
    const cols = line.trim().split(/\s+/)
    if (cols.length < 10 || cols[3] !== '0A') continue
    const port = parseInt(cols[1].split(':')[1], 16)
    if (port < START || port > END) continue
    listen.set(port, cols[9])
  }
}

const inodeToPid = new Map()
for (const pid of readdirSync('/proc').filter((d) => /^\d+$/.test(d))) {
  let fds = []
  try { fds = readdirSync('/proc/' + pid + '/fd') } catch { continue }
  for (const fd of fds) {
    let link = ''
    try { link = readlinkSync('/proc/' + pid + '/fd/' + fd) } catch { continue }
    const match = link.match(/^socket:\[(\d+)\]$/)
    if (match) inodeToPid.set(match[1], pid)
  }
}

// 容器端口由 root 的 docker-proxy 持有，普通用户读不到 fd，此时退化为"docker 端口"提示
const dockerPorts = new Set()
try {
  const out = readFileSync('/tmp/.docker-ports', 'utf8')
  for (const line of out.split('\n')) if (line.trim()) dockerPorts.add(Number(line.trim()))
} catch { /* 没预生成清单就跳过，不静默假装查到了 */ }

console.log('端口   PID      进程')
for (const [port, inode] of [...listen.entries()].sort((a, b) => a[0] - b[0])) {
  const pid = inodeToPid.get(inode)
  let cmd = '?'
  if (pid) {
    try { cmd = readFileSync('/proc/' + pid + '/cmdline', 'utf8').replace(/\0/g, ' ').trim() } catch {}
  } else if (dockerPorts.has(port)) {
    cmd = '(docker 发布端口，需 sudo 才能看到容器名)'
  }
  console.log(String(port).padEnd(6), String(pid ?? '-').padEnd(9), cmd.slice(0, 100))
}
