/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.SegmentsBuilder;

class AbstractPrependAsPath {

    static final Attributes prependAS(final Attributes attributes, final AsNumber as) {
        final List<Segments> oldSegments = attributes.getAsPath().getSegments();
        /*
         * We need to check the first segment.
         * If it has as-set then new as-sequence with local AS is prepended.
         * If it has as-sequence, we may add local AS when it has less than 255 elements.
         * Otherwise we need to create new as-sequence for local AS.
         */
        final ArrayList<AsNumber> newAsSequence = new ArrayList<>();
        newAsSequence.add(new AsNumber(as));

        List<Segments> newSegments = new ArrayList<>();
        if (oldSegments == null || oldSegments.isEmpty()) {
            newSegments = new ArrayList<>();
            newSegments.add(new SegmentsBuilder().setAsSequence(newAsSequence).build());
        } else {
            final Segments firstSegment = oldSegments.remove(0);
            final List<AsNumber> firstAsSequence = firstSegment.getAsSequence();
            if (firstAsSequence != null && firstAsSequence.size() < Values.UNSIGNED_BYTE_MAX_VALUE) {
                newAsSequence.addAll(firstAsSequence);
                newSegments.add(new SegmentsBuilder().setAsSequence(newAsSequence).build());
            } else {
                newSegments.add(new SegmentsBuilder().setAsSequence(newAsSequence).build());
                newSegments.add(firstSegment);
            }
            newSegments.addAll(oldSegments);
        }
        return new AttributesBuilder(attributes).setAsPath(new AsPathBuilder()
                .setSegments(newSegments).build()).build();
    }
}
