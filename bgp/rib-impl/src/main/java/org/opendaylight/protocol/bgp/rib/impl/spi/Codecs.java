/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Common interface for Codecs classes.
 *
 */
public interface Codecs {
    /**
     * Called when Codec tree is updated. Implementations should refresh codec context.
     *
     * @param tree BindingCodecTree
     */
    void onCodecTreeUpdated(BindingCodecTree tree);

    ContainerNode serializeUnreachNlri(MpUnreachNlri nlri);

    ContainerNode serializeReachNlri(MpReachNlri nlri);

    Attributes deserializeAttributes(NormalizedNode<?,?> attributes);

    ContainerNode serializeAttributes(Attributes pathAttr);
}
