/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getAfiSafiWithDefault;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getGlobalClusterIdentifier;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.toTableTypes;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
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
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RibImpl implements RIB, BGPRibStateProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RibImpl.class);

    private final RIBExtensionConsumerContext extensionProvider;
    private final BGPDispatcher dispatcher;
    private final CodecsRegistry codecsRegistry;
    private final DOMDataBroker domBroker;
    private final BGPRibRoutingPolicyFactory policyProvider;
    private final BGPStateProviderRegistry stateProviderRegistry;
    @GuardedBy("this")
    private RIBImpl ribImpl;
    @GuardedBy("this")
    private Registration stateProviderRegistration;
    @GuardedBy("this")
    private Collection<AfiSafi> afiSafi;
    @GuardedBy("this")
    private AsNumber asNumber;
    @GuardedBy("this")
    private Ipv4AddressNoZone routerId;
    @GuardedBy("this")
    private ClusterIdentifier clusterId;
    @GuardedBy("this")
    private RibId ribId;

    public RibImpl(
            final RIBExtensionConsumerContext extensionProvider,
            final BGPDispatcher dispatcher,
            final BGPRibRoutingPolicyFactory policyProvider,
            final CodecsRegistry codecsRegistry,
            final BGPStateProviderRegistry stateProviderRegistry,
            final DOMDataBroker domBroker) {
        this.extensionProvider = requireNonNull(extensionProvider);
        this.dispatcher = requireNonNull(dispatcher);
        this.codecsRegistry = requireNonNull(codecsRegistry);
        this.domBroker = requireNonNull(domBroker);
        this.policyProvider = requireNonNull(policyProvider);
        this.stateProviderRegistry = requireNonNull(stateProviderRegistry);
    }

    synchronized void start(final Global global, final String instanceName,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(ribImpl == null,
                "Previous instance %s was not closed.", this);
        LOG.info("Starting BGP instance {}", instanceName);
        ribId = new RibId(instanceName);
        ribImpl = createRib(global, tableTypeRegistry);
        stateProviderRegistration =  stateProviderRegistry.register(this);
    }

    synchronized Boolean isGlobalEqual(final Global global) {
        final Collection<AfiSafi> globalAfiSafi = getAfiSafiWithDefault(global.getAfiSafis(), true).values();
        final Config globalConfig = global.getConfig();
        final AsNumber globalAs = globalConfig.getAs();
        final Ipv4Address globalRouterId = global.getConfig().getRouterId();
        final ClusterIdentifier globalClusterId = getGlobalClusterIdentifier(globalConfig);
        return afiSafi.containsAll(globalAfiSafi) && globalAfiSafi.containsAll(afiSafi)
                && globalAs.equals(asNumber)
                && globalRouterId.getValue().equals(routerId.getValue())
                && globalClusterId.getValue().equals(clusterId.getValue());
    }

    @Override
    public synchronized KeyedInstanceIdentifier<Rib, RibKey> getInstanceIdentifier() {
        return ribImpl.getInstanceIdentifier();
    }

    @Override
    public synchronized AsNumber getLocalAs() {
        return ribImpl.getLocalAs();
    }

    @Override
    public synchronized BgpId getBgpIdentifier() {
        return ribImpl.getBgpIdentifier();
    }

    @Override
    public synchronized Set<? extends BgpTableType> getLocalTables() {
        return ribImpl.getLocalTables();
    }

    @Override
    public synchronized BGPDispatcher getDispatcher() {
        return ribImpl.getDispatcher();
    }

    @Override
    public synchronized DOMTransactionChain createPeerDOMChain(final DOMTransactionChainListener listener) {
        return ribImpl.createPeerDOMChain(listener);
    }

    @Override
    public synchronized RIBExtensionConsumerContext getRibExtensions() {
        return ribImpl.getRibExtensions();
    }

    @Override
    public synchronized RIBSupportContextRegistry getRibSupportContext() {
        return ribImpl.getRibSupportContext();
    }

    @Override
    public synchronized YangInstanceIdentifier getYangRibId() {
        return ribImpl.getYangRibId();
    }

    @Override
    public synchronized CodecsRegistry getCodecsRegistry() {
        return ribImpl.getCodecsRegistry();
    }

    @Override
    public synchronized DOMDataTreeChangeService getService() {
        return ribImpl.getService();
    }

    synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        if (ribImpl != null) {
            return ribImpl.closeServiceInstance();
        }
        return CommitInfo.emptyFluentFuture();
    }

    @Override
    public synchronized void close() throws ExecutionException, InterruptedException {
        if (ribImpl == null) {
            LOG.info("RIB instance {} already closed, skipping", ribId);
            return;
        }

        LOG.info("Closing RIB instance {}", ribId);
        if (stateProviderRegistration != null) {
            LOG.info("Unregistering state provider for RIB instance {}", ribId);
            stateProviderRegistration.close();
            stateProviderRegistration = null;
        }
        closeServiceInstance().get();
        ribImpl = null;
    }


    @Override
    public synchronized Set<TablesKey> getLocalTablesKeys() {
        return ribImpl.getLocalTablesKeys();
    }

    @Override
    public synchronized boolean supportsTable(final TablesKey tableKey) {
        return ribImpl.supportsTable(tableKey);
    }

    @Override
    public synchronized BGPRibRoutingPolicy getRibPolicies() {
        return ribImpl.getRibPolicies();
    }

    @Override
    public synchronized BGPPeerTracker getPeerTracker() {
        return ribImpl.getPeerTracker();
    }

    @Override
    public synchronized String toString() {
        return ribImpl != null ? ribImpl.toString() : "";
    }

    private synchronized RIBImpl createRib(
            final Global global,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        afiSafi = getAfiSafiWithDefault(global.getAfiSafis(), true).values();
        final Config globalConfig = global.getConfig();
        asNumber = globalConfig.getAs();
        routerId = IetfInetUtil.INSTANCE.ipv4AddressNoZoneFor(globalConfig.getRouterId());
        clusterId = getGlobalClusterIdentifier(globalConfig);
        final Map<TablesKey, PathSelectionMode> pathSelectionModes = OpenConfigMappingUtil
                .toPathSelectionMode(afiSafi, tableTypeRegistry).entrySet()
                .stream()
                .collect(Collectors.toMap(entry ->
                        new TablesKey(entry.getKey().getAfi(), entry.getKey().getSafi()), Map.Entry::getValue));

        final BGPRibRoutingPolicy ribPolicy = policyProvider.buildBGPRibPolicy(asNumber.getValue().toJava(),
                routerId, clusterId, RoutingPolicyUtil.getApplyPolicy(global.getApplyPolicy()));

        return new RIBImpl(
                tableTypeRegistry,
                ribId,
                asNumber,
                new BgpId(routerId),
                extensionProvider,
                dispatcher,
                codecsRegistry,
                domBroker,
                ribPolicy,
                toTableTypes(afiSafi, tableTypeRegistry),
                pathSelectionModes);
    }

    @Override
    public synchronized BGPRibState getRIBState() {
        return ribImpl.getRIBState();
    }

    public synchronized void instantiateServiceInstance() {
        if (ribImpl != null) {
            ribImpl.instantiateServiceInstance();
        }
    }

    @Override
    public synchronized void refreshTable(final TablesKey tk, final PeerId peerId) {
        ribImpl.refreshTable(tk, peerId);
    }
}
