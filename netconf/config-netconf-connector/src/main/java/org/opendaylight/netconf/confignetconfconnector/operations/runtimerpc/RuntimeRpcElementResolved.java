/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.operations.runtimerpc;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.netconf.confignetconfconnector.mapping.rpc.ModuleRpcs;
import org.opendaylight.netconf.util.xml.Xml;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * Represents parsed xpath to runtime bean instance
 */
public final class RuntimeRpcElementResolved {
	private final String moduleName;
	private final String instanceName;
	private final String namespace;
	private final String runtimeBeanName;
	private final Map<String, String> additionalAttributes;

	private RuntimeRpcElementResolved(String namespace, String moduleName, String instanceName, String runtimeBeanName,
			Map<String, String> additionalAttributes) {
		this.moduleName = moduleName;
		this.instanceName = instanceName;
		this.additionalAttributes = additionalAttributes;
		this.namespace = namespace;
		this.runtimeBeanName = runtimeBeanName;
	}

	public String getModuleName() {
		return moduleName;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getRuntimeBeanName() {
		return runtimeBeanName;
	}

	public ObjectName getObjectName(ModuleRpcs rpcMapping) {
		Map<String, String> additionalAttributesJavaNames = Maps.newHashMapWithExpectedSize(additionalAttributes.size());
		for (String attributeYangName : additionalAttributes.keySet()) {
			String attributeJavaName = rpcMapping.getRbeJavaName(attributeYangName);
			Preconditions.checkState(attributeJavaName != null, "Cannot find java name for runtime bean wtih yang name %s",
					attributeYangName);
			additionalAttributesJavaNames.put(attributeJavaName, additionalAttributes.get(attributeYangName));
		}
		return ObjectNameUtil.createRuntimeBeanName(moduleName, instanceName, additionalAttributesJavaNames);
	}

	private static final String xpathPatternBlueprint = "/" + Xml.DATA_KEY + "/" + Xml.MODULES_KEY + "/" + Xml.MODULE_KEY + "\\["
			+ Xml.NAME_KEY + "='(.+)'\\]/" + Xml.INSTANCE_KEY + "\\[" + Xml.NAME_KEY + "='([^']+)'\\](.*)";
	private static final Pattern xpathPattern = Pattern.compile(xpathPatternBlueprint);
	private static final String additionalPatternBlueprint = "(.+)\\[(.+)='(.+)'\\]";
	private static final Pattern additionalPattern = Pattern.compile(additionalPatternBlueprint);

	public static RuntimeRpcElementResolved fromXpath(String xpath, String elementName, String namespace) {
		Matcher matcher = xpathPattern.matcher(xpath);
		Preconditions.checkState(matcher.matches(),
				"Node %s with value '%s' not in required form on rpc element %s, required format is %s", RuntimeRpc.CONTEXT_INSTANCE,
				xpath, elementName, xpathPatternBlueprint);

		String moduleName = matcher.group(1);
		String instanceName = matcher.group(2);
		String additionalString = matcher.group(3);
		HashMap<String, String> additionalAttributes = Maps.<String, String> newHashMap();
		String runtimeBeanYangName = moduleName;
		for (String additionalKeyValue : additionalString.split("/")) {
			if (Strings.isNullOrEmpty(additionalKeyValue))
				continue;
			matcher = additionalPattern.matcher(additionalKeyValue);
			Preconditions.checkState(matcher.matches(),
					"Attribute %s not in required form on rpc element %s, required format for additional attributes is  %s",
					additionalKeyValue, elementName, additionalPatternBlueprint);
			String name = matcher.group(1);
			runtimeBeanYangName = name;
			additionalAttributes.put(name, matcher.group(3));
		}

		return new RuntimeRpcElementResolved(namespace, moduleName, instanceName, runtimeBeanYangName, additionalAttributes);
	}
}
