/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getAfiSafiWithDefault;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl.BGPRenderStats;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RibImpl implements RIB, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RibImpl.class);

    private final RIBExtensionConsumerContext extensions;
    private final BGPDispatcher dispatcher;
    private final BindingCodecTreeFactory codecTreeFactory;
    private final DOMDataBroker domBroker;
    private final SchemaService schemaService;
    private RIBImpl ribImpl;
    private ServiceRegistration<?> serviceRegistration;
    private ListenerRegistration<SchemaContextListener> schemaContextRegistration;
    private final ClusterSingletonServiceProvider provider;
    private List<AfiSafi> afiSafi;
    private AsNumber asNumber;
    private Ipv4Address routerId;

    private ClusterIdentifier clusterId;

    public RibImpl(final ClusterSingletonServiceProvider provider, final RIBExtensionConsumerContext contextProvider, final BGPDispatcher dispatcher,
            final BindingCodecTreeFactory codecTreeFactory, final DOMDataBroker domBroker, final SchemaService schemaService) {
        this.provider = Preconditions.checkNotNull(provider);
        this.extensions = contextProvider;
        this.dispatcher = dispatcher;
        this.codecTreeFactory = codecTreeFactory;
        this.domBroker = domBroker;
        this.schemaService = schemaService;
    }

    void start(final Global global, final String instanceName, final BGPOpenConfigMappingService mappingService,
        final BgpDeployer.WriteConfiguration configurationWriter) {
        Preconditions.checkState(this.ribImpl == null, "Previous instance %s was not closed.", this);
        this.ribImpl = createRib(this.provider, global, instanceName, mappingService, configurationWriter);
        this.schemaContextRegistration = this.schemaService.registerSchemaContextListener(this.ribImpl);
    }

    Boolean isGlobalEqual(final Global global) {
        final List<AfiSafi> globalAfiSafi = getAfiSafiWithDefault(global.getAfiSafis(), true);
        final Config globalConfig = global.getConfig();
        final AsNumber globalAs = globalConfig.getAs();
        final Ipv4Address globalRouterId = global.getConfig().getRouterId();
        final ClusterIdentifier globalClusterId = OpenConfigMappingUtil.getClusterIdentifier(globalConfig);
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
    public DOMTransactionChain createPeerChain(final TransactionChainListener listener) {
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
    public Optional<BGPOpenConfigProvider> getOpenConfigProvider() {
        return this.ribImpl.getOpenConfigProvider();
    }

    @Override
    public DOMDataTreeChangeService getService() {
        return this.ribImpl.getService();
    }

    @Override
    public void close() {
        if (this.ribImpl != null) {
            try {
                this.ribImpl.close();
            } catch (final Exception e) {
                LOG.warn("Failed to close {} rib instance", this, e);
            }
            this.ribImpl = null;
        }
        if (this.schemaContextRegistration != null) {
            this.schemaContextRegistration.close();
            this.schemaContextRegistration = null;
        }
        if (this.serviceRegistration != null) {
            try {
                this.serviceRegistration.unregister();
            } catch(final IllegalStateException e) {
                LOG.warn("Failed to unregister {} service instance", this, e);
            }
            this.serviceRegistration = null;
        }
    }

    void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    @Override
    public BGPRenderStats getRenderStats() {
        return this.ribImpl.getRenderStats();
    }

    @Override
    public ImportPolicyPeerTracker getImportPolicyPeerTracker() {
        return this.ribImpl.getImportPolicyPeerTracker();
    }

    @Override
    public ExportPolicyPeerTracker getExportPolicyPeerTracker(final TablesKey tablesKey) {
        return this.ribImpl.getExportPolicyPeerTracker(tablesKey);
    }

    @Override
    public Set<TablesKey> getLocalTablesKeys() {
        return this.ribImpl.getLocalTablesKeys();
    }

    @Override
    public ServiceGroupIdentifier getRibIServiceGroupIdentifier() {
        return this.ribImpl.getRibIServiceGroupIdentifier();
    }

    @Override
    public String toString() {
        return this.ribImpl != null ? this.ribImpl.toString() : null;
    }

    private RIBImpl createRib(final ClusterSingletonServiceProvider provider, final Global global, final String bgpInstanceName,
        final BGPOpenConfigMappingService mappingService, final BgpDeployer.WriteConfiguration configurationWriter) {
        this.afiSafi = getAfiSafiWithDefault(global.getAfiSafis(), true);
        final Config globalConfig = global.getConfig();
        this.asNumber = globalConfig.getAs();
        this.routerId = globalConfig.getRouterId();
        this.clusterId = OpenConfigMappingUtil.getClusterIdentifier(globalConfig);
        final Map<TablesKey, PathSelectionMode> pathSelectionModes = mappingService.toPathSelectionMode(this.afiSafi).entrySet()
                .stream().collect(Collectors.toMap(entry -> new TablesKey(entry.getKey().getAfi(), entry.getKey().getSafi()), Map.Entry::getValue));
        return new RIBImpl(provider, new RibId(bgpInstanceName), this.asNumber, new BgpId(this.routerId), this.clusterId,
                this.extensions, this.dispatcher, this.codecTreeFactory, this.domBroker, mappingService.toTableTypes(this.afiSafi), pathSelectionModes,
                this.extensions.getClassLoadingStrategy(), configurationWriter);
    }

    @Override
    public ClusterSingletonServiceRegistration registerClusterSingletonService(final ClusterSingletonService clusterSingletonService) {
        return this.provider.registerClusterSingletonService(clusterSingletonService);
    }
}
