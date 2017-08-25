/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;

public final class PSTUtil {
    private PSTUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Check whether Path is setup via RSVP-TE signaling protocol
     * @param pst
     * @return  true if setup is via RSVP-TE signaling protocol
     */
    public static boolean isDefaultPST(final PathSetupType pst) {
        if (pst != null && pst.getPst() != null && pst.getPst() != 0) {
            return false;
        }
        return true;
    }
}
