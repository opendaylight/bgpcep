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
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpcep.data.change.counter.config.rev170424.DataChangeCounterConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyDataChangeCounterDeployer implements DataTreeChangeListener<DataChangeCounterConfig>,
    AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounterDeployer.class);
    private static final InstanceIdentifier<DataChangeCounterConfig> DATA_CHANGE_COUNTER_IID =
        InstanceIdentifier.builder(DataChangeCounterConfig.class).build();
    private final DataBroker dataBroker;
    @GuardedBy("this")
    private final Map<String, TopologyDataChangeCounter> counters = new HashMap<>();
    private ListenerRegistration<TopologyDataChangeCounterDeployer> registration;

    public TopologyDataChangeCounterDeployer(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    public synchronized void register() {
        this.registration = this.dataBroker.registerDataTreeChangeListener(
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, DATA_CHANGE_COUNTER_IID), this);
        LOG.info("Data change counter Deployer initiated");
    }


    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<DataChangeCounterConfig>> changes) {
        for (final DataTreeModification<DataChangeCounterConfig> dataTreeModification : changes) {
            final DataObjectModification<DataChangeCounterConfig> rootNode = dataTreeModification.getRootNode();
            switch (dataTreeModification.getRootNode().getModificationType()) {
                case DELETE:
                    deleteCounterChange(rootNode.getDataBefore().getCounterId());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    final DataChangeCounterConfig change = rootNode.getDataAfter();
                    chandleCounterChange(change.getCounterId(), change.getTopologyName());
                    break;
            }
        }
    }

    public synchronized void deleteCounterChange(final String counterId) {
        final TopologyDataChangeCounter oldCounter = this.counters.remove(counterId);
        if (oldCounter != null) {
            LOG.info("Data change counter Deployer deleted: {}", counterId);
            oldCounter.close();
        }
    }

    public synchronized void chandleCounterChange(final String counterId, final String topologyName) {
        deleteCounterChange(counterId);
        LOG.info("Data change counter Deployer created: {} / {}", counterId, topologyName);

        final TopologyDataChangeCounter counter = new TopologyDataChangeCounter(this.dataBroker, counterId, topologyName);
        this.counters.put(counterId, counter);
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.info("Closing Data change counter Deployer");

        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
    }
}
