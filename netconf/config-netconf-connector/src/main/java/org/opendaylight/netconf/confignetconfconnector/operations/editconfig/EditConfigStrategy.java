/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.operations.editconfig;

import java.util.Map;

import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;

public interface EditConfigStrategy {

	void executeConfiguration(String module, String instance, Map<String, AttributeConfigElement> configuration, ConfigTransactionClient ta);

}
