/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

public final class ModelTestUtil {
    public static DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> createContBuilder(final YangInstanceIdentifier
        .NodeIdentifier nid) {
        return ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(nid);
    }

    public static <T> ImmutableLeafNodeBuilder<T> createValueBuilder(final T value, YangInstanceIdentifier.NodeIdentifier nid) {
        final ImmutableLeafNodeBuilder<T> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(nid).withValue(value);
        return valueBuilder;
    }
}
