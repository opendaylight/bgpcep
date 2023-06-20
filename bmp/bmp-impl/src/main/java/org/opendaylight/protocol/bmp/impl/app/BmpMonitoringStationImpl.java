/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.app;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FluentFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitoringStation;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev200120.odl.bmp.monitors.bmp.monitor.config.MonitoredRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.BmpMonitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.bmp.monitor.Monitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpMonitoringStationImpl implements BmpMonitoringStation, ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMonitoringStationImpl.class);

    private static final QName MONITOR_ID_QNAME = QName.create(Monitor.QNAME, "monitor-id").intern();
    private static final ServiceGroupIdentifier SERVICE_GROUP_IDENTIFIER =
            ServiceGroupIdentifier.create("bmp-monitors-service-group");

    private final DOMDataBroker domDataBroker;
    private final InetSocketAddress address;
    private final MonitorId monitorId;
    private final Collection<MonitoredRouter> monitoredRouters;
    private final BmpDispatcher dispatcher;
    private final RouterSessionManager sessionManager;
    private final YangInstanceIdentifier yangMonitorId;
    private Channel channel;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;

    public BmpMonitoringStationImpl(final DOMDataBroker domDataBroker, final BmpDispatcher dispatcher,
            final RIBExtensionConsumerContext extensions, final BindingCodecTree codecTree,
            final ClusterSingletonServiceProvider singletonProvider, final MonitorId monitorId,
            final InetSocketAddress address, final Collection<MonitoredRouter> mrs) {
        this.domDataBroker = requireNonNull(domDataBroker);
        this.dispatcher = requireNonNull(dispatcher);
        this.monitorId = monitorId;
        monitoredRouters = mrs;
        this.address = requireNonNull(address);

        yangMonitorId = YangInstanceIdentifier.builder()
                .node(BmpMonitor.QNAME).node(Monitor.QNAME)
                .nodeWithKey(Monitor.QNAME, MONITOR_ID_QNAME, monitorId.getValue()).build();

        sessionManager = new RouterSessionManager(yangMonitorId, this.domDataBroker, extensions, codecTree);

        LOG.info("BMP Monitor Singleton Service {} registered, Monitor Id {}",
                getIdentifier().getName(), this.monitorId.getValue());
        singletonServiceRegistration = singletonProvider.registerClusterSingletonService(this);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.info("BMP Monitor Singleton Service {} instantiated, Monitor Id {}",
                getIdentifier().getName(), monitorId.getValue());

        final ChannelFuture channelFuture = dispatcher.createServer(address, sessionManager,
                constructKeys(monitoredRouters));
        try {
            channel = channelFuture.sync().channel();
            createEmptyMonitor();
            LOG.info("BMP Monitoring station {} started", monitorId.getValue());

            connectMonitoredRouters(dispatcher);
            LOG.info("Connecting to monitored routers completed.");
        } catch (final InterruptedException e) {
            LOG.error("Failed to instantiate BMP Monitor Singleton {}", monitorId.getValue(), e);
        }

    }

    private static KeyMapping constructKeys(final Collection<MonitoredRouter> mrs) {
        if (mrs == null || mrs.isEmpty()) {
            return KeyMapping.of();
        }

        final Map<InetAddress, String> passwords = new HashMap<>();
        for (MonitoredRouter mr : mrs) {
            if (mr != null) {
                final Rfc2385Key password = mr.getPassword();
                if (password != null && !password.getValue().isEmpty()) {
                    passwords.put(IetfInetUtil.inetAddressForNoZone(mr.getAddress()), password.getValue());
                }
            }
        }

        return KeyMapping.of(passwords);
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        LOG.info("BMP Monitor Singleton Service {} instance closed, Monitor Id {}",
                getIdentifier().getName(), monitorId.getValue());
        if (channel != null) {
            channel.close().addListener((ChannelFutureListener) future -> {
                Preconditions.checkArgument(future.isSuccess(),
                        "Channel failed to close: %s", future.cause());
                BmpMonitoringStationImpl.this.sessionManager.close();
            });
        }

        final DOMDataTreeWriteTransaction wTx = domDataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, yangMonitorId);
        LOG.info("BMP monitoring station {} closed.", monitorId.getValue());
        return wTx.commit();
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SERVICE_GROUP_IDENTIFIER;
    }

    private void connectMonitoredRouters(final BmpDispatcher pdispatcher) {
        if (monitoredRouters != null) {
            for (final MonitoredRouter mr : monitoredRouters) {
                if (mr.getActive()) {
                    requireNonNull(mr.getAddress());
                    requireNonNull(mr.getPort());
                    final String s = mr.getAddress().getIpv4AddressNoZone().getValue();
                    final InetAddress addr = InetAddresses.forString(s);
                    final KeyMapping ret;
                    final Rfc2385Key rfc2385KeyPassword = mr.getPassword();
                    ret = KeyMapping.of(addr, rfc2385KeyPassword.getValue());
                    pdispatcher.createClient(Ipv4Util.toInetSocketAddress(mr.getAddress(), mr.getPort()),
                            sessionManager, ret);
                }
            }
        }
    }

    private synchronized void createEmptyMonitor() {
        final DOMDataTreeWriteTransaction wTx = domDataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                YangInstanceIdentifier.builder().node(BmpMonitor.QNAME).node(Monitor.QNAME)
                        .nodeWithKey(Monitor.QNAME, MONITOR_ID_QNAME, monitorId.getValue()).build(),
                ImmutableNodes.mapEntryBuilder(Monitor.QNAME, MONITOR_ID_QNAME, monitorId.getValue())
                        .addChild(ImmutableNodes.leafNode(MONITOR_ID_QNAME, monitorId.getValue()))
                        .addChild(ImmutableNodes.mapNodeBuilder(Router.QNAME).build())
                        .build());
        try {
            wTx.commit().get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.error("Failed to initiate BMP Monitor {}.", monitorId.getValue(), e);
        }
    }

    @Override
    public synchronized void close() throws Exception {
        if (singletonServiceRegistration != null) {
            singletonServiceRegistration.close();
            singletonServiceRegistration = null;
        }
    }
}
