import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Enumeration;
import java.util.Locale;

/**
 * This class monitors the uptime of the network connection. It continuously checks the connection and logs when
 * it's dropped.
 * When the connection is restored, it logs the restoration time and the duration of the outage.
 * <p>
 * The log file contains entries with timestamps of when the connection was lost and restored,
 * along with the duration of each outage. This information can be useful for diagnosing
 * network issues and understanding connection stability over time.
 * </p>
 * <p>
 * The program runs indefinitely until manually terminated.
 * </p>
 *
 * @author Tensenrin
 * @version 2.0
 * @since 2024-06-09
 *
 */
public class ConnectionMonitor {

    // Attributes
    private static final String NO_CONN_MSG = "Connection dropped at: %s on %s";
    private static final String RESTORED_CONN_MSG = "Connection restored at: %s on %s. Outage duration: %d seconds";
    private static final String NEW_DAY_MSG = "%s, %d %s, %d";

    /**
     * Checks the interface status for each network interface
     *
     * @return {@code true} if the connection is available, {@code false} if not.
     */
    public static boolean isOnline() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isUp() && !iface.isLoopback() && iface.getInterfaceAddresses().size() > 0) {
                    return true;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Logs a message to the log file.
     *
     * @param file the log file
     * @param message the message.
     */
    public static void logEvent(File file, String message) {
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(file, true))) {
            bf.write(message);
            bf.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs the start of the day.
     *
     * @param file the file to log the message to
     * @param currentDate the current date to log
     */
    public static void logNewDay(File file, LocalDate currentDate) {
        String dayOfWeek = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String month = currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        int dayOfMonth = currentDate.getDayOfMonth();
        int year = currentDate.getYear();
        String newDayMessage = String.format(NEW_DAY_MSG, dayOfWeek, dayOfMonth, month, year);
        logEvent(file, newDayMessage);
    }

    public static void main(String[] args) {
        File file = new File("log.txt");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime dropTime = null;
        LocalDate lastLoggedDate = null;
        LocalDate currentDate = LocalDate.now();

        while (true) {

            // checking if it's a new day
            if (!currentDate.equals(lastLoggedDate)) {
                logNewDay(file, currentDate);
                lastLoggedDate = currentDate;
            }

            // checking if  the connection is out
            if (!isOnline()) {
                if (dropTime == null) {
                    dropTime = LocalDateTime.now();
                    String formattedTime = dropTime.format(timeFormatter);
                    String formattedDate = dropTime.format(dateFormatter);
                    String logMessage = String.format(NO_CONN_MSG, formattedTime, formattedDate);
                    logEvent(file, logMessage);
                    System.out.println(logMessage);
                }
            } else {
                if (dropTime != null) {
                    LocalDateTime restoreTime = LocalDateTime.now();
                    Duration outageDuration = Duration.between(dropTime, restoreTime);
                    long secondsOut = outageDuration.getSeconds();
                    String formattedRestoreTime = restoreTime.format(timeFormatter);
                    String formattedRestoreDate = restoreTime.format(dateFormatter);
                    String logMessage = String.format(RESTORED_CONN_MSG, formattedRestoreTime, formattedRestoreDate, secondsOut);
                    logEvent(file, logMessage);
                    System.out.println(logMessage);
                    dropTime = null;
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}