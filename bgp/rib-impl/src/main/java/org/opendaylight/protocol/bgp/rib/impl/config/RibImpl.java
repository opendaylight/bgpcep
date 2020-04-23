/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getAfiSafiWithDefault;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getGlobalClusterIdentifier;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.toTableTypes;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.CodecsRegistryImpl;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RibImpl implements RIB, BGPRibStateConsumer, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RibImpl.class);

    private final RIBExtensionConsumerContext extensions;
    private final BGPDispatcher dispatcher;
    private final BindingCodecTreeFactory codecTreeFactory;
    private final DOMDataBroker domBroker;
    private final BGPRibRoutingPolicyFactory policyProvider;
    private RIBImpl ribImpl;
    private ServiceRegistration<?> serviceRegistration;
    private Collection<AfiSafi> afiSafi;
    private AsNumber asNumber;
    private Ipv4AddressNoZone routerId;

    private ClusterIdentifier clusterId;
    private final DataBroker dataBroker;

    public RibImpl(
            final RIBExtensionConsumerContext contextProvider,
            final BGPDispatcher dispatcher,
            final BGPRibRoutingPolicyFactory policyProvider,
            final BindingCodecTreeFactory codecTreeFactory,
            final DOMDataBroker domBroker,
            final DataBroker dataBroker,
            final DOMSchemaService domSchemaService
    ) {
        this.extensions = contextProvider;
        this.dispatcher = dispatcher;
        this.codecTreeFactory = codecTreeFactory;
        this.domBroker = domBroker;
        this.dataBroker = dataBroker;
        this.policyProvider = policyProvider;
    }

    void start(final Global global, final String instanceName, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.ribImpl == null,
                "Previous instance %s was not closed.", this);
        this.ribImpl = createRib(global, instanceName, tableTypeRegistry);
    }

    Boolean isGlobalEqual(final Global global) {
        final Collection<AfiSafi> globalAfiSafi = getAfiSafiWithDefault(global.getAfiSafis(), true).values();
        final Config globalConfig = global.getConfig();
        final AsNumber globalAs = globalConfig.getAs();
        final Ipv4Address globalRouterId = global.getConfig().getRouterId();
        final ClusterIdentifier globalClusterId = getGlobalClusterIdentifier(globalConfig);
        return this.afiSafi.containsAll(globalAfiSafi) && globalAfiSafi.containsAll(this.afiSafi)
                && globalAs.equals(this.asNumber)
                && globalRouterId.getValue().equals(this.routerId.getValue())
                && globalClusterId.getValue().equals(this.clusterId.getValue());
    }

    @Override
    public KeyedInstanceIdentifier<Rib, RibKey> getInstanceIdentifier() {
        return this.ribImpl.getInstanceIdentifier();
    }

    @Override
    public AsNumber getLocalAs() {
        return this.ribImpl.getLocalAs();
    }

    @Override
    public BgpId getBgpIdentifier() {
        return this.ribImpl.getBgpIdentifier();
    }

    @Override
    public Set<? extends BgpTableType> getLocalTables() {
        return this.ribImpl.getLocalTables();
    }

    @Override
    public BGPDispatcher getDispatcher() {
        return this.ribImpl.getDispatcher();
    }

    @Override
    public DOMTransactionChain createPeerDOMChain(final DOMTransactionChainListener listener) {
        return this.ribImpl.createPeerDOMChain(listener);
    }

    @Override
    public TransactionChain createPeerChain(final TransactionChainListener listener) {
        return this.ribImpl.createPeerChain(listener);
    }

    @Override
    public RIBExtensionConsumerContext getRibExtensions() {
        return this.ribImpl.getRibExtensions();
    }

    @Override
    public RIBSupportContextRegistry getRibSupportContext() {
        return this.ribImpl.getRibSupportContext();
    }

    @Override
    public YangInstanceIdentifier getYangRibId() {
        return this.ribImpl.getYangRibId();
    }

    @Override
    public CodecsRegistry getCodecsRegistry() {
        return this.ribImpl.getCodecsRegistry();
    }

    @Override
    public DOMDataTreeChangeService getService() {
        return this.ribImpl.getService();
    }

    @Override
    public DataBroker getDataBroker() {
        return this.ribImpl.getDataBroker();
    }

    FluentFuture<? extends CommitInfo> closeServiceInstance() {
        if (this.ribImpl != null) {
            return this.ribImpl.closeServiceInstance();
        }
        return CommitInfo.emptyFluentFuture();
    }

    @Override
    public void close() {
        if (this.ribImpl != null) {
            this.ribImpl.close();
            this.ribImpl = null;
        }
        if (this.serviceRegistration != null) {
            try {
                this.serviceRegistration.unregister();
            } catch (final IllegalStateException e) {
                LOG.warn("Failed to unregister {} service instance", this, e);
            }
            this.serviceRegistration = null;
        }
    }

    void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    @Override
    public Set<TablesKey> getLocalTablesKeys() {
        return this.ribImpl.getLocalTablesKeys();
    }

    @Override
    public boolean supportsTable(final TablesKey tableKey) {
        return this.ribImpl.supportsTable(tableKey);
    }

    @Override
    public BGPRibRoutingPolicy getRibPolicies() {
        return this.ribImpl.getRibPolicies();
    }

    @Override
    public BGPPeerTracker getPeerTracker() {
        return this.ribImpl.getPeerTracker();
    }

    @Override
    public String toString() {
        return this.ribImpl != null ? this.ribImpl.toString() : "";
    }

    private RIBImpl createRib(
            final Global global,
            final String bgpInstanceName,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        this.afiSafi = getAfiSafiWithDefault(global.getAfiSafis(), true).values();
        final Config globalConfig = global.getConfig();
        this.asNumber = globalConfig.getAs();
        this.routerId = IetfInetUtil.INSTANCE.ipv4AddressNoZoneFor(globalConfig.getRouterId());
        this.clusterId = getGlobalClusterIdentifier(globalConfig);
        final Map<TablesKey, PathSelectionMode> pathSelectionModes = OpenConfigMappingUtil
                .toPathSelectionMode(this.afiSafi, tableTypeRegistry).entrySet()
                .stream()
                .collect(Collectors.toMap(entry ->
                        new TablesKey(entry.getKey().getAfi(), entry.getKey().getSafi()), Map.Entry::getValue));

        final BGPRibRoutingPolicy ribPolicy = this.policyProvider.buildBGPRibPolicy(this.asNumber.getValue().toJava(),
                this.routerId, this.clusterId, RoutingPolicyUtil.getApplyPolicy(global.getApplyPolicy()));
        final CodecsRegistryImpl codecsRegistry = CodecsRegistryImpl.create(codecTreeFactory);

        return new RIBImpl(
                tableTypeRegistry,
                new RibId(bgpInstanceName),
                this.asNumber,
                new BgpId(this.routerId),
                this.extensions,
                this.dispatcher,
                codecsRegistry,
                this.domBroker,
                this.dataBroker,
                ribPolicy,
                toTableTypes(this.afiSafi, tableTypeRegistry),
                pathSelectionModes);
    }

    @Override
    public BGPRibState getRIBState() {
        return this.ribImpl.getRIBState();
    }

    public void instantiateServiceInstance() {
        if (this.ribImpl != null) {
            this.ribImpl.instantiateServiceInstance();
        }
    }

    @Override
    public void refreshTable(final TablesKey tk, final PeerId peerId) {
        this.ribImpl.refreshTable(tk, peerId);
    }
}
