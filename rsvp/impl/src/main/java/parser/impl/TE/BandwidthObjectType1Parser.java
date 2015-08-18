package parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.BandwidthObjectBuilder;
import parser.spi.RSVPParsingException;

public final class BandwidthObjectType1Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 5;
    public static final short CTYPE = 1;
    private static final Integer BODY_SIZE = 4;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        return parseBody(CTYPE,byteBuf);
    }

    protected static RsvpTeObject parseBody(final short ctype, final ByteBuf byteBuf) {
        final BandwidthObjectBuilder builder = new BandwidthObjectBuilder();
        builder.setCType(ctype);
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        return builder.build();
    }


    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BandwidthObject, "BandwidthObject is mandatory.");
        final BandwidthObject bandObject = (BandwidthObject) teLspObject;
        serialize(bandObject, bandObject.getCType(), output);
    }

    protected static void serialize(final BandwidthObject bandObject, final Short cType, final ByteBuf output) {
        serializeAttributeHeader(BODY_SIZE, CLASS_NUM, cType, output);
        final Bandwidth band = bandObject.getBandwidth();
        output.writeBytes(Unpooled.wrappedBuffer(band.getValue()));
    }
}
