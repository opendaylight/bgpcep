package parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.BandwidthObject;
import parser.spi.RSVPParsingException;

public final class BandwidthObjectType2Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 5;
    public static final short CTYPE = 2;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        return BandwidthObjectType1Parser.parseBody(CTYPE, byteBuf);
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BandwidthObject, "BandwidthObject is mandatory.");
        final BandwidthObject bandObject = (BandwidthObject) teLspObject;
        BandwidthObjectType1Parser.serialize(bandObject, bandObject.getCType(), output);
    }
}