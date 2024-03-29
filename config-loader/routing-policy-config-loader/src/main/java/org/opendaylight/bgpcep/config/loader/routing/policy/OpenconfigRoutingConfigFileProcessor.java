/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.routing.policy;

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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

@Singleton
public final class OpenconfigRoutingConfigFileProcessor extends AbstractConfigFileProcessor {
    @Inject
    public OpenconfigRoutingConfigFileProcessor(final ConfigLoader configLoader, final DOMDataBroker dataBroker) {
        super("Routing Policy", configLoader, dataBroker);
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
        return Absolute.of(RoutingPolicy.QNAME);
    }

    @Override
    protected FluentFuture<? extends CommitInfo> loadConfiguration(final DOMDataBroker dataBroker,
            final NormalizedNode dto) {
        final var wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of(RoutingPolicy.QNAME), dto);
        return wtx.commit();
    }
}
