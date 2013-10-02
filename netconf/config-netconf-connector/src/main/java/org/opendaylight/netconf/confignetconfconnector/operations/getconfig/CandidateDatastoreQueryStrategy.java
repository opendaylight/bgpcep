/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.operations.getconfig;

import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.netconf.confignetconfconnector.transactions.TransactionProvider;

import javax.management.ObjectName;
import java.util.Set;

public class CandidateDatastoreQueryStrategy implements DatastoreQueryStrategy {

	private final TransactionProvider transactionProvider;

	public CandidateDatastoreQueryStrategy(TransactionProvider transactionProvider) {
		this.transactionProvider = transactionProvider;
	}

	@Override
	public Set<ObjectName> queryInstances(ConfigRegistryClient configRegistryClient) {
		ObjectName on = transactionProvider.getOrCreateTransaction();
		ConfigTransactionClient proxy = configRegistryClient.getConfigTransactionClient(on);
		return proxy.lookupConfigBeans();
	}

}
