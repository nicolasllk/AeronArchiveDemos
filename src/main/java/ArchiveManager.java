import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.codecs.SourceLocation;
import org.agrona.concurrent.ShutdownSignalBarrier;

public class ArchiveManager {
    private static Archive.Context archiveContext;
    private static AeronArchive aeronArchive;
    private static AeronArchive.Context aeronArchiveContext;

    public static void main(String[] args) {

        initArchiveContext();
        System.out.println("Starting Archive...");
        if (null == Archive.launch(archiveContext)){
            System.out.println("Failed to start Archive service");
        }

        init();

    }

    private static void initArchiveContext() {
        archiveContext = new Archive.Context()
                .deleteArchiveOnStart(false)
                .fileSyncLevel(0)
                .catalogFileSyncLevel(0)
                .threadingMode(ArchiveThreadingMode.SHARED)
                .errorHandler(Throwable::printStackTrace);

        aeronArchiveContext = new AeronArchive.Context()
                .errorHandler(Throwable::printStackTrace)
                .controlResponseStreamId(AeronArchive.Configuration.controlResponseStreamId() + 1);
    }

    public static void init() {
        initAeronArchiveContext();
        aeronArchive = AeronArchive.connect(aeronArchiveContext);
        aeronArchive.startRecording(Configurations.ARCHIVE_CHANNEL, Configurations.ARCHIVE_STREAM_ID, SourceLocation.LOCAL);
    }

    private static void initAeronArchiveContext() {
        aeronArchiveContext = new AeronArchive.Context()
                .controlResponseStreamId(AeronArchive.Configuration.controlResponseStreamId() + 1)
                .errorHandler(Throwable::printStackTrace);
    }

}
