import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.ShutdownSignalBarrier;

public class MediaDriverManager {
    private static MediaDriver.Context mediaDriverContext;

    public static void main(String[] args) {
        initDriverContext();

        System.out.println("Starting Media Driver...");
        try (MediaDriver ignored = MediaDriver.launch(mediaDriverContext)) {
            if (null != ignored) {
                System.out.println("Media Driver started");
            }
            new ShutdownSignalBarrier().await();

            System.out.println("Shutdown Driver...");
        }
    }

    private static void initDriverContext() {
        mediaDriverContext = new MediaDriver.Context()
                .errorHandler(Throwable::printStackTrace)
                .spiesSimulateConnection(true)
                .dirDeleteOnStart(false)
                .dirDeleteOnShutdown(false)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(false)
                .spiesSimulateConnection(true);
    }
}
