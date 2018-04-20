/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

/**
 * The BGP extension may provide supported table type (AFI/SAFI).
 *
 */
public interface BGPTableTypeRegistryProvider extends  BGPTableTypeRegistryConsumer {

    /**
     * Register supported AFI/SAFI.
     * @param afi Local representation of AFI.
     * @param safi Local representation of SAFI.
     * @param afiSafiType OpenConfig AFI/SAFI representation.
     * @return Registration ticket.
     */
    @Nonnull AbstractRegistration registerBGPTableType(@Nonnull Class<? extends AddressFamily> afi,
            @Nonnull Class<? extends SubsequentAddressFamily> safi, @Nonnull Class<? extends AfiSafiType> afiSafiType);

}
