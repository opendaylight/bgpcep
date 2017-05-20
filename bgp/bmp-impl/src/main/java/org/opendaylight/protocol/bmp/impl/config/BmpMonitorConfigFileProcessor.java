/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.config;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Objects;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigLoader;
import org.opendaylight.protocol.bmp.impl.spi.BmpDeployer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.OdlBmpMonitors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BmpMonitorConfigFileProcessor implements ConfigFileProcessor, AutoCloseable {
    private static final SchemaPath BMP_MONITOR_SCHEMA_PATH = SchemaPath.create(true, OdlBmpMonitors.QNAME);
    private final BmpDeployer bmpDeployer;
    private final BindingNormalizedNodeSerializer codec;
    private static final YangInstanceIdentifier BMP_CONFIG_YII = YangInstanceIdentifier.of(OdlBmpMonitors.QNAME)
        .node(BmpMonitorConfig.QNAME);
    private final ConfigLoader configLoader;
    private AbstractRegistration registration;

    public BmpMonitorConfigFileProcessor(final ConfigLoader configLoader, final BmpDeployer bmpDeployerImpl) {
        this.configLoader = Preconditions.checkNotNull(configLoader);
        this.bmpDeployer = Preconditions.checkNotNull(bmpDeployerImpl);
        this.codec = configLoader.getBindingNormalizedNodeSerializer();
    }

    public void register() {
        this.registration = this.configLoader.registerConfigFile(this);
    }

    @Override
    public SchemaPath getSchemaPath() {
        return BMP_MONITOR_SCHEMA_PATH;
    }

    @Override
    public void loadConfiguration(final NormalizedNode<?, ?> dto) {
        final Collection<MapEntryNode> odlBmpConfig = ((MapNode) dto).getValue();
        odlBmpConfig.stream().map(bmpMonitorConfig-> this.codec
            .fromNormalizedNode(BMP_CONFIG_YII, bmpMonitorConfig)).filter(Objects::nonNull)
            .forEach(bi->this.bmpDeployer.writeBmpMonitor((BmpMonitorConfig) bi.getValue()));
    }

    @Override
    public void close() throws Exception {
        if (this.registration != null) {
            this.registration.close();
        }
    }

}
