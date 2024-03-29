/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.bmp;

import com.google.common.util.concurrent.FluentFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.bgpcep.config.loader.spi.AbstractConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev200120.OdlBmpMonitors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev200120.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

@Singleton
public final class BmpMonitorConfigFileProcessor extends AbstractConfigFileProcessor {
    @Inject
    public BmpMonitorConfigFileProcessor(final ConfigLoader configLoader, final DOMDataBroker dataBroker) {
        super("BMP", configLoader, dataBroker);
    }

    @PostConstruct
    public void init() {
        start();
    }

    @PreDestroy
    @Override
    public void close() {
        stop();
    }

    @Override
    public Absolute fileRootSchema() {
        return Absolute.of(OdlBmpMonitors.QNAME);
    }

    @Override
    protected FluentFuture<? extends CommitInfo> loadConfiguration(final DOMDataBroker dataBroker,
            final NormalizedNode dto) {
        final ContainerNode odlBmpMonitors = (ContainerNode) dto;
        final MapNode monitorsList = (MapNode) odlBmpMonitors.childByArg(new NodeIdentifier(BmpMonitorConfig.QNAME));
        if (monitorsList == null) {
            return CommitInfo.emptyFluentFuture();
        }

        final DOMDataTreeWriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.merge(LogicalDatastoreType.CONFIGURATION,
            YangInstanceIdentifier.of(new NodeIdentifier(OdlBmpMonitors.QNAME), monitorsList.name()),
            monitorsList);
        return wtx.commit();
    }
}
