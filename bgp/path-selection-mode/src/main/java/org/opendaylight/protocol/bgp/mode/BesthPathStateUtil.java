/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;

public final class BesthPathStateUtil {
    private BesthPathStateUtil() {
        // Hidden on purpose
    }

    public static long getPeerAs(final List<Segments> segments) {
        for (final Segments seg : segments) {
            if (seg.getAsSequence() != null && !seg.getAsSequence().isEmpty()) {
                return segments.get(0).getAsSequence().get(0).getValue().toJava();
            }
        }
        return 0;
    }
}
