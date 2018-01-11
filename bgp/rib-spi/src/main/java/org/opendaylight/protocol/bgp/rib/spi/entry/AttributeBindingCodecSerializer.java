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
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Provides support for BA/BI Route Attributes conversion.
 */
public interface AttributeBindingCodecSerializer {
    /**
     * Convert BI Attributes to BA Attributes
     *
     * @param ribSupport      ribSupport
     * @param routeIdentifier route key
     * @param route           Route
     * @return attributes
     */
    @Nonnull
    Optional<Attributes> getAttributes(
            @Nonnull RIBSupport ribSupport,
            @Nonnull NodeIdentifierWithPredicates routeIdentifier,
            @Nonnull NormalizedNode<?, ?> route);

    /**
     * Convert BA Attributes to BI Attributes
     *
     * @param ribSupport      ribSupport
     * @param routeIdentifier route key
     * @param attributes      containing route attributes
     * @return ContainerNode
     */
    @Nonnull
    Optional<ContainerNode> toNormalizedNodeAttribute(
            @Nonnull RIBSupport ribSupport,
            @Nonnull NodeIdentifierWithPredicates routeIdentifier,
            @Nonnull Optional<Attributes> attributes);
}
