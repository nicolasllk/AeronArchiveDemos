import io.aeron.ChannelUriStringBuilder;
import io.aeron.samples.SampleConfiguration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Configurations {
    public static final int PUBLISHER_STREAM_ID = 30;
//    public static final String PUBLISHER_CHANNEL = "aeron:udp?control=localhost:2020|control-mode=dynamic|session-id=123456789";
    public static final String PUBLISHER_CHANNEL = "aeron:udp?endpoint=224.0.1.1:40456|session-id=123456789";

    public static final int SUBSCRIBER_STREAM_ID = SampleConfiguration.STREAM_ID;
//    public static final String SUBSCRIBER_CHANNEL = "aeron:udp?endpoint=localhost:2021|control=localhost:2020|control-mode=dynamic";
    public static final String SUBSCRIBER_CHANNEL = "aeron:udp?endpoint=224.0.1.1:40456";

    public static final int ARCHIVE_STREAM_ID = PUBLISHER_STREAM_ID;
//    public static final String ARCHIVE_CHANNEL = "aeron:udp?control=localhost:2020";
    public static final String ARCHIVE_CHANNEL = "aeron:udp?endpoint=224.0.1.1:40456";

    public static final int REPLAY_STREAM_ID = 40;
    public static final String REPLAY_CHANNEL = "aeron:udp?endpoint=localhost:2022|session-id=123456789";

    public static final String REPLAY_DESTINATION = "aeron:udp?endpoint=localhost:2022";

    public static final int SPY_STREAM_ID = 30;
    public static final String SPY_CHANNEL = "aeron-spy:aeron:udp?control=224.0.1.1:2020";

    private static final int RECORDING_STREAM_ID = 33;
    private static final String RECORDING_CHANNEL = new ChannelUriStringBuilder()
            .media("udp")
            .endpoint("localhost:2020")
            .build();

    private static String generateSessionID() {
        LocalDate today = LocalDate.now();
        return "|session-id=" + DateTimeFormatter.ofPattern("yyyyMMdd").format(today);
    }
}
