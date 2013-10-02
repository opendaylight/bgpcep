/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.impl.osgi;

import java.util.*;

import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfOperationRouter;
import org.opendaylight.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.netconf.impl.mapping.operations.DefaultCloseSession;
import org.opendaylight.netconf.impl.mapping.operations.DefaultCommit;
import org.opendaylight.netconf.impl.mapping.operations.DefaultGetSchema;
import org.opendaylight.netconf.mapping.api.*;
import org.opendaylight.netconf.util.xml.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class NetconfOperationRouterImpl implements NetconfOperationRouter {

	private static final Logger logger = LoggerFactory.getLogger(NetconfOperationRouterImpl.class);

	private final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot;

	private final Set<NetconfOperation> allNetconfOperations;
	private final TreeSet<NetconfOperationFilter> allSortedFilters;

	private final CapabilityProvider capabilityProvider;

	public NetconfOperationRouterImpl(NetconfOperationServiceSnapshot netconfOperationServiceSnapshot,
			CapabilityProvider capabilityProvider, DefaultCommitNotificationProducer commitNotifier) {

		this.netconfOperationServiceSnapshot = netconfOperationServiceSnapshot;

		this.capabilityProvider = capabilityProvider;

		Set<NetconfOperation> defaultNetconfOperations = Sets.newHashSet();
		defaultNetconfOperations.add(new DefaultGetSchema(capabilityProvider, netconfOperationServiceSnapshot.getNetconfSessionIdForReporting()));
		defaultNetconfOperations.add(new DefaultCloseSession(netconfOperationServiceSnapshot.getNetconfSessionIdForReporting()));

		allNetconfOperations = getAllNetconfOperations(defaultNetconfOperations, netconfOperationServiceSnapshot);

		DefaultCommit defaultCommit = new DefaultCommit(commitNotifier, capabilityProvider, netconfOperationServiceSnapshot.getNetconfSessionIdForReporting());
		Set<NetconfOperationFilter> defaultFilters = Sets.<NetconfOperationFilter> newHashSet(defaultCommit);
		allSortedFilters = getAllNetconfFilters(defaultFilters, netconfOperationServiceSnapshot);
	}

	private static Set<NetconfOperation> getAllNetconfOperations(Set<NetconfOperation> defaultNetconfOperations,
			NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
		Set<NetconfOperation> result = new HashSet<>();
		result.addAll(defaultNetconfOperations);

		for (NetconfOperationService netconfOperationService : netconfOperationServiceSnapshot.getServices()) {
			final Set<NetconfOperation> netOpsFromService = netconfOperationService.getNetconfOperations();
			for (NetconfOperation netconfOperation : netOpsFromService) {
				Preconditions.checkState(result.contains(netconfOperation) == false, "Netconf operation %s already present",
						netconfOperation);
				result.add(netconfOperation);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	private static TreeSet<NetconfOperationFilter> getAllNetconfFilters(Set<NetconfOperationFilter> defaultFilters,
			NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
		TreeSet<NetconfOperationFilter> result = new TreeSet<>(defaultFilters);
		for (NetconfOperationService netconfOperationService : netconfOperationServiceSnapshot.getServices()) {
			final Set<NetconfOperationFilter> filtersFromService = netconfOperationService.getFilters();
			for (NetconfOperationFilter filter : filtersFromService) {
				Preconditions.checkState(result.contains(filter) == false, "Filter %s already present", filter);
				result.add(filter);
			}
		}
		return result;
	}

	public CapabilityProvider getCapabilityProvider() {
		return capabilityProvider;
	}

	@Override
	public synchronized Document onNetconfMessage(Document message) throws NetconfDocumentedException {
		NetconfOperationExecution netconfOperationExecution = getNetconfOperationWithHighestPriority(message);
		logger.debug("Forwarding netconf message {} to {}", Xml.toString(message), netconfOperationExecution.operationWithHighestPriority);

		final LinkedList<NetconfOperationFilterChain> chain = new LinkedList<>();
		chain.push(netconfOperationExecution);

		for (Iterator<NetconfOperationFilter> it = allSortedFilters.descendingIterator(); it.hasNext();) {
			final NetconfOperationFilter filter = it.next();
			final NetconfOperationFilterChain prevItem = chain.getFirst();
			NetconfOperationFilterChain currentItem = new NetconfOperationFilterChain() {
				@Override
				public Document execute(Document message, NetconfOperationRouter operationRouter) throws NetconfDocumentedException {
					logger.trace("Entering {}", filter);
					return filter.doFilter(message, operationRouter, prevItem);
				}
			};
			chain.push(currentItem);
		}
		return chain.getFirst().execute(message, this);
	}

	private NetconfOperationExecution getNetconfOperationWithHighestPriority(Document message) {

		// TODO test
		TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority = getSortedNetconfOperationsWithCanHandle(message);

		Preconditions.checkState(sortedPriority.isEmpty() == false, "No %s available to handle message %s",
				NetconfOperation.class.getName(), Xml.toString(message));

		HandlingPriority highestFoundPriority = sortedPriority.lastKey();

		int netconfOperationsWithHighestPriority = sortedPriority.get(highestFoundPriority).size();

		Preconditions.checkState(netconfOperationsWithHighestPriority == 1, "Multiple %s available to handle message %s",
				NetconfOperation.class.getName(), message);

		return new NetconfOperationExecution(sortedPriority, highestFoundPriority);
	}

	private TreeMap<HandlingPriority, Set<NetconfOperation>> getSortedNetconfOperationsWithCanHandle(Document message) {
		TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority = Maps.newTreeMap();

		for (NetconfOperation netconfOperation : allNetconfOperations) {
			final HandlingPriority handlingPriority = netconfOperation.canHandle(message);

			if (handlingPriority.equals(HandlingPriority.CANNOT_HANDLE) == false) {
				Set<NetconfOperation> netconfOperations = sortedPriority.get(handlingPriority);
				netconfOperations = checkIfNoOperationsOnPriority(sortedPriority, handlingPriority, netconfOperations);
				netconfOperations.add(netconfOperation);
			}
		}
		return sortedPriority;
	}

	private Set<NetconfOperation> checkIfNoOperationsOnPriority(TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority,
			HandlingPriority handlingPriority, Set<NetconfOperation> netconfOperations) {
		if (netconfOperations == null) {
			netconfOperations = Sets.newHashSet();
			sortedPriority.put(handlingPriority, netconfOperations);
		}
		return netconfOperations;
	}

	@Override
	public void close() {
		netconfOperationServiceSnapshot.close();
	}

	private class NetconfOperationExecution implements NetconfOperationFilterChain {
		private final NetconfOperation operationWithHighestPriority;

		private NetconfOperationExecution(NetconfOperation operationWithHighestPriority) {
			this.operationWithHighestPriority = operationWithHighestPriority;
		}

		public NetconfOperationExecution(TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority,
				HandlingPriority highestFoundPriority) {
			operationWithHighestPriority = sortedPriority.get(highestFoundPriority).iterator().next();
			sortedPriority.remove(highestFoundPriority);
		}

		@Override
		public Document execute(Document message, NetconfOperationRouter router) throws NetconfDocumentedException {
			return operationWithHighestPriority.handle(message, router);
		}
	}

	@Override
	public String toString() {
		return "NetconfOperationRouterImpl{" + "netconfOperationServiceSnapshot=" + netconfOperationServiceSnapshot + '}';
	}
}
