/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

/**
 * Provides access to BGP AFI/SAFI registry.
 */
public interface BGPTableTypeRegistryConsumer {

    /**
     * Looks for BgpTableType based on OpenConfig AFI/SAFI.
     *
     * @param afiSafiType afiSafi Type
     * @return Optional of BgpTableType or empty, if the table type is not supported.
     */
    @Nonnull
    Optional<BgpTableType> getTableType(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Looks for BgpTableType based on OpenConfig AFI/SAFI.
     *
     * @param afiSafiType afiSafi Type
     * @return Optional of TableKey or empty, if the table type is not supported.
     */
    @Nonnull
    Optional<TablesKey> getTableKey(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Looks for AfiSafiType based on BgpTableType.
     *
     * @param bgpTableType Bgp TableType
     * @return Optional of OpenConfig AFI/SAFI or empty, if the table type is not supported.
     */
    @Nonnull
    Optional<Class<? extends AfiSafiType>> getAfiSafiType(@Nonnull BgpTableType bgpTableType);

    /**
     * Looks for AfiSafiType based on TablesKey.
     *
     * @param tablesKey Tables Key
     * @return Optional of OpenConfig AFI/SAFI or empty, if the table type is not supported.
     */
    @Nonnull
    Optional<Class<? extends AfiSafiType>> getAfiSafiType(@Nonnull TablesKey tablesKey);
}
