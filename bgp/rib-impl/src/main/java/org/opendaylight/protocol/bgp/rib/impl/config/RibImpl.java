/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.ServiceRegistration;

public final class RibImpl implements RIB, AutoCloseable {

    private final RIBExtensionConsumerContext extensions;
    private final BGPDispatcher dispatcher;
    private final BindingCodecTreeFactory codecTreeFactory;
    private final DataBroker dataBroker;
    private final DOMDataBroker domBroker;
    private final BGPOpenConfigMappingService mappingService;
    private RIBImpl ribImpl;
    private ServiceRegistration<?> serviceRegistration;

    public RibImpl(final RIBExtensionConsumerContext contextProvider, final BGPDispatcher dispatcher,
            final BindingCodecTreeFactory codecTreeFactory, final DataBroker dataBroker, final DOMDataBroker domBroker,
            final BGPOpenConfigMappingService mappingService) {
        this.extensions = contextProvider;
        this.dispatcher = dispatcher;
        this.codecTreeFactory = codecTreeFactory;
        this.dataBroker = dataBroker;
        this.domBroker = domBroker;
        this.mappingService = mappingService;
    }

    void start(final Global global, final String instanceName) {
        this.ribImpl = createRib(global, instanceName);
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
    public Ipv4Address getBgpIdentifier() {
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
    public long getRoutesCount(final TablesKey key) {
        return this.ribImpl.getRoutesCount(key);
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
    public CacheDisconnectedPeers getCacheDisconnectedPeers() {
        return this.ribImpl.getCacheDisconnectedPeers();
    }

    @Override
    public DOMDataTreeChangeService getService() {
        return this.ribImpl.getService();
    }

    @Override
    public void close() {
        this.ribImpl.close();
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
        }
    }

    private RIBImpl createRib(final Global global, final String bgpInstanceName) {
        final Map<TablesKey, PathSelectionMode> pathSelectionModes = this.mappingService.toPathSelectionMode(global.getAfiSafis().getAfiSafi()).entrySet()
                .stream().collect(Collectors.toMap(entry -> new TablesKey(entry.getKey().getAfi(), entry.getKey().getSafi()), entry -> entry.getValue()));
        return new RIBImpl(new RibId(bgpInstanceName), new AsNumber(global.getConfig().getAs().getValue()),
                new Ipv4Address(global.getConfig().getRouterId().getValue()), new Ipv4Address(global.getConfig().getRouterId().getValue()),
                this.extensions, this.dispatcher, this.codecTreeFactory, this.dataBroker, this.domBroker,
                this.mappingService.toTableTypes(global.getAfiSafis().getAfiSafi()), pathSelectionModes,
                this.extensions.getClassLoadingStrategy());
    }

    public void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

}
