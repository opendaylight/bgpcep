/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;

/**
 * Interface for BGP RIB Routing Policy. Apply Import/Export Routing Policy to route attributes.
 */
public interface BGPRibRoutingPolicy {
    /**
     * Apply import policy to route attributes.
     *
     * @param policyParameters containing attributes and sender peer information
     * @param attributes       Route attributes
     * @param afiSafiType       Afi Safi Type
     * @return modified route attributes after apply policies
     */
    @NonNull Optional<Attributes> applyImportPolicies(@NonNull BGPRouteEntryImportParameters policyParameters,
            @NonNull Attributes attributes, @NonNull AfiSafiType afiSafiType);

    /**
     * Apply export policy to route attributes.
     *
     * @param policyParameters containing attributes and sender/receiver peer information
     * @param attributes       Route attributes
     * @param afiSafType       Afi Safi Type
     * @return modified route attributes after apply policies
     */
    @NonNull Optional<Attributes> applyExportPolicies(@NonNull BGPRouteEntryExportParameters policyParameters,
            @NonNull Attributes attributes, @NonNull AfiSafiType afiSafType);
}
