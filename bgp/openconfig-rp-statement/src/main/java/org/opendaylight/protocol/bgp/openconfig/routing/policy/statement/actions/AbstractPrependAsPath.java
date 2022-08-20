/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;

abstract class AbstractPrependAsPath {

    static final Attributes prependAS(final Attributes attributes, final AsNumber as) {
        return new AttributesBuilder(attributes)
            .setAsPath(new AsPathBuilder()
                .setSegments(prependAS(attributes.getAsPath().getSegments(), as))
                .build())
            .build();
    }

    private static List<Segments> prependAS(final List<Segments> oldSegments, final AsNumber as) {
        if (oldSegments == null || oldSegments.isEmpty()) {
            return ImmutableList.of(singleSequence(as));
        }

        /*
         * We need to check the first segment.
         * If it has as-set then new as-sequence with local AS is prepended.
         * If it has as-sequence, we may add local AS when it has less than 255 elements.
         * Otherwise we need to create new as-sequence for local AS.
         */
        final Iterator<Segments> it = oldSegments.iterator();
        final Segments firstSegment = it.next();
        final List<AsNumber> firstAsSequence = firstSegment.getAsSequence();

        final ImmutableList.Builder<Segments> newSegments;
        if (firstAsSequence != null && firstAsSequence.size() < Values.UNSIGNED_BYTE_MAX_VALUE) {
            newSegments = ImmutableList.<Segments>builderWithExpectedSize(oldSegments.size())
                .add(new SegmentsBuilder()
                    .setAsSequence(ImmutableList.<AsNumber>builderWithExpectedSize(firstAsSequence.size() + 1)
                        .add(as)
                        .addAll(firstAsSequence)
                        .build())
                    .build());
        } else {
            newSegments = ImmutableList.<Segments>builderWithExpectedSize(oldSegments.size() + 1)
                .add(singleSequence(as))
                .add(firstSegment);
        }

        it.forEachRemaining(newSegments::add);
        return newSegments.build();
    }

    private static Segments singleSequence(final AsNumber as) {
        return new SegmentsBuilder().setAsSequence(ImmutableList.of(as)).build();
    }
}
