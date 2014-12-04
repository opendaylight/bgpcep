/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.SrEroSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

public final class SrEroUtil {

    private static final int MPLS_LABEL_MIN_VALUE = 16;

    private SrEroUtil() {
        throw new UnsupportedOperationException();
    }

    protected static PCEPErrors validateSrEroSubobjects(final Ero ero) {
        if (ero.getSubobject() != null && !ero.getSubobject().isEmpty()) {
            for (final Subobject subobject : ero.getSubobject()) {
                if (!(subobject.getSubobjectType() instanceof SrEroSubobject)) {
                    return PCEPErrors.NON_IDENTICAL_ERO_SUBOBJECTS;
                }
                final SrEroSubobject srEroSubobject = (SrEroSubobject) subobject.getSubobjectType();
                if (srEroSubobject.getFlags() != null && srEroSubobject.getFlags().isM() && srEroSubobject.getSid() < MPLS_LABEL_MIN_VALUE) {
                    return PCEPErrors.BAD_LABEL_VALUE;
                }
            }
        }
        return null;
    }

    protected static boolean isSegmentRoutingPath(final Srp srp) {
        if (srp != null && srp.getTlvs() != null && isSrTePst(srp.getTlvs().getPathSetupType())) {
            return true;
        }
        return false;
    }

    protected static boolean isSegmentRoutingPath(final Rp rp) {
        if (rp != null && rp.getTlvs() != null && isSrTePst(rp.getTlvs().getPathSetupType())) {
            return true;
        }
        return false;
    }

    protected static boolean isSegmentRoutingPath(final Ero ero) {
        if (ero != null && ero.getSubobject() != null && !ero.getSubobject().isEmpty()) {
            for (final Subobject subobject : ero.getSubobject()) {
                if (!(subobject.getSubobjectType() instanceof SrEroSubobject)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isSrTePst(final PathSetupType tlv) {
        if (tlv != null && tlv.getPst() == 1) {
            return true;
        }
        return false;
    }

}
