/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.reconnectstrategy;

import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

public class GlobalEventExecutorUtil {

	public static ObjectName createOrGetInstance(
            final ConfigTransactionJMXClient transaction)
			throws InstanceAlreadyExistsException {
        ObjectName on;
        try {
            on =  transaction.lookupConfigBean(GlobalEventExecutorModuleFactory.NAME, GlobalEventExecutorModuleFactory.SINGLETON_NAME);
        } catch (InstanceNotFoundException e) {
            on = transaction.createModule(GlobalEventExecutorModuleFactory.NAME, GlobalEventExecutorModuleFactory.SINGLETON_NAME);
        }
		return on;
	}

}
