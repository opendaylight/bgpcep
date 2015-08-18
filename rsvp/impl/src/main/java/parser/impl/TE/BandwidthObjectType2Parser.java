package parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.bandwidth.object.BandwidthObjectBuilder;
import parser.spi.RSVPParsingException;

public final class BandwidthObjectType2Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 5;
    public static final short CTYPE = 2;
    private static final Integer BODY_SIZE = 4;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final BandwidthObjectBuilder builder = new BandwidthObjectBuilder();
        builder.setCType(CTYPE);
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BandwidthObject, "BandwidthObject is mandatory.");
        final BandwidthObject bandObject = (BandwidthObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, CLASS_NUM, CTYPE, output);
        final Bandwidth band = bandObject.getBandwidth();
        output.writeBytes(Unpooled.wrappedBuffer(band.getValue()));
    }
}