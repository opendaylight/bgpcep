/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.data.change.counter;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpcep.data.change.counter.config.rev170424.DataChangeCounterConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = { })
public final class TopologyDataChangeCounterDeployer implements DataTreeChangeListener<DataChangeCounterConfig>,
        AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounterDeployer.class);
    private static final InstanceIdentifier<DataChangeCounterConfig> DATA_CHANGE_COUNTER_IID =
            InstanceIdentifier.builder(DataChangeCounterConfig.class).build();

    private final @NonNull DataBroker dataBroker;
    @GuardedBy("this")
    private final Map<String, TopologyDataChangeCounter> counters = new HashMap<>();
    @GuardedBy("this")
    private ListenerRegistration<TopologyDataChangeCounterDeployer> registration;

    @Inject
    @Activate
    public TopologyDataChangeCounterDeployer(@Reference final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
        registration = dataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, DATA_CHANGE_COUNTER_IID), this);
        LOG.info("Data change counter Deployer started");
    }

    @Deactivate
    @PreDestroy
    @Override
    public synchronized void close() {
        if (registration != null) {
            registration.close();
            registration = null;
        }
        counters.values().forEach(TopologyDataChangeCounter::close);
        counters.clear();
        LOG.info("Data change counter Deployer stopped");
    }

    @Override
    public synchronized void onDataTreeChanged(
            final Collection<DataTreeModification<DataChangeCounterConfig>> changes) {
        for (var change : changes) {
            final var rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case DELETE:
                    deleteCounterChange(rootNode.getDataBefore().getCounterId());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    final var counterConfig = rootNode.getDataAfter();
                    handleCounterChange(counterConfig.getCounterId(), counterConfig.getTopologyName());
                    break;
                default:
                    LOG.error("Unhandled modification Type: {}", rootNode.getModificationType());
                    break;
            }
        }
    }

    private synchronized void deleteCounterChange(final String counterId) {
        final var oldCounter = counters.remove(counterId);
        if (oldCounter != null) {
            LOG.info("Data change counter Deployer deleted: {}", counterId);
            oldCounter.close();
        }
    }

    private synchronized void handleCounterChange(final String counterId, final String topologyName) {
        deleteCounterChange(counterId);
        LOG.info("Data change counter Deployer created: {} / {}", counterId, topologyName);
        counters.put(counterId, new TopologyDataChangeCounter(dataBroker, counterId, topologyName));
    }
}
