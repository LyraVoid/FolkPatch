package rikka.shizuku.server.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * 内置 Shizuku Server 的持久化日志汇聚点。
 *
 * <p>所有经过 {@link Logger#println} 的日志都会在此留存两份：
 * <ul>
 *   <li>内存环形缓冲（最近 {@link #MEM_MAX_LINES} 行），供 binder 快速读取；</li>
 *   <li>磁盘文件 {@link #LOG_FILE}，跨 server 重启保留，用于排查"配置为什么消失"
 *       之类跨进程/跨重启的问题（logcat 会随重启或轮转丢失）。</li>
 * </ul>
 *
 * <p>文件写在 shell 的 de 目录（与 shizuku.json 同级），server 无论以 root 还是 shell
 * 身份运行都可读写；超过 {@link #FILE_MAX_BYTES} 时轮转一次到 {@code .1}，磁盘占用有上限。
 *
 * <p>本类只被 server 进程使用；manager 侧通过 binder 事务或 root 直接读取 {@link #LOG_FILE}。
 */
public final class ServerLog {

    private ServerLog() {
    }

    /** 与 shizuku.json 同级，server（root/shell）均可读写。 */
    public static final File LOG_DIR = new File("/data/user_de/0/com.android.shell");
    public static final File LOG_FILE = new File(LOG_DIR, "shizuku_folk.log");
    private static final File LOG_FILE_BACKUP = new File(LOG_DIR, "shizuku_folk.log.1");

    /** 单文件上限 512 KiB，超出后轮转一次；连同 .1 备份磁盘占用 ≤ ~1 MiB。 */
    private static final long FILE_MAX_BYTES = 512 * 1024;
    /** 内存环形缓冲行数上限。 */
    private static final int MEM_MAX_LINES = 2000;
    /** binder 单次事务返回上限（避免超过 1 MiB binder 限制），返回文件末尾这么多字节。 */
    private static final int DUMP_MAX_BYTES = 256 * 1024;

    private static final Object LOCK = new Object();
    private static final Deque<String> MEM = new ArrayDeque<>(MEM_MAX_LINES);
    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ENGLISH);

    private static FileOutputStream fileStream;
    private static boolean fileInitTried;

    private static char levelChar(int priority) {
        switch (priority) {
            case Log.VERBOSE: return 'V';
            case Log.DEBUG:   return 'D';
            case Log.INFO:    return 'I';
            case Log.WARN:    return 'W';
            case Log.ERROR:   return 'E';
            case Log.ASSERT:  return 'A';
            default:          return '?';
        }
    }

    /** 由 {@link Logger#println} 调用：把一条日志写入内存缓冲与磁盘文件。 */
    public static void append(int priority, String tag, String msg) {
        if (msg == null) {
            msg = "";
        }
        String line = TIME_FMT.format(new java.util.Date())
                + ' ' + levelChar(priority) + '/' + tag + ": " + msg;
        synchronized (LOCK) {
            if (MEM.size() >= MEM_MAX_LINES) {
                MEM.pollFirst();
            }
            MEM.addLast(line);
            writeFileLocked(line);
        }
    }

    private static void writeFileLocked(String line) {
        try {
            if (!fileInitTried) {
                fileInitTried = true;
                openFileLocked();
            }
            if (fileStream == null) {
                return;
            }
            rotateIfNeededLocked();
            if (fileStream == null) {
                openFileLocked();
                if (fileStream == null) {
                    return;
                }
            }
            fileStream.write((line + '\n').getBytes());
            fileStream.flush();
        } catch (Throwable ignored) {
            // 日志落盘失败不能影响 server 正常运行；内存缓冲仍可用。
        }
    }

    private static void openFileLocked() {
        try {
            if (!LOG_DIR.isDirectory()) {
                // 目录理论上一定存在（shizuku.json 所在），缺失时也尽力创建。
                //noinspection ResultOfMethodCallIgnored
                LOG_DIR.mkdirs();
            }
            fileStream = new FileOutputStream(LOG_FILE, true /* append */);
            //noinspection ResultOfMethodCallIgnored
            LOG_FILE.setReadable(true, false);
        } catch (IOException e) {
            fileStream = null;
        }
    }

    private static void rotateIfNeededLocked() {
        try {
            if (LOG_FILE.length() < FILE_MAX_BYTES) {
                return;
            }
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException ignored) {
                }
                fileStream = null;
            }
            //noinspection ResultOfMethodCallIgnored
            LOG_FILE_BACKUP.delete();
            //noinspection ResultOfMethodCallIgnored
            LOG_FILE.renameTo(LOG_FILE_BACKUP);
            openFileLocked();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 返回持久化日志文本（末尾最多 {@link #DUMP_MAX_BYTES} 字节）。
     * 优先读磁盘文件（含本次启动前的历史），文件不可用时回退到内存缓冲。
     */
    public static String dump() {
        synchronized (LOCK) {
            String fromFile = readFileTailLocked();
            if (fromFile != null && !fromFile.isEmpty()) {
                return fromFile;
            }
            if (MEM.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (String l : MEM) {
                sb.append(l).append('\n');
            }
            return sb.toString();
        }
    }

    private static String readFileTailLocked() {
        if (!LOG_FILE.isFile()) {
            return null;
        }
        try (RandomAccessFile raf = new RandomAccessFile(LOG_FILE, "r")) {
            long len = raf.length();
            long start = Math.max(0, len - DUMP_MAX_BYTES);
            raf.seek(start);
            byte[] buf = new byte[(int) (len - start)];
            raf.readFully(buf);
            String text = new String(buf);
            if (start > 0) {
                int nl = text.indexOf('\n');
                if (nl >= 0 && nl + 1 < text.length()) {
                    // 丢弃被截断的首行残片
                    text = text.substring(nl + 1);
                }
                text = "…(older lines truncated)…\n" + text;
            }
            return text;
        } catch (Throwable e) {
            return null;
        }
    }

    /** 清空内存缓冲与磁盘文件。 */
    public static void clear() {
        synchronized (LOCK) {
            MEM.clear();
            try {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException ignored) {
                    }
                    fileStream = null;
                }
                //noinspection ResultOfMethodCallIgnored
                LOG_FILE.delete();
                //noinspection ResultOfMethodCallIgnored
                LOG_FILE_BACKUP.delete();
                fileInitTried = false;
            } catch (Throwable ignored) {
            }
        }
    }
}
