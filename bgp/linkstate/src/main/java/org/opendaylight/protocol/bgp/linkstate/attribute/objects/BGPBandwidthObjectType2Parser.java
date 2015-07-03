/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.attribute.objects;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.bandwidth.object.BandwidthObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

public class BGPBandwidthObjectType2Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 5;
    public static final short CTYPE = 2;
    private static final Integer BODY_SIZE = 4;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final BandwidthObjectBuilder builder = new BandwidthObjectBuilder();
        builder.setCType(CTYPE);
        builder.setClassNum(CLASS_NUM);
        builder.setBandwidth(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BandwidthObject, "BandwidthObject is mandatory.");
        final BandwidthObject bandObject = (BandwidthObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, bandObject.getClassNum(), bandObject.getCType(), output);
        writeFloat32(bandObject.getBandwidth(), output);
    }
}
