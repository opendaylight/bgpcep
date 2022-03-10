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
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
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
        R extends Route & ChildOf<? super S> & Identifiable<?>> extends AbstractConcurrentDataBrokerTest {
    protected static final PathId PATH_ID = new PathId(Uint32.ONE);
    protected static final Attributes ATTRIBUTES = new AttributesBuilder().build();
    private static final InstanceIdentifier<LocRib> RIB = InstanceIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("rib"))).child(LocRib.class).build();

    @Mock
    protected DOMDataTreeWriteTransaction tx;
    protected List<InstanceIdentifier<R>> deletedRoutes;
    protected List<Map.Entry<InstanceIdentifier<?>, DataObject>> insertedRoutes;

    protected AdapterContext adapter;
    private AbstractRIBSupport<C, S, R> abstractRIBSupport;

    protected final void setUpTestCustomizer(final AbstractRIBSupport<C, S, R> ribSupport) throws Exception {
        this.abstractRIBSupport = ribSupport;
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            AbstractRIBSupportTest.this.insertedRoutes.add(adapter.currentSerializer()
                    .fromNormalizedNode((YangInstanceIdentifier) args[1], (NormalizedNode) args[2]));
            return args[1];
        }).when(this.tx).put(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
                any(NormalizedNode.class));

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            AbstractRIBSupportTest.this.deletedRoutes.add((InstanceIdentifier)
                adapter.currentSerializer().fromYangInstanceIdentifier((YangInstanceIdentifier) args[1]));
            return args[1];
        }).when(this.tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        this.deletedRoutes = new ArrayList<>();
        this.insertedRoutes = new ArrayList<>();
    }

    @Override
    protected final AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.adapter = customizer.getAdapterContext();
        return customizer;
    }

    private @NonNull BindingDataObjectCodecTreeNode<Attributes> updateAttributesCodec() {
        return adapter.currentSerializer().streamChild(Update.class).streamChild(Attributes.class);
    }

    protected final ContainerNode createNlriWithDrawnRoute(final DestinationType destUnreach) {
        return (ContainerNode) updateAttributesCodec()
            .streamChild(AttributesUnreach.class).streamChild(MpUnreachNlri.class)
            .serialize(new MpUnreachNlriBuilder()
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(destUnreach).build())
                .build());
    }

    protected final ContainerNode createNlriAdvertiseRoute(final DestinationType destReach) {
        return (ContainerNode) updateAttributesCodec()
            .streamChild(AttributesReach.class).streamChild(MpReachNlri.class)
            .serialize(new MpReachNlriBuilder()
                .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destReach).build())
                .build());
    }

    protected final ContainerNode createAttributes() {
        return (ContainerNode) updateAttributesCodec().serialize(ATTRIBUTES);
    }

    protected final MapEntryNode createEmptyTable() {
        final Tables tables = new TablesBuilder().withKey(getTablesKey())
            .setAttributes(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329
                .rib.tables.AttributesBuilder().build()).build();
        return (MapEntryNode) this.adapter.currentSerializer().toNormalizedNode(tablesIId(), tables).getValue();
    }

    protected final ChoiceNode createRoutes(final Routes routes) {
        final Tables tables = new TablesBuilder().withKey(getTablesKey()).setRoutes(routes).build();
        return (ChoiceNode) verifyNotNull(((MapEntryNode) this.adapter.currentSerializer()
            .toNormalizedNode(tablesIId(), tables).getValue())
            .childByArg(new NodeIdentifier(BindingReflections.findQName(Routes.class))));
    }

    protected final Collection<MapEntryNode> createRoutes(final S routes) {
        Preconditions.checkArgument(routes.implementedInterface()
                .equals(this.abstractRIBSupport.routesContainerClass()));
        final InstanceIdentifier<S> routesIId = routesIId();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode> normalizedNode = this.adapter.currentSerializer()
                .toNormalizedNode(routesIId, routes);
        final ContainerNode container = (ContainerNode) normalizedNode.getValue();
        final NodeIdentifier routeNid = new NodeIdentifier(getRouteListQname());
        return ((MapNode) verifyNotNull(container.childByArg(routeNid))).body();
    }

    private TablesKey getTablesKey() {
        return new TablesKey(this.abstractRIBSupport.getAfi(), this.abstractRIBSupport.getSafi());
    }

    private InstanceIdentifier<Tables> tablesIId() {
        return RIB.child(Tables.class, getTablesKey());
    }

    private InstanceIdentifier<S> routesIId() {
        final InstanceIdentifier<Tables> tables = tablesIId();
        return tables.child(this.abstractRIBSupport.routesCaseClass(), this.abstractRIBSupport.routesContainerClass());
    }

    protected final YangInstanceIdentifier getTablePath() {
        final InstanceIdentifier<Tables> tables = tablesIId();
        return this.adapter.currentSerializer().toYangInstanceIdentifier(tables);
    }

    protected final YangInstanceIdentifier getRoutePath() {
        final InstanceIdentifier<S> routesIId = routesIId();
        return this.adapter.currentSerializer().toYangInstanceIdentifier(routesIId).node(getRouteListQname());
    }

    private QName getRouteListQname() {
        return BindingReflections.findQName(this.abstractRIBSupport.routesListClass())
                .bindTo(BindingReflections.getQNameModule(this.abstractRIBSupport.routesCaseClass()));
    }

    protected final NodeIdentifierWithPredicates createRouteNIWP(final S routes) {
        final Collection<MapEntryNode> map = createRoutes(routes);
        return Iterables.getOnlyElement(map).getIdentifier();
    }
}
