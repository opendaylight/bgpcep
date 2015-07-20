package org.opendaylight.bgpcep.bgp.openconfig;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv4Unicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv6Unicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpPeerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.RibImplBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.AdvertizedTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.AdvertizedTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.AdvertizedTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.PeerRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.RibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.Modules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpConfigWriter implements DataChangeListener, BindingAwareConsumer, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BgpConfigWriter.class);

    private static final InstanceIdentifier<Bgp> BGP_IID = InstanceIdentifier.create(Bgp.class);
    private static final InstanceIdentifier<Global> GLOBAL_IID = BGP_IID.child(Global.class);
    private static final InstanceIdentifier<Neighbors> NEIGHBORS_IID = BGP_IID.child(Neighbors.class);
    private static final InstanceIdentifier<Node> CONTROLLER_CONFIG_IID = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
            .child(Node.class, new NodeKey(new NodeId("controller-config")));
    private static final InstanceIdentifier<Modules> MODULES_IID = InstanceIdentifier.create(Modules.class);

    private MountPointService mountService;
    private ListenerRegistration<DataChangeListener> registration;

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.info("On Data Change {}", change);
        final Optional<MountPoint> mp = mountService.getMountPoint(CONTROLLER_CONFIG_IID);
        if (mp.isPresent()) {
            final Optional<DataBroker> dataBrokerMaybe = mp.get().getService(DataBroker.class);
            if (dataBrokerMaybe.isPresent()) {
                final DataBroker dataBroker = dataBrokerMaybe.get();
                final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
                if (change.getCreatedData() != null) {
                    LOG.info("Created data: {}", change.getCreatedData());
                    for (final Map.Entry<InstanceIdentifier<?>, DataObject> createdEntry : change.getCreatedData().entrySet()) {
                        if (createdEntry.getKey().equals(GLOBAL_IID)) {
                            //final Module module = globalConfigToRibImplConfigModule((Global) createdEntry.getValue());
                            //wTx.put(LogicalDatastoreType.CONFIGURATION, MODULES_IID.builder().child(Module.class, module.getKey()).build(), module);
                        } else if (createdEntry.getKey().firstKeyOf(Neighbor.class, null) != null) {
                            if (createdEntry.getValue() instanceof Neighbor) {
                                final Module module = neighborConfigToBgpPeerConfigModule(((Neighbor) createdEntry.getValue()));
                                wTx.put(LogicalDatastoreType.CONFIGURATION, MODULES_IID.builder().child(Module.class, module.getKey()).build(), module);
                            }
                        }
                    }
                }
                if (change.getUpdatedData() != null) {
                    //LOG.info("Update data: {}", change.getUpdatedData());
                    for (final Map.Entry<InstanceIdentifier<?>, DataObject> updatedEntry : change.getUpdatedData().entrySet()) {
                        if (updatedEntry.getKey().equals(GLOBAL_IID)) {
                            //final Module module = globalConfigToRibImplConfigModule((Global) updatedEntry.getValue());
                            //wTx.merge(LogicalDatastoreType.CONFIGURATION, MODULES_IID.builder().child(Module.class, module.getKey()).build(), module);
                        } else if (updatedEntry.getKey().contains(NEIGHBORS_IID)) {
                            //final Module module = neighborConfigToBgpPeerConfigModule((Neighbor) updatedEntry.getValue());
                            //wTx.merge(LogicalDatastoreType.CONFIGURATION, MODULES_IID.builder().child(Module.class, module.getKey()).build(), module);
                        }
                    }
                }
                if (change.getRemovedPaths() != null) {
                    for (final InstanceIdentifier<?> removedPath : change.getRemovedPaths()) {
                        //TODO remove module
                    }
                }
                wTx.submit();
            }
        }
    }

    @Override
    public void onSessionInitialized(final ConsumerContext session) {
        this.mountService = session.getSALService(MountPointService.class);
        final DataBroker dataBroker = session.getSALService(DataBroker.class);
        this.registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, BGP_IID, this, DataChangeScope.SUBTREE);
    }

    private static Module globalConfigToRibImplConfigModule(final Global globalConfig) {
        final RibImplBuilder ribImplBuilder = new RibImplBuilder();
        //TODO null checking
        ribImplBuilder.setBgpRibId(globalConfig.getConfig().getRouterId());
        ribImplBuilder.setLocalAs(globalConfig.getConfig().getAs().getValue());
        //TODO complete implemention
        final ModuleBuilder mBuilder = new ModuleBuilder();
        mBuilder.setConfiguration(ribImplBuilder.build());
        mBuilder.setName("example-bgp-rib");
        mBuilder.setType(RibImpl.class);
        mBuilder.setKey(new ModuleKey(mBuilder.getName(), mBuilder.getType()));
        return mBuilder.build();
    }

    private static Module neighborConfigToBgpPeerConfigModule(final Neighbor neighbor) {
        final BgpPeerBuilder builder = new BgpPeerBuilder();
        //TODO null checking
        builder.setHoldtimer(neighbor.getTimers().getConfig().getHoldTime().shortValue());
        builder.setHost(neighbor.getNeighborAddress());
        builder.setInitiateConnection(neighbor.getTransport().getConfig().isPassiveMode());
        builder.setAdvertizedTable(afiSafisToAdvertizedTable(neighbor.getAfiSafis().getAfiSafi()));
        if (neighbor.getConfig().getAuthPassword() != null) {
            builder.setPassword(new Rfc2385Key(neighbor.getConfig().getAuthPassword()));
        }
        builder.setPeerRole(peerTypeToPeerRole(neighbor.getConfig().getPeerType()));
        builder.setRemoteAs(neighbor.getConfig().getLocalAs().getValue());
        //FIXME hard-coded value
        builder.setRib(new RibBuilder()
            .setName("example-bgp-rib")
            .setType(RibInstance.class)
            .build());
        builder.setPeerRegistry(
                new PeerRegistryBuilder()
                .setName("global-bgp-peer-registry")
                .setType(BgpPeerRegistry.class)
                .build());

        final ModuleBuilder mBuilder = new ModuleBuilder();
        mBuilder.setConfiguration(builder.build());
        mBuilder.setName(neighbor.getConfig().getDescription());
        mBuilder.setType(BgpPeer.class);
        mBuilder.setKey(new ModuleKey(mBuilder.getName(), mBuilder.getType()));
        LOG.info("Peer config module created: {}", mBuilder.build());
        return mBuilder.build();
    }

    private static List<AdvertizedTable> afiSafisToAdvertizedTable(final List<AfiSafi> afisafis) {
        return Lists.transform(afisafis, new Function<AfiSafi, AdvertizedTable>() {
            @Override
            public AdvertizedTable apply(final AfiSafi afiSafi) {
                // TODO augment for afi/safi linkstate, flowspec
                final AdvertizedTableBuilder builder = new AdvertizedTableBuilder();
                if (afiSafi.getAfiSafiName() == Ipv4Unicast.class) {
                    //FIXME hard-coded value
                    builder.setName("ipv4-unicast");
                } else if (afiSafi.getAfiSafiName() == Ipv6Unicast.class) {
                    builder.setName("ipv6-unicast");
                }
                builder.setType(BgpTableType.class);
                builder.setKey(new AdvertizedTableKey(builder.getName(), builder.getType()));
                return builder.build();
            }
        });
    }

    //TODO rr-client
    private static PeerRole peerTypeToPeerRole(final PeerType peerType) {
        switch (peerType) {
        case INTERNAL:
            return PeerRole.Ibgp;
        case EXTERNAL:
            return PeerRole.Ebgp;
        default:
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }
}
