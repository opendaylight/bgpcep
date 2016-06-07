/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AigpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.aigp.AigpTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.util.SingletonSet;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public abstract class AbstractRIBSupportTest {
    protected static final QName PEER1_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet:test", "2015-03-05", "peer1");
    protected static final Ipv4NextHopCase IPV4_NEXT_HOP;
    protected static final Ipv6NextHopCase IPV6_NEXT_HOP;
    protected final static long PATH_ID = 1;
    private static final Ipv4Address IPV4_ADDRESS_20 = new Ipv4Address("20.20.20.20");
    private static final Ipv4Address IPV4_ADDRESS_30 = new Ipv4Address("30.30.30.30");
    private static final Ipv4Address IPV4_ADDRESS_40 = new Ipv4Address("40.40.40.40");
    private static final Ipv4Address IPV4_ADDRESS_12 = new Ipv4Address("12.12.12.12");

    static {
        IPV4_NEXT_HOP = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("42.42.42.42")).build()).build();
        IPV6_NEXT_HOP = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8:1:2::")).build()).build();
    }

    protected ArrayList<YangInstanceIdentifier> routesYiiPutList;
    protected ArrayList<NormalizedNode<?, ?>> routesPutList;
    protected ArrayList<YangInstanceIdentifier> routesYiiDelList;
    protected BindingToNormalizedNodeCodec mappingService;

    @Mock
    protected DOMDataWriteTransaction tx;

    /**
     * register any other required class to be serialized
     *
     * @param moduleInfoBackedContext
     */
    protected abstract void registerModuleInfo(final ModuleInfoBackedContext moduleInfoBackedContext) throws Exception;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                AbstractRIBSupportTest.this.routesYiiPutList.add((YangInstanceIdentifier) args[1]);
                AbstractRIBSupportTest.this.routesPutList.add((NormalizedNode<?, ?>) args[2]);
                return args[1];
            }
        }).when(this.tx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                AbstractRIBSupportTest.this.routesYiiDelList.add((YangInstanceIdentifier) args[1]);
                return args[1];
            }
        }).when(this.tx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class));
        this.mappingService = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
            new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();

        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(BgpParameters.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(MultiprotocolCapability.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(AdvertizedRoutes.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(BgpRib.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Attributes1.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(MpReachNlri.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Attributes1.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(MpUnreachNlri.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Attributes2.class));
        registerModuleInfo(moduleInfoBackedContext);
        this.mappingService.onGlobalContextUpdated(moduleInfoBackedContext.tryToCreateSchemaContext().get());
        this.routesYiiPutList = new ArrayList<>();
        this.routesYiiDelList = new ArrayList<>();
        this.routesPutList = new ArrayList<>();
    }

    /**
     * Creates Attributes containing MpReach and MpUnreach.
     */
    protected AttributesBuilder createAttributes(final CNextHop hop, final DestinationType destReach, final DestinationType destUnreach) {
        final List<AsNumber> asSequences = Lists.newArrayList(new AsNumber(72L), new AsNumber(82L), new AsNumber(92L));
        final List<Segments> segments = Lists.newArrayList();
        final SegmentsBuilder segmentsBuild = new SegmentsBuilder();
        segmentsBuild.setAsSequence(asSequences).build();

        final AttributesBuilder attribBuilder = new AttributesBuilder()
            .setAggregator(new AggregatorBuilder().setAsNumber(new AsNumber(72L)).setNetworkAddress(new Ipv4Address(IPV4_ADDRESS_20)).build())
            .setAigp(new AigpBuilder().setAigpTlv(new AigpTlvBuilder().setMetric(new AccumulatedIgpMetric(BigInteger.ONE)).build()).build())
            .setAsPath(new AsPathBuilder().setSegments(segments).build())
            .setAtomicAggregate(new AtomicAggregateBuilder().build())
            .setClusterId(new ClusterIdBuilder().setCluster(Lists.newArrayList(new ClusterIdentifier(IPV4_ADDRESS_30),
                new ClusterIdentifier(IPV4_ADDRESS_40))).build())
            .setCNextHop(hop)
            .setLocalPref(new LocalPrefBuilder().setPref(2L).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(123L).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setOriginatorId(new OriginatorIdBuilder().setOriginator(IPV4_ADDRESS_12).build())
            .setUnrecognizedAttributes(new ArrayList<>());

        final MpReachNlri reach = buildReach(destReach, hop);
        final MpUnreachNlri unreach = buildUnreach(destUnreach);
        attribBuilder.addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(reach).build());
        attribBuilder.addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(unreach).build());

        return attribBuilder;
    }

    protected final ContainerNode createNlriWithDrawnRoute(final DestinationType destUnreach) {
        final InstanceIdentifier tablesIid = InstanceIdentifier.create(Update.class).child(Attributes.class);
        final InstanceIdentifier<MpUnreachNlri> routesIId = tablesIid.child(Attributes2.class).child(MpUnreachNlri.class);
        final MpUnreachNlri mpReach = new MpUnreachNlriBuilder().setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(destUnreach).build()).build();

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> result = this.mappingService.toNormalizedNode(routesIId, mpReach);
        return (ContainerNode) result.getValue();
    }

    protected final ContainerNode createNlriAdvertiseRoute(final DestinationType destReach) {
        final InstanceIdentifier TABLES_IID = InstanceIdentifier.create(Update.class).child(Attributes.class);
        final InstanceIdentifier<MpReachNlri> routesIId = TABLES_IID.child(Attributes1.class).child(MpReachNlri.class);
        final MpReachNlri mpReach = new MpReachNlriBuilder().setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destReach).build()).build();

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> result = this.mappingService.toNormalizedNode(routesIId, mpReach);
        return (ContainerNode) result.getValue();
    }

    private MpReachNlri buildReach(final DestinationType destinationType, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(getAfi());
        mb.setSafi(getSafi());
        mb.setCNextHop(hop);
        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destinationType).build());
        return mb.build();
    }

    protected abstract Class<? extends SubsequentAddressFamily> getSafi();

    protected abstract Class<? extends AddressFamily> getAfi();

    private MpUnreachNlri buildUnreach(final DestinationType destinationType) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(getAfi());
        mb.setSafi(getSafi());
        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(destinationType).build());
        return mb.build();
    }

    protected final InstanceIdentifier<Tables> getKeyIId() {
        return KeyedInstanceIdentifier
            .builder(ApplicationRib.class, new ApplicationRibKey(new ApplicationRibId("example-app-rib")))
            .child(Tables.class, new TablesKey(getAfi(), getSafi())).build();
    }

    protected final <T extends DataObject> void checkEmptyRoute(final InstanceIdentifier<T> path, final T data) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> expected = this.mappingService.toNormalizedNode(path, data);
        final NormalizedNode<?, ?> temp = this.routesPutList.get(0);
        final SingletonSet<ContainerNode> emptyRoutes = (SingletonSet) temp.getValue();
        assertEquals(expected.getValue(), emptyRoutes.getElement());
    }

    @After
    public final void tearDown() throws InterruptedException, ExecutionException {
        this.mappingService.close();
    }
}