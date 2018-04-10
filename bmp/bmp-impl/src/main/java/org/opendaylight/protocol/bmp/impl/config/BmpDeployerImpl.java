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
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.app.BmpMonitoringStationImpl;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitoringStation;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev180329.OdlBmpMonitors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev180329.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev180329.server.config.Server;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev180329.BmpMonitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev180329.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev180329.bmp.monitor.Monitor;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpDeployerImpl implements ClusteredDataTreeChangeListener<OdlBmpMonitors>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BmpDeployerImpl.class);

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private static final InstanceIdentifier<OdlBmpMonitors> ODL_BMP_MONITORS_IID =
            InstanceIdentifier.create(OdlBmpMonitors.class);
    private static final YangInstanceIdentifier BMP_MONITOR_YII =
            YangInstanceIdentifier.of(BmpMonitor.QNAME);
    private static final ContainerNode EMPTY_PARENT_NODE = Builders.containerBuilder().withNodeIdentifier(
            new NodeIdentifier(BmpMonitor.QNAME)).addChild(ImmutableNodes.mapNodeBuilder(Monitor.QNAME)
            .build()).build();
    private final BmpDispatcher dispatcher;
    @GuardedBy("this")
    private final Map<MonitorId, BmpMonitoringStationImpl> bmpMonitorServices = new HashMap<>();
    private final BmpDeployerDependencies bmpDeployerDependencies;
    @GuardedBy("this")
    private ListenerRegistration<BmpDeployerImpl> registration;

    public BmpDeployerImpl(final BmpDispatcher dispatcher, final BmpDeployerDependencies bmpDeployerDependencies) {
        this.dispatcher = requireNonNull(dispatcher);
        this.bmpDeployerDependencies = requireNonNull(bmpDeployerDependencies);
    }

    public synchronized void init() {
        final DOMDataWriteTransaction wTx = this.bmpDeployerDependencies.getDomDataBroker().newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.OPERATIONAL, BMP_MONITOR_YII, EMPTY_PARENT_NODE);
        wTx.submit();
        this.registration = this.bmpDeployerDependencies.getDataBroker().registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, ODL_BMP_MONITORS_IID), this);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<OdlBmpMonitors>> changes) {
        final DataTreeModification<OdlBmpMonitors> dataTreeModification = Iterables.getOnlyElement(changes);
        final Collection<DataObjectModification<? extends DataObject>> rootNode = dataTreeModification.getRootNode()
                .getModifiedChildren();
        if (rootNode.isEmpty()) {
            return;
        }
        rootNode.forEach(dto -> handleModification((DataObjectModification<BmpMonitorConfig>) dto));
    }

    private synchronized void handleModification(final DataObjectModification<BmpMonitorConfig> config) {
        final ModificationType modificationType = config.getModificationType();
        LOG.trace("Bmp Monitor configuration has changed: {}, type modification {}", config, modificationType);
        switch (modificationType) {
            case DELETE:
                removeBmpMonitor(config.getDataBefore().getMonitorId());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                updateBmpMonitor(config.getDataAfter());
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void updateBmpMonitor(final BmpMonitorConfig bmpConfig) {
        final MonitorId monitorId = bmpConfig.getMonitorId();
        final BmpMonitoringStationImpl oldService = this.bmpMonitorServices.remove(monitorId);
        try {
            if (oldService != null) {
                oldService.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                oldService.close();
            }

            final Server server = bmpConfig.getServer();
            final InetSocketAddress inetAddress =
                    Ipv4Util.toInetSocketAddress(server.getBindingAddress(), server.getBindingPort());
            final BmpMonitoringStationImpl monitor = new BmpMonitoringStationImpl(this.bmpDeployerDependencies,
                    this.dispatcher, monitorId, inetAddress, bmpConfig.getMonitoredRouter());
            this.bmpMonitorServices.put(monitorId, monitor);
        } catch (final Exception e) {
            LOG.error("Failed to create Bmp Monitor {}.", monitorId, e);
        }

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void removeBmpMonitor(final MonitorId monitorId) {
        final BmpMonitoringStation service = this.bmpMonitorServices.remove(monitorId);
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
    public synchronized void close() {
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
    }
}
