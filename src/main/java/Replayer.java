import io.aeron.Aeron;
import io.aeron.ChannelUri;
import io.aeron.Subscription;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.RecordingDescriptorConsumer;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.samples.SampleConfiguration;
import org.agrona.collections.MutableLong;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SigInt;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Replayer {
    private static final int FRAGMENT_COUNT_LIMIT = SampleConfiguration.FRAGMENT_COUNT_LIMIT;

    public static void main(String[] args) {
        final int fragmentLimitCount = 10;
        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        // dataHandler method is called for every new datagram received
        final FragmentHandler fragmentHandler =
                (buffer, offset, length, header) ->
                {
                    final byte[] data = new byte[length];
                    buffer.getBytes(offset, data);
                    System.out.println("Received message " + new String(data));

                };



        final Aeron.Context ctx = new Aeron.Context();
        Aeron aeron = Aeron.connect(ctx);
        final AeronArchive.Context archiveCtx = new AeronArchive.Context()
                .errorHandler(Throwable::printStackTrace)
                .controlResponseStreamId(AeronArchive.Configuration.controlResponseStreamId() + 2)
                .aeron(aeron);

        try (AeronArchive archive = AeronArchive.connect(archiveCtx)) {
            final long recordingId = findLatestRecording(archive);
            final long position = 0L;
            final long length = Long.MAX_VALUE;

            final long sessionId = archive.startReplay(recordingId, position, length, Configurations.REPLAY_CHANNEL, Configurations.REPLAY_STREAM_ID);
            final String channel = ChannelUri.addSessionId(Configurations.REPLAY_CHANNEL, (int)sessionId);

            try (Subscription subscription = archive.context().aeron().addSubscription(channel, Configurations.REPLAY_STREAM_ID)) {
                final IdleStrategy idleStrategy = new BackoffIdleStrategy(
                        100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));

                while (!subscription.isConnected()) {
                    System.out.println("subscription not connected");
                }
                System.out.println(archive.getRecordingPosition(recordingId));
                // Try to read the data from subscriber
                while (running.get())
                {

                    final int fragmentsRead = subscription.poll(fragmentHandler, fragmentLimitCount);


                    idleStrategy.idle(fragmentsRead);
                }

                System.out.println("Shutting down...");
            }
        }


    }

    private static long findLatestRecording(final AeronArchive archive)
    {
        final MutableLong lastRecordingId = new MutableLong();

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
                 sourceIdentity) -> lastRecordingId.set(recordingId);

        final long fromRecordingId = 0L;
        final int recordCount = 100;

        final int foundCount = archive.listRecordingsForUri(fromRecordingId, recordCount, Configurations.ARCHIVE_CHANNEL, Configurations.ARCHIVE_STREAM_ID, consumer);
        if (foundCount == 0)
        {
            throw new IllegalStateException("no recordings found");
        }

        return lastRecordingId.get();
    }
}
