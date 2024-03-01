/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.config;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.app.BmpMonitoringStationImpl;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev200120.OdlBmpMonitors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev200120.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev200120.server.config.Server;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.BmpMonitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.bmp.monitor.Monitor;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = {})
public final class BmpDeployerImpl implements DataTreeChangeListener<OdlBmpMonitors>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BmpDeployerImpl.class);

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private static final InstanceIdentifier<OdlBmpMonitors> ODL_BMP_MONITORS_IID =
            InstanceIdentifier.create(OdlBmpMonitors.class);
    private static final YangInstanceIdentifier BMP_MONITOR_YII = YangInstanceIdentifier.of(BmpMonitor.QNAME);
    private static final ContainerNode EMPTY_PARENT_NODE = ImmutableNodes.newContainerBuilder()
        .withNodeIdentifier(new NodeIdentifier(BmpMonitor.QNAME))
        .addChild(ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(new NodeIdentifier(Monitor.QNAME)).build())
        .build();

    private final BmpDispatcher dispatcher;
    private final DOMDataBroker domDataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final BindingCodecTree codecTree;
    private final ClusterSingletonServiceProvider singletonProvider;

    @GuardedBy("this")
    private final Map<MonitorId, BmpMonitoringStationImpl> bmpMonitorServices = new HashMap<>();
    @GuardedBy("this")
    private Registration registration;

    @Activate
    @Inject
    public BmpDeployerImpl(@Reference final BmpDispatcher dispatcher, @Reference final DataBroker dataBroker,
            @Reference final DOMDataBroker domDataBroker, @Reference final RIBExtensionConsumerContext extensions,
            @Reference final BindingCodecTree codecTree,
            @Reference final ClusterSingletonServiceProvider singletonProvider) {
        this.dispatcher = requireNonNull(dispatcher);
        this.domDataBroker = requireNonNull(domDataBroker);
        this.extensions = requireNonNull(extensions);
        this.codecTree = requireNonNull(codecTree);
        this.singletonProvider = requireNonNull(singletonProvider);

        final DOMDataTreeWriteTransaction wTx = domDataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.OPERATIONAL, BMP_MONITOR_YII, EMPTY_PARENT_NODE);
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed commit", trw);
            }
        }, MoreExecutors.directExecutor());
        registration = dataBroker.registerTreeChangeListener(
            DataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, ODL_BMP_MONITORS_IID), this);
    }

    @Override
    public synchronized void onDataTreeChanged(final List<DataTreeModification<OdlBmpMonitors>> changes) {
        // FIXME: what about multiple events?!
        final var dataTreeModification = Iterables.getOnlyElement(changes);
        final var rootNode = dataTreeModification.getRootNode().modifiedChildren();
        if (rootNode.isEmpty()) {
            return;
        }
        rootNode.forEach(dto -> handleModification((DataObjectModification<BmpMonitorConfig>) dto));
    }

    private synchronized void handleModification(final DataObjectModification<BmpMonitorConfig> config) {
        final var modificationType = config.modificationType();
        LOG.trace("Bmp Monitor configuration has changed: {}, type modification {}", config, modificationType);
        switch (modificationType) {
            case DELETE:
                removeBmpMonitor(config.dataBefore().getMonitorId());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                updateBmpMonitor(config.dataAfter());
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void updateBmpMonitor(final BmpMonitorConfig bmpConfig) {
        final MonitorId monitorId = bmpConfig.getMonitorId();
        final BmpMonitoringStationImpl oldService = bmpMonitorServices.remove(monitorId);
        try {
            if (oldService != null) {
                oldService.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                oldService.close();
            }

            final Server server = bmpConfig.getServer();
            final InetSocketAddress inetAddress =
                    Ipv4Util.toInetSocketAddress(server.getBindingAddress(), server.getBindingPort());
            final BmpMonitoringStationImpl monitor = new BmpMonitoringStationImpl(domDataBroker, dispatcher,
                extensions, codecTree, singletonProvider, monitorId, inetAddress,
                bmpConfig.nonnullMonitoredRouter().values());
            bmpMonitorServices.put(monitorId, monitor);
        } catch (final Exception e) {
            LOG.error("Failed to create Bmp Monitor {}.", monitorId, e);
        }

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void removeBmpMonitor(final MonitorId monitorId) {
        final var service = bmpMonitorServices.remove(monitorId);
        if (service != null) {
            LOG.debug("Closing Bmp Monitor {}.", monitorId);
            try {
                service.close();
            } catch (final Exception e) {
                LOG.error("Failed to close Bmp Monitor {}.", monitorId, e);
            }
        }
    }

    @Override
    @Deactivate
    @PreDestroy
    public synchronized void close() {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }
}
