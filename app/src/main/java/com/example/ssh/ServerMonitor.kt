package com.example.ssh

import com.example.data.model.Server
import kotlin.math.max

data class SystemStats(
    val osName: String = "Unknown Linux",
    val kernel: String = "",
    val uptime: String = "",
    val loadAvg: String = ""
)

data class CpuStats(
    val percentage: Float = 0f,
    val prevTotal: Long = 0,
    val prevIdle: Long = 0
)

data class RamStats(
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val percentage: Float = 0f
)

data class DiskStats(
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val percentage: Float = 0f,
    val mountPoint: String = "/"
)

data class NetStats(
    val downloadSpeedBytes: Long = 0,
    val uploadSpeedBytes: Long = 0,
    val prevRxBytes: Long = 0,
    val prevTxBytes: Long = 0,
    val lastCheckTime: Long = 0
)

data class ProcessInfo(
    val pid: String,
    val cpuPercentage: Float,
    val memPercentage: Float,
    val command: String
)

data class ServerPerformanceMetrics(
    val system: SystemStats = SystemStats(),
    val cpu: CpuStats = CpuStats(),
    val ram: RamStats = RamStats(),
    val disk: DiskStats = DiskStats(),
    val net: NetStats = NetStats(),
    val processes: List<ProcessInfo> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

object ServerMonitor {

    private const val MONITOR_COMMAND = """
echo "===SYSTEM===" && uname -sr && cat /etc/os-release | grep -E "^PRETTY_NAME=" | cut -d= -f2 | tr -d '"' && uptime && echo "===CPU===" && cat /proc/stat | head -n 1 && echo "===MEM===" && cat /proc/meminfo | grep -E "MemTotal:|MemAvailable:" && echo "===DISK===" && df -B1 / && echo "===NET===" && cat /proc/net/dev && echo "===PROC===" && ps -eo pid,%cpu,%mem,comm --sort=-%cpu | head -n 15
"""

    /**
     * Fetch all metrics from server in a single SSH execution block
     */
    suspend fun fetchMetrics(
        server: Server,
        prevMetrics: ServerPerformanceMetrics? = null
    ): ServerPerformanceMetrics {
        val rawOutput = SshManager.executeCommand(server, MONITOR_COMMAND)
        return parseMetrics(rawOutput, prevMetrics)
    }

    /**
     * Parse raw shell output into a structured ServerPerformanceMetrics object
     */
    fun parseMetrics(
        raw: String,
        prevMetrics: ServerPerformanceMetrics? = null
    ): ServerPerformanceMetrics {
        val blocks = raw.split("===")
        var systemStats = prevMetrics?.system ?: SystemStats()
        var cpuStats = prevMetrics?.cpu ?: CpuStats()
        var ramStats = prevMetrics?.ram ?: RamStats()
        var diskStats = prevMetrics?.disk ?: DiskStats()
        var netStats = prevMetrics?.net ?: NetStats()
        val processesList = mutableListOf<ProcessInfo>()

        for (i in 1 until blocks.size step 2) {
            if (i + 1 >= blocks.size) break
            val sectionName = blocks[i].trim()
            val sectionContent = blocks[i + 1].trim()

            when (sectionName) {
                "SYSTEM" -> {
                    val lines = sectionContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val kernel = lines.getOrNull(0) ?: ""
                    val osName = lines.getOrNull(1) ?: "Linux Server"
                    val uptimeLine = lines.getOrNull(2) ?: ""
                    val loadAvg = if (uptimeLine.contains("load average:")) {
                        uptimeLine.substringAfter("load average:").trim()
                    } else ""
                    val uptimeClean = if (uptimeLine.contains("up")) {
                        val part = uptimeLine.substringAfter("up").substringBefore(",").trim()
                        if (part.contains(":") && !part.contains("day")) "$part hours" else part
                    } else ""

                    systemStats = SystemStats(osName, kernel, uptimeClean, loadAvg)
                }
                "CPU" -> {
                    val cpuLine = sectionContent.split("\n").firstOrNull { it.startsWith("cpu") } ?: ""
                    if (cpuLine.isNotEmpty()) {
                        val parts = cpuLine.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                        if (parts.size >= 5) {
                            val user = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                            val nice = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                            val system = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                            val idle = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                            val iowait = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                            val irq = parts.getOrNull(6)?.toLongOrNull() ?: 0L
                            val softirq = parts.getOrNull(7)?.toLongOrNull() ?: 0L
                            val steal = parts.getOrNull(8)?.toLongOrNull() ?: 0L

                            val currentTotal = user + nice + system + idle + iowait + irq + softirq + steal
                            val currentIdle = idle + iowait

                            if (prevMetrics != null && prevMetrics.cpu.prevTotal > 0) {
                                val totalDiff = currentTotal - prevMetrics.cpu.prevTotal
                                val idleDiff = currentIdle - prevMetrics.cpu.prevIdle
                                if (totalDiff > 0) {
                                    val usage = (totalDiff - idleDiff).toFloat() / totalDiff * 100f
                                    cpuStats = CpuStats(max(0f, usage), currentTotal, currentIdle)
                                }
                            } else {
                                cpuStats = CpuStats(0f, currentTotal, currentIdle)
                            }
                        }
                    }
                }
                "MEM" -> {
                    var totalMemKb = 0L
                    var availMemKb = 0L
                    for (line in sectionContent.split("\n")) {
                        val lower = line.lowercase()
                        if (lower.contains("memtotal")) {
                            totalMemKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        } else if (lower.contains("memavailable")) {
                            availMemKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        }
                    }
                    if (totalMemKb > 0) {
                        val usedMemKb = totalMemKb - availMemKb
                        val percentage = (usedMemKb.toFloat() / totalMemKb) * 100f
                        ramStats = RamStats(
                            totalBytes = totalMemKb * 1024,
                            usedBytes = usedMemKb * 1024,
                            percentage = percentage
                        )
                    }
                }
                "DISK" -> {
                    val lines = sectionContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    // Typically: Filesystem 1B-blocks Used Available Use% Mounted on
                    // We parse the 2nd line which is the metrics line
                    val metricLine = lines.getOrNull(1)
                    if (metricLine != null) {
                        val parts = metricLine.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                        if (parts.size >= 5) {
                            val total = parts[1].toLongOrNull() ?: 0L
                            val used = parts[2].toLongOrNull() ?: 0L
                            val percentStr = parts[4].replace("%", "")
                            val percentage = percentStr.toFloatOrNull() ?: 0f
                            val mount = parts.getOrNull(5) ?: "/"

                            diskStats = DiskStats(total, used, percentage, mount)
                        }
                    }
                }
                "NET" -> {
                    var currentRx = 0L
                    var currentTx = 0L
                    for (line in sectionContent.split("\n")) {
                        if (line.contains(":") && !line.contains("lo:")) {
                            val parts = line.substringAfter(":").trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
                            val rx = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                            val tx = parts.getOrNull(8)?.toLongOrNull() ?: 0L
                            currentRx += rx
                            currentTx += tx
                        }
                    }
                    val now = System.currentTimeMillis()
                    if (prevMetrics != null && prevMetrics.net.lastCheckTime > 0) {
                        val timeDiffSec = (now - prevMetrics.net.lastCheckTime) / 1000.0
                        if (timeDiffSec > 0) {
                            val rxDiff = currentRx - prevMetrics.net.prevRxBytes
                            val txDiff = currentTx - prevMetrics.net.prevTxBytes
                            
                            val dlSpeed = if (rxDiff >= 0) (rxDiff / timeDiffSec).toLong() else 0L
                            val ulSpeed = if (txDiff >= 0) (txDiff / timeDiffSec).toLong() else 0L

                            netStats = NetStats(dlSpeed, ulSpeed, currentRx, currentTx, now)
                        }
                    } else {
                        netStats = NetStats(0L, 0L, currentRx, currentTx, now)
                    }
                }
                "PROC" -> {
                    val lines = sectionContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    // First line is header: PID %CPU %MEM COMMAND
                    for (lineIdx in 1 until lines.size) {
                        val line = lines[lineIdx]
                        val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                        if (parts.size >= 4) {
                            val pid = parts[0]
                            val cpu = parts[1].toFloatOrNull() ?: 0f
                            val mem = parts[2].toFloatOrNull() ?: 0f
                            // Command name is the rest of parts joined together
                            val cmd = parts.subList(3, parts.size).joinToString(" ")
                            processesList.add(ProcessInfo(pid, cpu, mem, cmd))
                        }
                    }
                }
            }
        }

        return ServerPerformanceMetrics(
            system = systemStats,
            cpu = cpuStats,
            ram = ramStats,
            disk = diskStats,
            net = netStats,
            processes = processesList
        )
    }
}
