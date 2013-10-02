/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.mapping.config;

import java.util.Map;

import org.opendaylight.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.netconf.confignetconfconnector.operations.editconfig.EditConfigStrategy;
import org.opendaylight.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser;
import org.opendaylight.netconf.confignetconfconnector.operations.editconfig.EditStrategyType;

import com.google.common.base.Preconditions;

/**
 * Parsed xml element containing whole configuration for an instance of some module. Contains preferred edit strategy
 * type.
 */
public class InstanceConfigElementResolved {

	private final EditStrategyType editStrategy;
	private final Map<String, AttributeConfigElement> configuration;

	public InstanceConfigElementResolved(String strat, Map<String, AttributeConfigElement> configuration) {
		EditStrategyType valueOf = checkStrategy(strat);
		this.editStrategy = valueOf;
		this.configuration = configuration;
	}

	EditStrategyType checkStrategy(String strat) {
		EditStrategyType valueOf = EditStrategyType.valueOf(strat);
		if (EditStrategyType.defaultStrategy().isEnforcing()) {
			Preconditions.checkArgument(valueOf == EditStrategyType.defaultStrategy(), "With " + EditStrategyType.defaultStrategy()
					+ " as " + EditConfigXmlParser.DEFAULT_OPERATION_KEY
					+ " operations on module elements are not permitted since the default option is restrictive");
		}
		return valueOf;
	}

	public InstanceConfigElementResolved(Map<String, AttributeConfigElement> configuration) {
		editStrategy = EditStrategyType.defaultStrategy();
		this.configuration = configuration;
	}

	public EditConfigStrategy getEditStrategy() {
		return editStrategy.getFittingStrategy();
	}

	public Map<String, AttributeConfigElement> getConfiguration() {
		return configuration;
	}
}
