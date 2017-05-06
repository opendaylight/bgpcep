package org.opendaylight.bgpcep.programming.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.OdlProgramming;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.OdlProgrammingBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoClass implements ClusteredDataTreeChangeListener<OdlProgramming>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DemoClass.class);

    private final InstanceIdentifier<OdlProgramming> iid;
    private final ListenerRegistration<DemoClass> registration;

    DemoClass(final DataBroker dataProvider) {
        this.iid = InstanceIdentifier.create(OdlProgramming.class);

        LOG.info("Instantiating Instruction Deployer {}", this.iid);
        final WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, this.iid, new OdlProgrammingBuilder()
            .setOdlProgrammingConfig(Collections.emptyList()).build());
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Instance {} initialized successfully.", DemoClass.this.iid);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize Instruction Instance {}.", DemoClass.this.iid, t);
            }
        });

        this.registration = dataProvider.registerDataTreeChangeListener(
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, this.iid), this);
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.info("Closing Demo Class {}", this.iid);
        this.registration.close();
    }

    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<OdlProgramming>> collection) {

    }

    @VisibleForTesting
    InstanceIdentifier<OdlProgramming> getInstructionIID() {
        return this.iid;
    }
}
