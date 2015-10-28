/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;

/**
 * Util class for encoding/decoding 20bit leftmost value.
 */
public final class MplsLabelUtil {

    private static final int LABEL_OFFSET = 4;

    private MplsLabelUtil() {
        throw new UnsupportedOperationException();
    }

    public static MplsLabel mplsLabelForInt(final int value) {
        return new MplsLabel(new Long(value >> LABEL_OFFSET));
    }

    public static int intForMplsLabel(final MplsLabel label) {
        return label.getValue().intValue() << LABEL_OFFSET;
    }

}
