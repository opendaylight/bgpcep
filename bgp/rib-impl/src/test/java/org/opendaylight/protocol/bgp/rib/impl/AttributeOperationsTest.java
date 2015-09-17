/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Iterator;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class AttributeOperationsTest {

    static final NodeIdentifier ORIGIN_NID = new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, Origin.QNAME.getLocalName())));
    static final NodeIdentifier ORIGIN_VALUE_NID = new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "value")));
    static final NodeIdentifier AS_PATH_NID = new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, AsPath.QNAME.getLocalName())));
    static final NodeIdentifier ATOMIC_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, AtomicAggregate.QNAME.getLocalName()));

    @Test
    public void testExportedAttributesSetFirst() {
        final Long ourAs = 72L;
        final ContainerNode attributesSetBefore = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID)
                    .addChild(BestPathSelectorTest.SET_SEGMENT)
                    .addChild(BestPathSelectorTest.SEQ_SEGMENT)
                    .build())
            .build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributesSetBefore);
        final ContainerNode exportedAttributes = operations.exportedAttributes(attributesSetBefore, ourAs);

        // make sure our AS is prepended to the list (as the AS-PATH starts with AS-SET)
        final LeafSetNode<?> list = checkFirstLeafList(exportedAttributes);
        assertEquals(ourAs, list.getValue().iterator().next().getValue());
    }

    @Test
    public void testExportedAttributesListFirst() {
        final Long ourAs = 72L;
        final ContainerNode attributesListBefore = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID)
                    .addChild(BestPathSelectorTest.SEQ_SEGMENT)
                    .addChild(BestPathSelectorTest.SET_SEGMENT)
                    .build())
            .build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributesListBefore);
        final ContainerNode exportedAttributes = operations.exportedAttributes(attributesListBefore, ourAs);

        // make sure our AS is appended to the a-list (as the AS-PATH starts with A-LIST)
        final LeafSetNode<?> list = checkFirstLeafList(exportedAttributes);
        final Iterator<?> iter = list.getValue().iterator();
        assertEquals(ourAs, ((LeafSetEntryNode<?>)iter.next()).getValue());
        assertEquals(1L, ((LeafSetEntryNode<?>)iter.next()).getValue());
        assertEquals(2L, ((LeafSetEntryNode<?>)iter.next()).getValue());
        assertEquals(3L, ((LeafSetEntryNode<?>)iter.next()).getValue());
    }

    @Test
    public void testExportedAttributesEmptyWithTransitive() {
        final Long ourAs = 72L;
        final ContainerNode attributesSetBefore = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(ORIGIN_NID)
                .addChild(Builders.leafBuilder().withNodeIdentifier(ORIGIN_VALUE_NID).withValue(BgpOrigin.Egp).build())
            .build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID).build())
            .build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(ATOMIC_NID).build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributesSetBefore);
        final ContainerNode exportedAttributes = operations.exportedAttributes(attributesSetBefore, ourAs);

        // Origin should be within exportedAttributes as it is Transitive
        assertTrue(exportedAttributes.getChild(ORIGIN_NID).isPresent());

        // AS-PATH should also be there with our AS
        final LeafSetNode<?> list = checkFirstLeafList(exportedAttributes);
        assertEquals(1, list.getValue().size());
        assertEquals(ourAs, list.getValue().iterator().next().getValue());

        // Atomic Aggregate should be filtered out
        assertFalse(exportedAttributes.getChild(ATOMIC_NID).isPresent());
    }

    private LeafSetNode<?> checkFirstLeafList(final ContainerNode exportedAttributes) {
        assertTrue(NormalizedNodes.findNode(exportedAttributes, AS_PATH_NID, BestPathSelectorTest.SEGMENTS_NID).isPresent());
        final UnkeyedListNode segments = (UnkeyedListNode) NormalizedNodes.findNode(exportedAttributes, AS_PATH_NID, BestPathSelectorTest.SEGMENTS_NID).get();
        final UnkeyedListEntryNode seg = segments.getValue().iterator().next();
        final DataContainerChild<? extends PathArgument, ?> firstLeafList = seg.getValue().iterator().next();
        return (LeafSetNode<?>) firstLeafList;
    }
}
