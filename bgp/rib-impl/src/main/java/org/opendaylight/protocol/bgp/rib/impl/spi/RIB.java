/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Internal reference to a RIB instance.
 */
public interface RIB extends RibReference, RibOutRefresh {
    /**
     * RIB AS.
     *
     * @return AS
     */
    AsNumber getLocalAs();

    BgpId getBgpIdentifier();

    /**
     * Return the set of table identifiers which are accepted and advertised
     * by this RIB instance.
     *
     * @return A set of identifiers.
     */
    @NonNull Set<? extends BgpTableType> getLocalTables();

    BGPDispatcher getDispatcher();

    /**
     * Allocate a new transaction chain for use with a peer.
     *
     * @param listener {@link TransactionChainListener} handling recovery
     * @return A new transaction chain.
     */
    DOMTransactionChain createPeerDOMChain(DOMTransactionChainListener listener);

    /**
     * Allocate a new transaction chain for use with a peer.
     *
     * @param listener {@link TransactionChainListener} handling recovery
     * @return A new transaction chain.
     */
    TransactionChain createPeerChain(TransactionChainListener listener);

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
     * Return instance of DOMDataTreeChangeService, where consumer can register to
     * listen on DOM data changes.
     *
     * @return DOMDataTreeChangeService
     */
    DOMDataTreeChangeService getService();

    /**
     * Return DataBroker.
     *
     * @return DataBroker
     */
    DataBroker getDataBroker();

    /**
     * Returns true if RIB supports table.
     *
     * @param tableKey table
     * @return true if supported
     */
    boolean supportsTable(TablesKey tableKey);

    Set<TablesKey> getLocalTablesKeys();

    /**
     * Return Policies Container.
     *
     * @return policies
     */
    BGPRibRoutingPolicy getRibPolicies();

    /**
     * Returns peer tracker for the rib.
     *
     * @return peer tracker
     */
    BGPPeerTracker getPeerTracker();
}
