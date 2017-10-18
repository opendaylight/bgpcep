/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public abstract class AbstractRIBSupportTest {
    protected static final long PATH_ID = 1;
    protected static final Attributes ATTRIBUTES = new AttributesBuilder().build();
    private static final InstanceIdentifier<LocRib> RIB = InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(new RibId("rib"))).child(LocRib.class).build();
    private static final InstanceIdentifier<Attributes> ATTRIBUTES_IID = InstanceIdentifier.create(Update.class).child(Attributes.class);
    private static final InstanceIdentifier<MpUnreachNlri> MP_UNREACH_IID = ATTRIBUTES_IID.augmentation(Attributes2.class).child(MpUnreachNlri.class);
    private static final InstanceIdentifier<MpReachNlri> MP_REACH_IID = ATTRIBUTES_IID.augmentation(Attributes1.class).child(MpReachNlri.class);

    @Mock
    protected DOMDataWriteTransaction tx;
    protected List<InstanceIdentifier<?>> deletedRoutes;
    protected List<Map.Entry<InstanceIdentifier<?>, DataObject>> insertedRoutes;

    private BindingToNormalizedNodeCodec mappingService;
    private AbstractRIBSupport abstractRIBSupport;
    private ModuleInfoBackedContext moduleInfoBackedContext;

    protected final void setUpTestCustomizer(final AbstractRIBSupport ribSupport) throws Exception {
        this.abstractRIBSupport = ribSupport;
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(this.abstractRIBSupport.routesContainerClass()));
        this.mappingService.onGlobalContextUpdated(this.moduleInfoBackedContext.tryToCreateSchemaContext().get());
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            AbstractRIBSupportTest.this.insertedRoutes.add(AbstractRIBSupportTest.this.mappingService.fromNormalizedNode((YangInstanceIdentifier) args[1], (NormalizedNode<?, ?>) args[2]));
            return args[1];
        }).when(this.tx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));

        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            AbstractRIBSupportTest.this.deletedRoutes.add(AbstractRIBSupportTest.this.mappingService.fromYangInstanceIdentifier((YangInstanceIdentifier) args[1]));
            return args[1];
        }).when(this.tx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class));
        this.deletedRoutes = new ArrayList<>();
        this.insertedRoutes = new ArrayList<>();

        this.mappingService = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
            new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        this.moduleInfoBackedContext = ModuleInfoBackedContext.create();
    }

    protected final ContainerNode createNlriWithDrawnRoute(final DestinationType destUnreach) {
        final MpUnreachNlri mpReach = new MpUnreachNlriBuilder().setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(destUnreach).build()).build();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> result = this.mappingService.toNormalizedNode(MP_UNREACH_IID, mpReach);
        return (ContainerNode) result.getValue();
    }

    protected final ContainerNode createNlriAdvertiseRoute(final DestinationType destReach) {
        final MpReachNlri mpReach = new MpReachNlriBuilder().setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destReach).build()).build();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> result = this.mappingService.toNormalizedNode(MP_REACH_IID, mpReach);
        return (ContainerNode) result.getValue();
    }

    protected final ContainerNode createAttributes() {
        return (ContainerNode) this.mappingService.toNormalizedNode(ATTRIBUTES_IID, ATTRIBUTES).getValue();
    }

    protected final ChoiceNode createRoutes(final Routes routes) {
        final Tables tables = new TablesBuilder().setKey(getTablesKey()).setRoutes(routes).build();
        return (ChoiceNode) ((MapEntryNode) this.mappingService.toNormalizedNode(tablesIId(), tables).getValue())
            .getChild(new NodeIdentifier(BindingReflections.findQName(Routes.class))).get();
    }

    private TablesKey getTablesKey() {
        return new TablesKey(this.abstractRIBSupport.getAfi(), this.abstractRIBSupport.getSafi());
    }

    private InstanceIdentifier<Tables> tablesIId() {
        return RIB.child(Tables.class, getTablesKey());
    }

    private InstanceIdentifier<DataObject> routesIId() {
        final InstanceIdentifier<Tables> tables = tablesIId();
        return tables.child((Class) this.abstractRIBSupport.routesContainerClass());
    }

    protected final YangInstanceIdentifier getTablePath() {
        final InstanceIdentifier<Tables> tables = tablesIId();
        return this.mappingService.toYangInstanceIdentifier(tables);
    }

    protected final YangInstanceIdentifier getRoutePath() {
        final InstanceIdentifier<DataObject> routesIId = routesIId();
        return this.mappingService.toYangInstanceIdentifier(routesIId).node(getRouteListQname());
    }

    protected final Collection<MapEntryNode> createRoutes(final DataObject routes) {
        Preconditions.checkArgument(routes.getImplementedInterface().equals(this.abstractRIBSupport.routesContainerClass()));
        final InstanceIdentifier<DataObject> routesIId = routesIId();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode = this.mappingService.toNormalizedNode(routesIId, routes);
        final ContainerNode container = (ContainerNode) normalizedNode.getValue();
        final NodeIdentifier routeNid = new NodeIdentifier(getRouteListQname());
        return ((MapNode) container.getChild(routeNid).get()).getValue();
    }

    private QName getRouteListQname() {
       return QName.create(BindingReflections.findQName(this.abstractRIBSupport.routesContainerClass()),
           BindingReflections.findQName(this.abstractRIBSupport.routesListClass()).intern().getLocalName());
    }

    protected final NodeIdentifierWithPredicates createRouteNIWP(final DataObject routes) {
        final Collection<MapEntryNode> map = createRoutes(routes);
        return Iterables.getOnlyElement(map).getIdentifier();
    }

    @After
    public final void tearDown() throws InterruptedException, ExecutionException {
        this.mappingService.close();
    }
}
