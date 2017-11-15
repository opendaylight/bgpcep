/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.bmp;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.OdlBmpMonitors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.BmpMonitorConfigKey;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpMonitorConfigFileProcessor implements ConfigFileProcessor, AutoCloseable {

    @VisibleForTesting
    static final InstanceIdentifier<OdlBmpMonitors> ODL_BMP_MONITORS_IID =
            InstanceIdentifier.create(OdlBmpMonitors.class);
    private static final Logger LOG = LoggerFactory.getLogger(BmpMonitorConfigFileProcessor.class);
    private final SchemaPath bmpMonitorsSchemaPath = SchemaPath.create(true, OdlBmpMonitors.QNAME);
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final YangInstanceIdentifier bmpMonitorsYii;
    private final ConfigLoader configLoader;
    private final DataBroker dataBroker;
    @GuardedBy("this")
    private AbstractRegistration registration;

    public BmpMonitorConfigFileProcessor(final ConfigLoader configLoader, final DataBroker dataBroker) {
        requireNonNull(configLoader);
        this.configLoader = requireNonNull(configLoader);
        this.dataBroker = requireNonNull(dataBroker);
        this.bindingSerializer = configLoader.getBindingNormalizedNodeSerializer();
        this.bmpMonitorsYii = this.bindingSerializer.toYangInstanceIdentifier(
                InstanceIdentifier.create(OdlBmpMonitors.class).child(BmpMonitorConfig.class));
    }

    private static void processBmpMonitorConfig(final BmpMonitorConfig bmpConfig, final WriteTransaction wtx) {
        final KeyedInstanceIdentifier<BmpMonitorConfig, BmpMonitorConfigKey> iid = ODL_BMP_MONITORS_IID
                .child(BmpMonitorConfig.class, bmpConfig.getKey());

        wtx.merge(LogicalDatastoreType.CONFIGURATION, iid, bmpConfig, true);
    }

    public synchronized void init() {
        this.registration = this.configLoader.registerConfigFile(this);
        LOG.info("BMP Config Loader service initiated");
    }

    @Override
    public synchronized void close() {
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
    }

    @Override
    public SchemaPath getSchemaPath() {
        return this.bmpMonitorsSchemaPath;
    }

    @Override
    public synchronized void loadConfiguration(final NormalizedNode<?, ?> dto) {
        final ContainerNode bmpMonitorsConfigsContainer = (ContainerNode) dto;
        final MapNode monitorsList = (MapNode) bmpMonitorsConfigsContainer.getChild(
                this.bmpMonitorsYii.getLastPathArgument()).orNull();
        if (monitorsList == null) {
            return;
        }
        final Collection<MapEntryNode> bmpMonitorConfig = monitorsList.getValue();
        final WriteTransaction wtx = this.dataBroker.newWriteOnlyTransaction();

        bmpMonitorConfig.stream().map(topology -> this.bindingSerializer
                .fromNormalizedNode(this.bmpMonitorsYii, topology))
                .filter(Objects::nonNull)
                .forEach(bi -> processBmpMonitorConfig((BmpMonitorConfig) bi.getValue(), wtx));

        try {
            wtx.submit().get();
        } catch (final ExecutionException | InterruptedException e) {
            LOG.warn("Failed to create Bmp config", e);
        }
    }
}
