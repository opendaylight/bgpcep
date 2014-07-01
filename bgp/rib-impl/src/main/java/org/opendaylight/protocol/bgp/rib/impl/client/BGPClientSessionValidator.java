/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.client;

import java.util.List;
import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates Bgp sessions established from current device to remote.
 */
public class BGPClientSessionValidator implements BGPSessionValidator {

    private static final Logger LOG = LoggerFactory.getLogger(BGPClientSessionValidator.class);

    private final AsNumber remoteAs;
    private final BGPSessionPreferences localPref;

    public BGPClientSessionValidator(final AsNumber remoteAs, final BGPSessionPreferences localPref) {
        this.remoteAs = remoteAs;
        this.localPref = localPref;
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
    public void validate(final Open openObj, final IpAddress address) throws BGPDocumentedException {
        final AsNumber as = AsNumberUtil.advertizedAsNumber(openObj);
        if (!this.remoteAs.equals(as)) {
            LOG.warn("Unexpected remote AS number. Expecting {}, got {}", this.remoteAs, as);
            throw new BGPDocumentedException("Peer AS number mismatch", BGPError.BAD_PEER_AS);
        }

        final List<BgpParameters> prefs = openObj.getBgpParameters();
        if (prefs != null && !prefs.isEmpty()) {
            if (!prefs.containsAll(this.localPref.getParams())) {
                LOG.info("BGP Open message session parameters differ, session still accepted.");
            }
        } else {
            throw new BGPDocumentedException("Open message unacceptable. Check the configuration of BGP speaker.", BGPError.UNSPECIFIC_OPEN_ERROR);
        }
    }
}
