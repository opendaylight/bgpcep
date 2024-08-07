/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.app;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.routers.Router;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RouterSessionManager implements BmpSessionListenerFactory, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RouterSessionManager.class);

    private final Map<RouterId, BmpRouter> sessionListeners = new ConcurrentHashMap<>();

    private final YangInstanceIdentifier yangRoutersId;
    private final DOMDataBroker domDataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final BindingCodecTree tree;

    RouterSessionManager(final YangInstanceIdentifier yangMonitorId, final DOMDataBroker domDataBroker,
            final RIBExtensionConsumerContext extensions, final BindingCodecTree tree) {
        this.domDataBroker = domDataBroker;
        yangRoutersId = YangInstanceIdentifier.builder(yangMonitorId).node(Router.QNAME).build();
        this.extensions = extensions;
        this.tree = tree;
    }

    @Override
    public BmpSessionListener getSessionListener() {
        return new BmpRouterImpl(this);
    }

    private synchronized boolean isSessionExist(final BmpRouter sessionListener) {
        requireNonNull(sessionListener);
        return sessionListeners.containsKey(requireNonNull(sessionListener.getRouterId()));
    }

    synchronized boolean addSessionListener(final BmpRouter sessionListener) {
        if (isSessionExist(sessionListener)) {
            LOG.warn("Session listener for router {} was already added.", sessionListener.getRouterId());
            return false;
        }
        sessionListeners.put(sessionListener.getRouterId(), sessionListener);
        return true;
    }

    synchronized void removeSessionListener(final BmpRouter sessionListener) {
        if (!isSessionExist(sessionListener)) {
            LOG.warn("Session listener for router {} was already removed.", sessionListener.getRouterId());
            return;
        }
        sessionListeners.remove(sessionListener.getRouterId());
    }

    @Override
    public void close() throws Exception {
        for (final BmpRouter sessionListener : sessionListeners.values()) {
            sessionListener.close();
        }
    }

    YangInstanceIdentifier getRoutersYangIId() {
        return yangRoutersId;
    }

    DOMDataBroker getDomDataBroker() {
        return domDataBroker;
    }

    RIBExtensionConsumerContext getExtensions() {
        return extensions;
    }

    BindingCodecTree getCodecTree() {
        return tree;
    }

}
