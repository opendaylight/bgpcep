/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static com.google.common.base.Verify.verifyNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingDataObjectCodecTreeNode;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyAware;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public abstract class AbstractRIBSupportTest<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & KeyAware<?>> extends AbstractConcurrentDataBrokerTest {
    protected static final @NonNull PathId PATH_ID = new PathId(Uint32.ONE);
    protected static final @NonNull Attributes ATTRIBUTES = new AttributesBuilder().build();
    private static final InstanceIdentifier<LocRib> RIB = InstanceIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("rib"))).child(LocRib.class).build();

    @Mock
    protected DOMDataTreeWriteTransaction tx;
    protected List<InstanceIdentifier<R>> deletedRoutes;
    protected List<Map.Entry<InstanceIdentifier<?>, DataObject>> insertedRoutes;

    protected AdapterContext adapter;
    private AbstractRIBSupport<C, S, R> abstractRIBSupport;

    protected final void setUpTestCustomizer(final AbstractRIBSupport<C, S, R> ribSupport) throws Exception {
        abstractRIBSupport = ribSupport;
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doAnswer(invocation -> {
            final var path = invocation.getArgument(1, YangInstanceIdentifier.class);
            final var data = invocation.getArgument(2, NormalizedNode.class);

            AbstractRIBSupportTest.this.insertedRoutes.add(adapter.currentSerializer().fromNormalizedNode(path, data));
            return null;
        }).when(tx).put(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
                any(NormalizedNode.class));

        doAnswer(invocation -> {
            final var path = invocation.getArgument(1, YangInstanceIdentifier.class);

            AbstractRIBSupportTest.this.deletedRoutes.add((InstanceIdentifier)
                adapter.currentSerializer().fromYangInstanceIdentifier(path));
            return null;
        }).when(tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        deletedRoutes = new ArrayList<>();
        insertedRoutes = new ArrayList<>();
    }

    @Override
    protected final AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        adapter = customizer.getAdapterContext();
        return customizer;
    }

    private @NonNull BindingDataObjectCodecTreeNode<Attributes> updateAttributesCodec() {
        return adapter.currentSerializer().getStreamChild(Update.class).getStreamDataObject(Attributes.class);
    }

    protected final ContainerNode createNlriWithDrawnRoute(final DestinationType destUnreach) {
        return (ContainerNode) updateAttributesCodec()
            .getStreamChild(AttributesUnreach.class).getStreamDataObject(MpUnreachNlri.class)
            .serialize(new MpUnreachNlriBuilder()
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(destUnreach).build())
                .build());
    }

    protected final ContainerNode createNlriAdvertiseRoute(final DestinationType destReach) {
        return (ContainerNode) updateAttributesCodec()
            .getStreamChild(AttributesReach.class).getStreamDataObject(MpReachNlri.class)
            .serialize(new MpReachNlriBuilder()
                .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destReach).build())
                .build());
    }

    protected final ContainerNode createAttributes() {
        return (ContainerNode) updateAttributesCodec().serialize(ATTRIBUTES);
    }

    protected final MapEntryNode createEmptyTable() {
        final Tables tables = new TablesBuilder().withKey(abstractRIBSupport.getTablesKey())
            .setAttributes(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329
                .rib.tables.AttributesBuilder().build()).build();
        return (MapEntryNode) adapter.currentSerializer().toNormalizedDataObject(tablesIId(), tables).node();
    }

    protected final ChoiceNode createRoutes(final Routes routes) {
        final Tables tables = new TablesBuilder().withKey(abstractRIBSupport.getTablesKey()).setRoutes(routes).build();
        return (ChoiceNode) verifyNotNull(((MapEntryNode) adapter.currentSerializer()
            .toNormalizedDataObject(tablesIId(), tables).node())
            .childByArg(new NodeIdentifier(Routes.QNAME)));
    }

    protected final Collection<MapEntryNode> createRoutes(final S routes) {
        Preconditions.checkArgument(routes.implementedInterface()
                .equals(abstractRIBSupport.routesContainerClass()));
        final InstanceIdentifier<S> routesIId = routesIId();
        final NodeResult normalizedNode = adapter.currentSerializer().toNormalizedDataObject(routesIId, routes);
        final ContainerNode container = (ContainerNode) normalizedNode.node();
        final NodeIdentifier routeNid = new NodeIdentifier(abstractRIBSupport.routeQName());
        return ((MapNode) container.getChildByArg(routeNid)).body();
    }

    private InstanceIdentifier<Tables> tablesIId() {
        return RIB.child(Tables.class, abstractRIBSupport.getTablesKey());
    }

    private InstanceIdentifier<S> routesIId() {
        final InstanceIdentifier<Tables> tables = tablesIId();
        return tables.child(abstractRIBSupport.routesCaseClass(), abstractRIBSupport.routesContainerClass());
    }

    protected final YangInstanceIdentifier getTablePath() {
        final InstanceIdentifier<Tables> tables = tablesIId();
        return adapter.currentSerializer().toYangInstanceIdentifier(tables);
    }

    protected final YangInstanceIdentifier getRoutePath() {
        final InstanceIdentifier<S> routesIId = routesIId();
        return adapter.currentSerializer().toYangInstanceIdentifier(routesIId).node(abstractRIBSupport.routeQName());
    }

    protected final NodeIdentifierWithPredicates createRouteNIWP(final S routes) {
        final Collection<MapEntryNode> map = createRoutes(routes);
        return Iterables.getOnlyElement(map).name();
    }
}
