/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.open.CapabilitySerializerHandler;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates Bgp sessions established from current device to remote.
 */
public class BGPClientSessionValidator implements BGPSessionValidator {

    private static final Logger LOG = LoggerFactory.getLogger(BGPClientSessionValidator.class);

    private final AsNumber remoteAs;

    public BGPClientSessionValidator(final AsNumber remoteAs) {
        this.remoteAs = remoteAs;
    }

    /**
     * Validates with exception:
     * <ul>
     * <li>correct remote AS attribute</li>
     * <li>non empty BgpParameters collection</li>
     * </ul>
     *
     * Validates with log message:
     * <ul>
     * <li>local BgpParameters are superset of remote BgpParameters</li>
     * </ul>
     */
    @Override
    public void validate(final Open openObj, final BGPSessionPreferences localPref) throws BGPDocumentedException {
        final AsNumber as = AsNumberUtil.advertizedAsNumber(openObj);
        if (!this.remoteAs.equals(as)) {
            LOG.warn("Unexpected remote AS number. Expecting {}, got {}", this.remoteAs, as);
            throw new BGPDocumentedException("Peer AS number mismatch", BGPError.BAD_PEER_AS);
        }
        //https://tools.ietf.org/html/rfc6286#section-2.2
        if (openObj.getBgpIdentifier() != null &&  openObj.getBgpIdentifier().equals(localPref.getBgpId())) {
            LOG.warn("Remote and local BGP Identifiers are the same: {}", openObj.getBgpIdentifier());
            throw new BGPDocumentedException("Remote and local BGP Identifiers are the same.", BGPError.BAD_BGP_ID);
        }

        final List<BgpParameters> prefs = openObj.getBgpParameters();
        if (prefs != null) {
            As4BytesCapability localAS4Capa = getAs4BytesCapability(localPref.getParams());
            if(localAS4Capa != null && getAs4BytesCapability(prefs) == null) {
                throw new BGPDocumentedException("The peer must advertise AS4Bytes capability.", BGPError.UNSUPPORTED_CAPABILITY,
                        serializeAs4BytesCapability(localAS4Capa));
            }
            if (!prefs.containsAll(localPref.getParams())) {
                LOG.info("BGP Open message session parameters differ, session still accepted.");
            }
        } else {
            throw new BGPDocumentedException("Open message unacceptable. Check the configuration of BGP speaker.", BGPError.UNSPECIFIC_OPEN_ERROR);
        }
    }

    private static As4BytesCapability getAs4BytesCapability(final List<BgpParameters> prefs) {
        for(final BgpParameters param : prefs) {
            for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                final CParameters cParam = capa.getCParameters();
                if(cParam.getAs4BytesCapability() !=null) {
                    return cParam.getAs4BytesCapability();
                }
            }
        }
        return null;
    }

    private static byte[] serializeAs4BytesCapability(final As4BytesCapability as4Capability) {
        final ByteBuf buffer = Unpooled.buffer(1 /*CODE*/ + 1 /*LENGTH*/ + Integer.SIZE / Byte.SIZE /*4 byte value*/);
        final CapabilitySerializer serializer = new CapabilitySerializerHandler();
        serializer.serializeCapability(new CParametersBuilder().setAs4BytesCapability(as4Capability).build(), buffer);
        return buffer.array();
    }
}
