/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.open.As4CapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.As4BytesCase;
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

        final List<BgpParameters> prefs = openObj.getBgpParameters();
        if (prefs != null && !prefs.isEmpty()) {
            if(getAs4BytesCapability(localPref.getParams()).isPresent() && !getAs4BytesCapability(prefs).isPresent()) {
                throw new BGPDocumentedException("The peer must advertise AS4Bytes capability.", BGPError.UNSUPPORTED_CAPABILITY,
                        serializeAs4BytesCapability(getAs4BytesCapability(localPref.getParams()).get()));
            }
            if (!prefs.containsAll(localPref.getParams())) {
                LOG.info("BGP Open message session parameters differ, session still accepted.");
            }
        } else {
            throw new BGPDocumentedException("Open message unacceptable. Check the configuration of BGP speaker.", BGPError.UNSPECIFIC_OPEN_ERROR);
        }
    }

    private static Optional<As4BytesCase> getAs4BytesCapability(final List<BgpParameters> prefs) {
        for(final BgpParameters param : prefs) {
            for (final OptionalCapabilities capa : param.getOptionalCapabilities()) {
                if(capa.getCParameters() instanceof As4BytesCase) {
                    return Optional.of((As4BytesCase) capa.getCParameters());
                }
            }
        }
        return Optional.absent();
    }

    private static byte[] serializeAs4BytesCapability(final As4BytesCase as4Capability) {
        final ByteBuf buffer = Unpooled.buffer(1 /*CODE*/ + 1 /*LENGTH*/ + Integer.SIZE / Byte.SIZE /*4 byte value*/);
        final CapabilitySerializer serializer = new As4CapabilityHandler();
        serializer.serializeCapability(as4Capability, buffer);
        return buffer.array();
    }
}
