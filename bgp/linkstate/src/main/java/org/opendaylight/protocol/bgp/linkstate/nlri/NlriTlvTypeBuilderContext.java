/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;

public final class NlriTlvTypeBuilderContext {
    private final NodeDescriptorsBuilder nodebuilder = new NodeDescriptorsBuilder();
    private final LocalNodeDescriptorsBuilder lnbuilder = new LocalNodeDescriptorsBuilder();
    private final RemoteNodeDescriptorsBuilder rmbuilder = new RemoteNodeDescriptorsBuilder();
    private final AdvertisingNodeDescriptorsBuilder nodeprefbuilder = new AdvertisingNodeDescriptorsBuilder();
    private final LinkDescriptorsBuilder linkdescbuilder = new LinkDescriptorsBuilder();
    private final PrefixDescriptorsBuilder prefixdescbuilder = new PrefixDescriptorsBuilder();

    private boolean isLocal = false;

    public NlriTlvTypeBuilderContext () {
    }

    public NlriTlvTypeBuilderContext (final boolean isLocal) {
        this.isLocal = isLocal;
    }

    public final NodeDescriptorsBuilder getNodeDescriptorsBuilder() {
        return this.nodebuilder;
    }

    public final RemoteNodeDescriptorsBuilder getRemoteNodeDescBuilder() {
        return this.rmbuilder;
    }

    public final LocalNodeDescriptorsBuilder getLocalNodeDescBuilder() {
        return this.lnbuilder;
    }

    public final LinkDescriptorsBuilder getLinkDescriptorsBuilder() {
        return this.linkdescbuilder;
    }

    public final PrefixDescriptorsBuilder getPrefixDescriptorsBuilder() {
        return this.prefixdescbuilder;
    }

    public final AdvertisingNodeDescriptorsBuilder getAdvertisingNodeDescriptorsBuilder() {
        return this.nodeprefbuilder;
    }

    public boolean isLocal() {
        return this.isLocal;
    }
}

