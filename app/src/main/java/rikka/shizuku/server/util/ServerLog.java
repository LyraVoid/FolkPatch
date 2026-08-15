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
 * Persistent log sink for the built-in Shizuku server.
 *
 * <p>Every log line that passes through {@link Logger#println} is kept in two places:
 * <ul>
 *   <li>an in-memory ring buffer (the latest {@link #MEM_MAX_LINES} lines), for fast binder reads;</li>
 *   <li>the on-disk file {@link #LOG_FILE}, which survives server restarts and is meant for
 *       troubleshooting cross-process/cross-restart issues such as "why did my config disappear"
 *       (logcat is lost on reboot or rotation).</li>
 * </ul>
 *
 * <p>The file lives in the shell user's de directory (same place as shizuku.json), so the server
 * can read/write it whether it runs as root or as shell; once it exceeds {@link #FILE_MAX_BYTES}
 * it is rotated once to {@code .1}, bounding disk usage.
 *
 * <p>This class is only used by the server process; the manager side reads {@link #LOG_FILE}
 * either through a binder transaction or directly via root.
 */
public final class ServerLog {

    private ServerLog() {
    }

    /** Same directory as shizuku.json; writable by the server whether it runs as root or shell. */
    public static final File LOG_DIR = new File("/data/user_de/0/com.android.shell");
    public static final File LOG_FILE = new File(LOG_DIR, "shizuku_folk.log");
    private static final File LOG_FILE_BACKUP = new File(LOG_DIR, "shizuku_folk.log.1");

    /** Max size of a single file; rotated once when exceeded. With the .1 backup, disk usage stays ≤ ~1 MiB. */
    private static final long FILE_MAX_BYTES = 512 * 1024;
    /** Max number of lines kept in the in-memory ring buffer. */
    private static final int MEM_MAX_LINES = 2000;
    /** Max bytes returned by a single binder transaction (to stay under the 1 MiB binder limit); returns that many bytes from the end of the file. */
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

    /** Called by {@link Logger#println}: writes one log line to the memory buffer and the disk file. */
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
            // A failed disk write must not affect server operation; the memory buffer still works.
        }
    }

    private static void openFileLocked() {
        try {
            if (!LOG_DIR.isDirectory()) {
                // The directory should always exist (it holds shizuku.json), but try to create it anyway.
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
     * Returns the persisted log text (at most the last {@link #DUMP_MAX_BYTES} bytes).
     * Prefers the disk file (includes history from before this boot); falls back to the memory
     * buffer when the file is unavailable.
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
                    // Drop the truncated first-line fragment
                    text = text.substring(nl + 1);
                }
                text = "…(older lines truncated)…\n" + text;
            }
            return text;
        } catch (Throwable e) {
            return null;
        }
    }

    /** Clears the memory buffer and the disk files. */
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
