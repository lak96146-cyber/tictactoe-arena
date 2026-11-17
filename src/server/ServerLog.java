package server;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Standardized server-side logger providing timestamped messages for cloud
 * and headless Linux production environments.
 */
public class ServerLog {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static synchronized void info(String tag, String message) {
        String timestamp = SDF.format(new Date());
        System.out.println("[" + timestamp + "] [" + tag + "] " + message);
    }

    public static synchronized void warn(String tag, String message) {
        String timestamp = SDF.format(new Date());
        System.out.println("[" + timestamp + "] [" + tag + "] ⚠️  " + message);
    }

    public static synchronized void error(String tag, String message) {
        String timestamp = SDF.format(new Date());
        System.err.println("[" + timestamp + "] [" + tag + "] ❌ " + message);
    }
}
