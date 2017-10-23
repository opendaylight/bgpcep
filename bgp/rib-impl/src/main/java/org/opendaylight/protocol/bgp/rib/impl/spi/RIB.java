/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Internal reference to a RIB instance.
 */
public interface RIB extends RibReference, ClusterSingletonServiceProvider {
    AsNumber getLocalAs();

    BgpId getBgpIdentifier();

    /**
     * Return the set of table identifiers which are accepted and advertised
     * by this RIB instance.
     *
     * @return A set of identifiers.
     */
    @Nonnull
    Set<? extends BgpTableType> getLocalTables();

    BGPDispatcher getDispatcher();

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
     * Return instance of DOMDataTreeChangeService, where consumer can register to
     * listen on DOM data changes.
     *
     * @return DOMDataTreeChangeService
     */
    DOMDataTreeChangeService getService();

    ImportPolicyPeerTracker getImportPolicyPeerTracker();

    /**
     * Returns ExportPolicyPeerTracker for specific tableKey, where peer can register himself
     * as supporting the table. Same export policy can be used to check which peers support respective
     * table and announce then routes if required.
     *
     * @param tablesKey supported table
     * @return ExportPolicyPeerTracker
     */
    ExportPolicyPeerTracker getExportPolicyPeerTracker(TablesKey tablesKey);

    Set<TablesKey> getLocalTablesKeys();

    /**
     * Return common ServiceGroupIdentifier to be used between same group cluster service
     *
     * @return ServiceGroupIdentifier
     */
    ServiceGroupIdentifier getRibIServiceGroupIdentifier();
}
