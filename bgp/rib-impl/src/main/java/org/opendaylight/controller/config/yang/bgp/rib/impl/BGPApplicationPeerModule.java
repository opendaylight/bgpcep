/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Application peer handler which handles translation from custom RIB into local RIB
 */
public class BGPApplicationPeerModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPApplicationPeerModule {
    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    private static final QName APP_ID_QNAME = QName.cachedReference(QName.create(Rib.QNAME, "id"));

    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.BGPApplicationPeerModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final YangInstanceIdentifier id = YangInstanceIdentifier.builder().node(ApplicationRib.QNAME).nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, getApplicationRibId()).build();
        final DOMDataTreeChangeService service = (DOMDataTreeChangeService) getDataBrokerDependency().getSupportedExtensions().get(DOMDataTreeChangeService.class);
        final ApplicationPeer peer = new ApplicationPeer(getApplicationRibId(), getBgpPeerId(), (RIBImpl) getTargetRibDependency());
        return service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, id), peer);
    }
}
