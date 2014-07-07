/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.PathSetupTypeTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.SrEroSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;

public final class SrEroUtil {

    private static final int MPLS_LABEL_MIN_VALUE = 16;

    private SrEroUtil() {
    }

    protected static PCEPErrors validateSrEroSubobjects(final Ero ero) {
        if (ero.getSubobject() == null || ero.getSubobject().isEmpty()) {
            return null;
        }
        for (final Subobject subobject : ero.getSubobject()) {
            if (!(subobject.getSubobjectType() instanceof SrEroSubobject)) {
                return PCEPErrors.NON_IDENTICAL_ERO_SUBOBJECTS;
            }
            final SrEroSubobject srEroSubobject = (SrEroSubobject) subobject.getSubobjectType();
            if (srEroSubobject.getFlags().isM() && srEroSubobject.getSid() < MPLS_LABEL_MIN_VALUE) {
                return PCEPErrors.BAD_LABEL_VALUE;
            }
        }
        return null;
    }

    protected static boolean isPst(final PathSetupTypeTlv tlv) {
        if (tlv != null && tlv.getPathSetupType() != null && tlv.getPathSetupType().isPst()) {
            return true;
        }
        return false;
    }

}
