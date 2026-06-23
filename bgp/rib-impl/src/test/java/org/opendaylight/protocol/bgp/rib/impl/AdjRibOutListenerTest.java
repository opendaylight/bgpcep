/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;

/**
 * Covers {@link AdjRibOutListener}'s handling of an in-place route replacement: when the RIB support reports that the
 * on-wire NLRI identity changed (e.g. a BGP-LU label-stack change on a route whose route-key stays stable), the
 * superseded NLRI must be withdrawn before the replacement is advertised; otherwise only the advertisement is sent.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AdjRibOutListenerTest {
    private static final QName ROUTE_QNAME = QName.create("urn:opendaylight:test:adjribout", "route").intern();
    private static final QName ROUTE_KEY_QNAME = QName.create(ROUTE_QNAME, "route-key").intern();
    private static final QName LABEL_QNAME = QName.create(ROUTE_QNAME, "label").intern();
    private static final NodeIdentifier ATTRS_NID = new NodeIdentifier(QName.create(ROUTE_QNAME, "attributes"));
    private static final TablesKey IPV4_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);

    private final MapEntryNode before = route(Uint32.valueOf(100));
    private final MapEntryNode after = route(Uint32.valueOf(200));

    @Mock
    private RIBSupport<?, ?> support;
    @Mock
    private CodecsRegistry registry;
    @Mock
    private Codecs codecs;
    @Mock
    private DataTreeChangeExtension service;
    @Mock
    private ChannelOutputLimiter session;
    @Mock
    private Registration registration;
    @Mock
    private DataTreeCandidate candidate;
    @Mock
    private DataTreeCandidateNode rootNode;
    @Mock
    private DataTreeCandidateNode childNode;
    @Mock
    private DataTreeCandidateNode routeNode;
    @Mock
    private Update withdrawUpdate;
    @Mock
    private Update advertiseUpdate;

    private AdjRibOutListener listener;

    @Before
    public void setUp() {
        // Wires an in-place modification of a single route (before -> after). requiresWithdrawalOnReplace stays with
        // each test, which needs a different outcome from it.
        doReturn(codecs).when(registry).getCodecs(support);
        doReturn(NodeIdentifierWithPredicates.of(Tables.QNAME)).when(support).tablesKey();
        doReturn(registration).when(service).registerTreeChangeListener(any(), any());

        listener = AdjRibOutListener.create(new PeerId("bgp://127.0.0.1"),
            YangInstanceIdentifier.of(), registry, support, service, session, true);

        doReturn(ModificationType.SUBTREE_MODIFIED).when(rootNode).modificationType();
        doReturn(List.of(childNode)).when(rootNode).childNodes();
        doReturn(rootNode).when(candidate).getRootNode();
        doReturn(List.of(routeNode)).when(support).changedRoutes(childNode);

        doReturn(ModificationType.SUBTREE_MODIFIED).when(routeNode).modificationType();
        doReturn(before).when(routeNode).dataBefore();
        doReturn(after).when(routeNode).getDataAfter();

        doReturn(ATTRS_NID).when(support).routeAttributesIdentifier();
        doReturn(advertiseUpdate).when(support)
            .buildUpdate(eq(Collections.singleton(after)), eq(Collections.emptyList()), any());
        doReturn(IPV4_KEY).when(support).getTablesKey();
        doReturn(null).when(codecs).deserializeAttributes(nullable(NormalizedNode.class));
        doNothing().when(session).write(any());
        doNothing().when(session).flush();
    }

    /**
     * Tests that an in-place route modification which changes the on-wire NLRI identity writes a withdrawal of the
     * superseded NLRI before advertising the replacement.
     */
    @Test
    public void testChangedIdentityWithdrawsBeforeAdvertising() {
        doReturn(true).when(support).requiresWithdrawalOnReplace(before, after);
        doReturn(withdrawUpdate).when(support)
            .buildUpdate(eq(Collections.emptyList()), eq(Collections.singleton(before)), any());

        listener.onDataTreeChanged(List.of(candidate));

        // The superseded NLRI must be withdrawn before the replacement is advertised.
        final var inOrder = inOrder(session);
        inOrder.verify(session).write(withdrawUpdate);
        inOrder.verify(session).write(advertiseUpdate);
    }

    /**
     * Tests that an in-place route modification which keeps the on-wire NLRI identity advertises the replacement
     * without emitting any withdrawal.
     */
    @Test
    public void testUnchangedIdentityAdvertisesWithoutWithdrawal() {
        doReturn(false).when(support).requiresWithdrawalOnReplace(before, after);

        listener.onDataTreeChanged(List.of(candidate));

        verify(session).write(advertiseUpdate);
        verify(session, never()).write(withdrawUpdate);
    }

    private static MapEntryNode route(final Uint32 label) {
        return ImmutableNodes.newMapEntryBuilder()
            .withNodeIdentifier(NodeIdentifierWithPredicates.of(ROUTE_QNAME, ROUTE_KEY_QNAME, "test-route"))
            .withChild(ImmutableNodes.leafNode(LABEL_QNAME, label))
            .build();
    }
}
