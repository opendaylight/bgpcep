/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.app;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.impl.app.KeyConstructorUtil.constructKeys;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.config.BmpDeployerDependencies;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitoringStation;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev171207.odl.bmp.monitors.bmp.monitor.config.MonitoredRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.BmpMonitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.bmp.monitor.Monitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.routers.Router;
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
    private final List<MonitoredRouter> monitoredRouters;
    private final BmpDispatcher dispatcher;
    private final RouterSessionManager sessionManager;
    private final YangInstanceIdentifier yangMonitorId;
    private Channel channel;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;

    public BmpMonitoringStationImpl(final BmpDeployerDependencies bmpDeployerDependencies,
            final BmpDispatcher dispatcher, final MonitorId monitorId, final InetSocketAddress address,
            final List<MonitoredRouter> mrs) {
        this.domDataBroker = requireNonNull(bmpDeployerDependencies.getDomDataBroker());
        this.dispatcher = requireNonNull(dispatcher);
        this.monitorId = monitorId;
        this.monitoredRouters = mrs;
        this.address = requireNonNull(address);

        this.yangMonitorId = YangInstanceIdentifier.builder()
                .node(BmpMonitor.QNAME).node(Monitor.QNAME)
                .nodeWithKey(Monitor.QNAME, MONITOR_ID_QNAME, monitorId.getValue()).build();

        this.sessionManager = new RouterSessionManager(this.yangMonitorId, this.domDataBroker,
                bmpDeployerDependencies.getExtensions(), bmpDeployerDependencies.getTree());

        LOG.info("BMP Monitor Singleton Service {} registered, Monitor Id {}",
                getIdentifier().getValue(), this.monitorId.getValue());
        this.singletonServiceRegistration = bmpDeployerDependencies.getClusterSingletonProvider()
                .registerClusterSingletonService(this);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.info("BMP Monitor Singleton Service {} instantiated, Monitor Id {}",
                getIdentifier().getValue(), this.monitorId.getValue());

        final ChannelFuture channelFuture = this.dispatcher.createServer(this.address, this.sessionManager,
                constructKeys(this.monitoredRouters));
        try {
            this.channel = channelFuture.sync().channel();
            createEmptyMonitor();
            LOG.info("BMP Monitoring station {} started", this.monitorId.getValue());

            connectMonitoredRouters(this.dispatcher);
            LOG.info("Connecting to monitored routers completed.");
        } catch (final InterruptedException e) {
            LOG.error("Failed to instantiate BMP Monitor Singleton {}", this.monitorId.getValue(), e);
        }

    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        LOG.info("BMP Monitor Singleton Service {} instance closed, Monitor Id {}",
                getIdentifier().getValue(), this.monitorId.getValue());
        if (this.channel != null) {
            this.channel.close().addListener((ChannelFutureListener) future -> {
                Preconditions.checkArgument(future.isSuccess(),
                        "Channel failed to close: %s", future.cause());
                BmpMonitoringStationImpl.this.sessionManager.close();
            });
        }

        final DOMDataWriteTransaction wTx = this.domDataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.yangMonitorId);
        LOG.info("BMP monitoring station {} closed.", this.monitorId.getValue());
        return wTx.submit();
    }

    @Nonnull
    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SERVICE_GROUP_IDENTIFIER;
    }

    private void connectMonitoredRouters(final BmpDispatcher dispatcher) {
        if (this.monitoredRouters != null) {
            for (final MonitoredRouter mr : this.monitoredRouters) {
                if (mr.isActive()) {
                    requireNonNull(mr.getAddress());
                    requireNonNull(mr.getPort());
                    final String s = mr.getAddress().getIpv4Address().getValue();
                    final InetAddress addr = InetAddresses.forString(s);
                    final KeyMapping ret;
                    final Rfc2385Key rfc2385KeyPassword = mr.getPassword();
                    ret = KeyMapping.getKeyMapping(addr, rfc2385KeyPassword.getValue());
                    dispatcher.createClient(Ipv4Util.toInetSocketAddress(mr.getAddress(), mr.getPort()),
                            this.sessionManager, ret);
                }
            }
        }
    }

    private synchronized void createEmptyMonitor() {
        final DOMDataWriteTransaction wTx = this.domDataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                YangInstanceIdentifier.builder().node(BmpMonitor.QNAME).node(Monitor.QNAME)
                        .nodeWithKey(Monitor.QNAME, MONITOR_ID_QNAME, this.monitorId.getValue()).build(),
                ImmutableNodes.mapEntryBuilder(Monitor.QNAME, MONITOR_ID_QNAME, this.monitorId.getValue())
                        .addChild(ImmutableNodes.leafNode(MONITOR_ID_QNAME, this.monitorId.getValue()))
                        .addChild(ImmutableNodes.mapNodeBuilder(Router.QNAME).build())
                        .build());
        try {
            wTx.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            LOG.error("Failed to initiate BMP Monitor {}.", this.monitorId.getValue(), e);
        }
    }

    @Override
    public synchronized void close() throws Exception {
        if (this.singletonServiceRegistration != null) {
            this.singletonServiceRegistration.close();
            this.singletonServiceRegistration = null;
        }
    }
}
