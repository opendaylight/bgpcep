/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigLoader;
import org.opendaylight.protocol.bmp.impl.api.BmpDeployer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.OdlBmpMonitors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpMonitorConfigFileProcessor implements ConfigFileProcessor, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMonitorConfigFileProcessor.class);

    private final SchemaPath bmpMonitorsSchemaPath = SchemaPath.create(true, OdlBmpMonitors.QNAME);
    private final BmpDeployer deployer;
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final AbstractRegistration registration;
    private final YangInstanceIdentifier bmpMonitorsYii;

    public BmpMonitorConfigFileProcessor(final ConfigLoader configLoader, final BmpDeployer deployer) {
        requireNonNull(configLoader);
        this.deployer = requireNonNull(deployer);
        this.bindingSerializer = configLoader.getBindingNormalizedNodeSerializer();
        this.bmpMonitorsYii = this.bindingSerializer.toYangInstanceIdentifier(
                InstanceIdentifier.create(OdlBmpMonitors.class).child(BmpMonitorConfig.class));
        this.registration = configLoader.registerConfigFile(this);
    }

    @Override
    public void close() throws Exception {
        this.registration.close();
    }

    @Nonnull
    @Override
    public SchemaPath getSchemaPath() {
        return this.bmpMonitorsSchemaPath;
    }

    @Override
    public void loadConfiguration(@Nonnull final NormalizedNode<?, ?> dto) {
        final ContainerNode bmpMonitorsConfigsContainer = (ContainerNode) dto;
        final MapNode monitorsList = (MapNode) bmpMonitorsConfigsContainer.getChild(
                this.bmpMonitorsYii.getLastPathArgument()).orNull();
        if (monitorsList == null) {
            return;
        }
        final Collection<MapEntryNode> bmpMonitorConfig = monitorsList.getValue();
        for (final MapEntryNode topology : bmpMonitorConfig) {
            final Map.Entry<InstanceIdentifier<?>, DataObject> bi = this.bindingSerializer
                    .fromNormalizedNode(this.bmpMonitorsYii, topology);
            if (bi != null) {
                final BmpMonitorConfig config = (BmpMonitorConfig) bi.getValue();
                try {
                    this.deployer.writeBmpMonitor(config);
                } catch (final TransactionCommitFailedException e) {
                    LOG.error("Failed to create BMP Monitor {}.", config, e);
                }
            }
        }
    }
}
