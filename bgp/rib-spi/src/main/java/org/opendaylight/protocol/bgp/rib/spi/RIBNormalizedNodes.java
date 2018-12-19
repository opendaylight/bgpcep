/*
 * Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Utility constant {@link NormalizedNode}s.
 */
public final class RIBNormalizedNodes {
    public static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_FALSE = ImmutableNodes.leafNode(
        UPTODATE_NID, Boolean.FALSE);
    public static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_TRUE = ImmutableNodes.leafNode(
        UPTODATE_NID, Boolean.TRUE);

    public static final ContainerNode NOT_UPTODATE_ATTRIBUTES = Builders.containerBuilder()
            .withNodeIdentifier(ATTRIBUTES_NID).withChild(ATTRIBUTES_UPTODATE_FALSE).build();
    public static final ContainerNode UPTODATE_ATTRIBUTES = Builders.containerBuilder()
            .withNodeIdentifier(ATTRIBUTES_NID).withChild(ATTRIBUTES_UPTODATE_TRUE).build();

    private RIBNormalizedNodes() {

    }
}
