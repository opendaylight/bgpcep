/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.data.change.counter;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpcep.data.change.counter.config.rev170424.DataChangeCounterConfig;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = { })
public final class TopologyDataChangeCounterDeployer
        implements DataTreeChangeListener<DataChangeCounterConfig>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounterDeployer.class);

    private final Map<String, TopologyDataChangeCounter> counters = new HashMap<>();
    private final @NonNull DataBroker dataBroker;

    private Registration registration;

    @Inject
    @Activate
    public TopologyDataChangeCounterDeployer(@Reference final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
        registration = dataBroker.registerTreeChangeListener(LogicalDatastoreType.CONFIGURATION,
            DataObjectReference.builder(DataChangeCounterConfig.class).build(), this);
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
    public synchronized void onDataTreeChanged(final List<DataTreeModification<DataChangeCounterConfig>> changes) {
        for (var change : changes) {
            final var rootNode = change.getRootNode();
            switch (rootNode.modificationType()) {
                case DELETE:
                    deleteCounterChange(rootNode.coerceKeyStep(DataChangeCounterConfig.class).key().getCounterId());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    final var counterConfig = rootNode.dataAfter();
                    handleCounterChange(counterConfig.getCounterId(), counterConfig.getTopologyName());
                    break;
                default:
                    LOG.error("Unhandled modification Type: {}", rootNode.modificationType());
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
