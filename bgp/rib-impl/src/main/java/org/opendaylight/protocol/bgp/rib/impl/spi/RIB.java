/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.base.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Internal reference to a RIB instance.
 */
public interface RIB  extends RibReference {
    AsNumber getLocalAs();

    Ipv4Address getBgpIdentifier();

    /**
     * Return the set of table identifiers which are accepted and advertised
     * by this RIB instance.
     *
     * @return A set of identifiers.
     */
    @Nonnull Set<? extends BgpTableType> getLocalTables();

    BGPDispatcher getDispatcher();

    long getRoutesCount(TablesKey key);

    /**
     * Allocate a new transaction chain for use with a peer.
     *
     * @param listener {@link TransactionChainListener} handling recovery
     * @return A new transaction chain.
     */
    DOMTransactionChain createPeerChain(TransactionChainListener listener);

    /**
     * Return the RIB extensions available to the RIB instance.
     *
     * @return RIB extensions handle.
     */
    RIBExtensionConsumerContext getRibExtensions();

    /**
     * Return the RIB extensions available to the RIB instance
     * with additional RIB specific context such as
     * translation between DOM and Binding.
     *
     * @return RIB extensions handle.
     */
    RIBSupportContextRegistry getRibSupportContext();

    /**
     * Return YangInstanceIdentifier of BGP Rib with its RibId.
     *
     * @return YangInstanceIdentifier
     */
    YangInstanceIdentifier getYangRibId();

    CodecsRegistry getCodecsRegistry();

    /**
     * Optionally returns OpenConfigProvider, which brings an access to
     * BGP OpenConfig mappers.
     * @return An Optional of BGPOpenConfigProvider or Absent if provider is
     * not available.
     */
    Optional<BGPOpenConfigProvider> getOpenConfigProvider();

    /**
     * Return cache disconnected peers which allows us to avoid update
     * DS from a peer already disconnected, when multiple peers are disconnected
     * at the same time and their own exportPolicy has not been updated yet.
     * @return
     */
    CacheDisconnectedPeers getCacheDisconnectedPeers();

    /**
     * Return instance of DOMDataTreeChangeService, where consumer can register to
     * listen on DOM data changes.
     * @return DOMDataTreeChangeService
     */
    DOMDataTreeChangeService getService();
}
