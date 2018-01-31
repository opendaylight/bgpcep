/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public final class ExportAttributeTestUtil {
    public static final QName ATTRS_EXTENSION_Q = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2017-12-07", "attributes");
    public static final QName AS_NUMBER_Q = QName.create(ATTRS_EXTENSION_Q, "as-number");
    public static final NodeIdentifier SEGMENTS_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            Segments.QNAME.getLocalName()));
    public static final NodeIdentifier SEQ_LEAFLIST_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            "as-sequence"));
    public static final UnkeyedListEntryNode SEQ_SEGMENT = Builders.unkeyedListEntryBuilder()
            .withNodeIdentifier(SEGMENTS_NID).addChild(Builders.orderedLeafSetBuilder()
                    .withNodeIdentifier(SEQ_LEAFLIST_NID)
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 1L)).withValue(1L).build())
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 2L)).withValue(2L).build())
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 3L)).withValue(3L).build())
                    .build()).build();
    public static final long LOCAL_AS = 8;
    public static final Ipv4Address IPV4 = new Ipv4Address("1.2.3.4");
    public static final ClusterIdentifier CLUSTER = new ClusterIdentifier(IPV4);
    private static final NodeIdentifier SET_LEAFLIST_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            "as-set"));
    public static final UnkeyedListEntryNode SET_SEGMENT = Builders.unkeyedListEntryBuilder()
            .withNodeIdentifier(SEGMENTS_NID).addChild(Builders.leafSetBuilder().withNodeIdentifier(SET_LEAFLIST_NID)
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 10L)).withValue(10L).build())
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 11L)).withValue(11L).build())
                    .build()).build();

    private ExportAttributeTestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * container cluster-id. {
     * leaf-list cluster {
     * type cluster-identifier;
     * }
     * uses cluster-id;
     * container originator-id {
     * leaf originator {
     * type ipv4-address;
     * }
     * uses originator-id;
     * }
     */
    public static Attributes createInputWithOriginator() {
        return new AttributesBuilder().setClusterId(createClusterId()).setOriginatorId(createOriginatorId()).build();
    }

    public static Attributes createClusterInput() {
        return new AttributesBuilder().setClusterId(createClusterIdInput()).build();
    }

    private static ClusterId createClusterId() {
        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return new ClusterIdBuilder().setCluster(Arrays.asList(CLUSTER, cluster1, cluster2)).build();
    }

    private static ClusterId createClusterIdInput() {
        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return new ClusterIdBuilder().setCluster(Arrays.asList(cluster1, cluster2)).build();
    }

    private static OriginatorId createOriginatorId() {
        return new OriginatorIdBuilder().setOriginator(CLUSTER).build();
    }

    /**
     * AsPath.
     */
    static Attributes createPathInputWithAs() {
        return createPathInput(createSequenceWithLocalAs());
    }

    static Attributes createPathInput(final List<Segments> segB) {
        return new AttributesBuilder().setAsPath(new AsPathBuilder().setSegments(segB).build()).build();
    }

    private static List<Segments> createSequenceWithLocalAs() {
        return Collections.singletonList(new SegmentsBuilder()
                .setAsSequence(Collections.singletonList(new AsNumber(LOCAL_AS))).build());
    }
}
