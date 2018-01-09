/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;

public final class BesthPathStateUtil {
    BesthPathStateUtil() {
        throw new UnsupportedOperationException();
    }

    public static AsNumber getPeerAs(final List<Segments> segments) {
        if (segments.isEmpty()) {
            return new AsNumber(0L);
        }
        for (final Segments seg : segments) {
            if (seg.getAsSequence() != null && !seg.getAsSequence().isEmpty()) {
                return segments.get(0).getAsSequence().get(0);
            }
        }
        return new AsNumber(0L);
    }
}
