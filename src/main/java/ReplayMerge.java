import io.aeron.*;
import io.aeron.Subscription;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.RecordingDescriptorConsumer;
import io.aeron.logbuffer.FragmentHandler;

public class ReplayMerge {
    private static Aeron aeron;
    private static AeronArchive aeronArchive;
    static final int FRAGMENT_LIMIT = 10;

    public static void main(String[] args) {
        aeron = Aeron.connect(
                new Aeron.Context()
                        .errorHandler(Throwable::printStackTrace));

        final AeronArchive.Context archiveCtx = new AeronArchive.Context()
                .errorHandler(Throwable::printStackTrace)
                .controlResponseStreamId(AeronArchive.Configuration.controlResponseStreamId() + 2)
                .aeron(aeron);

//        aeronArchive.context().aeron().addSubscription();
        aeronArchive = AeronArchive.connect(archiveCtx);

        aeronArchive.listRecordings(0, 100, new RecordingDescriptorConsumer() {
            @Override
            public void onRecordingDescriptor(long controlSessionId, long correlationId, long recordingId, long startTimestamp, long stopTimestamp, long startPosition, long stopPosition, int initialTermId, int segmentFileLength, int termBufferLength, int mtuLength, int sessionId, int streamId, String strippedChannel, String originalChannel, String sourceIdentity) {
                System.out.println(recordingId);
            }
        });

        ChannelUriStringBuilder subscriptionChannelBuilder = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL);

        String subscriptionChannel = subscriptionChannelBuilder.sessionId(123456789).build();

        // dataHandler method is called for every new datagram received
        final FragmentHandler fragmentHandler =
                (buffer, offset, length, header) ->
                {
                    final byte[] data = new byte[length];
                    buffer.getBytes(offset, data);
                    System.out.println("Received message " + new String(data));

                };

        try (Subscription subscription = aeron.addSubscription(subscriptionChannel, 30) ) {

            io.aeron.archive.client.ReplayMerge replayMerge = new io.aeron.archive.client.ReplayMerge(
                    subscription,
                    aeronArchive,
                    Configurations.REPLAY_CHANNEL,
                    Configurations.REPLAY_DESTINATION,
                    Configurations.PUBLISHER_CHANNEL,
                    0,
                    0);

            while (!replayMerge.isMerged()) {
                replayMerge.poll(fragmentHandler, FRAGMENT_LIMIT);

            }

            System.out.println("MERGED!");
        }

    }
}
