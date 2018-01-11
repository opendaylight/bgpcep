/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Provides support for serialize Route Attributes
 */
public interface AttributeBindingCodecSerializer {
    /**
     * @param ribSupport      ribSupport
     * @param routeIdentifier routeId
     * @param route           Route
     * @return attributes
     */
    @Nonnull
    Optional<Attributes> getAttributes(
            RIBSupport ribSupport,
            NodeIdentifierWithPredicates routeIdentifier,
            NormalizedNode<?, ?> route);

    /**
     * @param ribSupport      ribSupport
     * @param routeIdentifier route routeIdentifier
     * @param attributes      containing route attributes
     * @return ContainerNode
     */
    @Nullable
    ContainerNode toNormalizedNodeAttribute(
            RIBSupport ribSupport,
            NodeIdentifierWithPredicates routeIdentifier,
            Optional<Attributes> attributes);
}
