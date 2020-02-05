import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.RecordingDescriptorConsumer;
import io.aeron.archive.codecs.SourceLocation;
import io.aeron.archive.status.RecordingPos;
import org.agrona.BufferUtil;
import org.agrona.collections.MutableLong;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.status.CountersReader;

import java.util.concurrent.atomic.AtomicBoolean;

public class Publisher {
    private static final UnsafeBuffer BUFFER = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));
    public static void main(String[] args) throws InterruptedException {
        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        Aeron.Context aeronContext = new Aeron.Context()
                .errorHandler(Throwable::printStackTrace);

        try(Aeron aeron = Aeron.connect(aeronContext)){
            try (Publication publication = aeron.addPublication(Configurations.PUBLISHER_CHANNEL, Configurations.PUBLISHER_STREAM_ID)) {
                publication.sessionId();
                for (long i = 0; i < 10000; i++) {
                    final byte[] messageBytes = Long.toString(i).getBytes();
                    BUFFER.putBytes(0, messageBytes);
                    System.out.println("Offering " + i);

                    final long result = publication.offer(BUFFER, 0, messageBytes.length);

                    if (result < 0L) {
                        if (result == Publication.BACK_PRESSURED) {
                            System.out.println("Offer failed due to back pressure");
                        } else if (result == Publication.NOT_CONNECTED) {
                            System.out.println("Offer failed because publisher is not connected to subscriber");
                        } else if (result == Publication.ADMIN_ACTION) {
                            System.out.println("Offer failed because of an administration action in the system");
                        } else if (result == Publication.CLOSED) {
                            System.out.println("Offer failed publication is closed");
                            break;
                        } else if (result == Publication.MAX_POSITION_EXCEEDED) {
                            System.out.println("Offer failed due to publication reaching max position");
                            break;
                        } else {
                            System.out.println("Offer failed due to unknown reason");
                        }
                    } else {
                        System.out.println("yay!");
                    }

                    if (!publication.isConnected()) {
                        System.out.println("No active subscribers detected");
                    }

                    Thread.sleep(1000);
                }
            }
       }
    }
}
