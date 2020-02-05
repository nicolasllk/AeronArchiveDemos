import io.aeron.*;
import io.aeron.Subscription;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.RecordingDescriptorConsumer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import java.util.concurrent.atomic.AtomicInteger;

import static io.aeron.archive.codecs.SourceLocation.LOCAL;

public class ArchivingTest {
    private static ArchivingMediaDriver archivingMediaDriver;
    private static Aeron aeron;
    private static AeronArchive aeronArchive;
    private static String aeronDirectoryName = CommonContext.generateRandomDirName();
    private static final int RECORDING_STREAM_ID = 33;
    private static final String RECORDING_CHANNEL = new ChannelUriStringBuilder()
            .media("udp")
            .endpoint("localhost:3333")
            .termLength(65536)
            .build();

    public static void main(String[] args) {

        archivingMediaDriver = ArchivingMediaDriver.launch(
                new MediaDriver.Context()
                        .aeronDirectoryName(aeronDirectoryName)
                        .termBufferSparseFile(true)
                        .threadingMode(ThreadingMode.SHARED)
                        .errorHandler(Throwable::printStackTrace)
                        .spiesSimulateConnection(true)
                        .dirDeleteOnShutdown(true)
                        .dirDeleteOnStart(true),
                new Archive.Context()
                        .maxCatalogEntries(1024)
                        .aeronDirectoryName(aeronDirectoryName)
                        .deleteArchiveOnStart(false)
                        .fileSyncLevel(0)
                        .threadingMode(ArchiveThreadingMode.SHARED));

        aeron = Aeron.connect(
                new Aeron.Context()
                        .aeronDirectoryName(aeronDirectoryName));

        aeronArchive = AeronArchive.connect(
                new AeronArchive.Context()
                        .aeron(aeron));

        final long subscriptionId = aeronArchive.startRecording(RECORDING_CHANNEL, RECORDING_STREAM_ID, LOCAL);

        try (Subscription subscription = aeron.addSubscription(RECORDING_CHANNEL, RECORDING_STREAM_ID);
             Publication publication = aeron.addPublication(RECORDING_CHANNEL, RECORDING_STREAM_ID))
        {

        }

        aeronArchive.stopRecording(subscriptionId);
        cleanup();
    }

    public static long[] listEmptyRecordings() {
        AtomicInteger index = new AtomicInteger();
        long[] emptyRecordings = new long[10];
        final RecordingDescriptorConsumer consumer =
                (controlSessionId,
                 correlationId,
                 recordingId,
                 startTimestamp,
                 stopTimestamp,
                 startPosition,
                 stopPosition,
                 initialTermId,
                 segmentFileLength,
                 termBufferLength,
                 mtuLength,
                 sessionId,
                 streamId,
                 strippedChannel,
                 originalChannel,
                 sourceIdentity) -> {
                    System.out.println("****************");
                    System.out.println(recordingId);
                    System.out.println(startPosition);
                    System.out.println(stopPosition);
                    System.out.println("*****************");
                    if (startPosition == 0 && stopPosition == 0) {
                        emptyRecordings[index.get()] = recordingId;
                        index.getAndIncrement();
                    }

                };

        aeronArchive.listRecordings(0L, 100, consumer);
        return emptyRecordings;
    }

    private static void cleanup() {
        long[] emptyRecordings = listEmptyRecordings();
        for (int i = 0 ; i < emptyRecordings.length ; i++) {
            System.out.println("Truncating recording ID: " + emptyRecordings[i]);
            aeronArchive.truncateRecording(emptyRecordings[i], 0L);
        }
    }
}
