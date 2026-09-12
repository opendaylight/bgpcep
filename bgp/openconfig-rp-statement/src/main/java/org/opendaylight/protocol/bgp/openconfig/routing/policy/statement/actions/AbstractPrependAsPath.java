/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedBytes;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;

@NonNullByDefault
abstract class AbstractPrependAsPath {

    static final Attributes prependAS(final Attributes attributes, final AsNumber as) {
        return new AttributesBuilder(attributes)
            .setAsPath(new AsPathBuilder().setSegments(prependAS(attributes.getAsPath().getSegments(), as)).build())
            .build();
    }

    private static List<Segments> prependAS(final @Nullable List<Segments> oldSegments, final AsNumber as) {
        if (oldSegments == null) {
            return prependAS0(as);
        }
        final var oldSize = oldSegments.size();
        return switch (oldSize) {
            case 0 -> prependAS0(as);
            /*
             * We need to check the first segment.
             * If it has as-set then new as-sequence with local AS is prepended.
             * If it has as-sequence, we may add local AS when it has less than 255 elements.
             * Otherwise we need to create new as-sequence for local AS.
             *
             * The logic is split into two methods to optimize instantiation.
             */
            case 1 -> prependAS1(as, oldSegments.getFirst());
            default -> {
                final var oldSegment = oldSegments.getFirst();
                final var oldAsSequence = oldSegment.getAsSequence();
                if (oldAsSequence == null) {
                    yield segmentsOf(as, oldSegments, oldSize);
                }
                final var oldSeqSize = oldAsSequence.size();
                yield oldSeqSize >= UnsignedBytes.MAX_VALUE ? segmentsOf(as, oldSegments, oldSize)
                    : ImmutableList.<Segments>builderWithExpectedSize(oldSize)
                        .add(new SegmentsBuilder()
                            .setAsSequence(ImmutableList.<AsNumber>builderWithExpectedSize(oldSeqSize + 1)
                                .add(as)
                                .addAll(oldAsSequence)
                                .build())
                            .build())
                        .addAll(oldSegments.subList(1, oldSize))
                        .build();
            }
        };
    }

    private static List<Segments> prependAS0(final AsNumber asn) {
        return ImmutableList.of(singleSequence(asn));
    }

    private static List<Segments> prependAS1(final AsNumber asn, final Segments oldSegment) {
        final var oldAsSequence = oldSegment.getAsSequence();
        if (oldAsSequence == null) {
            return segmentsOf(asn, oldSegment);
        }
        final var oldSeqSize = oldAsSequence.size();
        if (oldSeqSize >= UnsignedBytes.MAX_VALUE) {
            return segmentsOf(asn, oldSegment);
        }
        return ImmutableList.of(new SegmentsBuilder()
            .setAsSequence(ImmutableList.<AsNumber>builderWithExpectedSize(oldSeqSize + 1)
                .add(asn)
                .addAll(oldAsSequence)
                .build())
            .build());
    }

    private static List<Segments> segmentsOf(final AsNumber asn, final Segments oldSegment) {
        return List.of(singleSequence(asn), oldSegment);
    }


    private static List<Segments> segmentsOf(final AsNumber asn, final List<Segments> oldSegments, final int oldSize) {
        return ImmutableList.<Segments>builderWithExpectedSize(oldSize + 1)
            .add(singleSequence(asn))
            .addAll(oldSegments)
            .build();
    }

    private static Segments singleSequence(final AsNumber asn) {
        return new SegmentsBuilder().setAsSequence(ImmutableList.of(asn)).build();
    }
}
