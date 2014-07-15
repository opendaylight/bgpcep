/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;

/**
 * Validates Bgp sessions established from remote devices.
 */
public final class BGPServerSessionValidator implements BGPSessionValidator {

    @Override
    public void validate(final Open openObj, final BGPSessionPreferences localPref) throws BGPDocumentedException {
        // No validation performed, org.opendaylight.protocol.bgp.rib.impl.AbstractBGPSessionNegotiator checks if peer is configured
    }
}
