/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.mapping.runtime;

import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class Runtime {

	private final Map<String, Map<String, ModuleRuntime>> moduleRuntimes;

	public Runtime(Map<String, Map<String, ModuleRuntime>> moduleRuntimes) {
		this.moduleRuntimes = moduleRuntimes;
	}

	private Map<String, Multimap<String, ObjectName>> mapInstancesToModules(Set<ObjectName> instancesToMap) {
		Map<String, Multimap<String, ObjectName>> retVal = Maps.newHashMap();

		for (ObjectName objectName : instancesToMap) {
			String moduleName = ObjectNameUtil.getFactoryName(objectName);

			Multimap<String, ObjectName> multimap = retVal.get(moduleName);
			if (multimap == null) {
				multimap = HashMultimap.create();
				retVal.put(moduleName, multimap);
			}

			String instanceName = ObjectNameUtil.getInstanceName(objectName);

			multimap.put(instanceName, objectName);
		}

		return retVal;
	}

	public Element toXml(Set<ObjectName> instancesToMap, Document document) {
		Element root = document.createElement(Xml.DATA_KEY);

		Element modulesElement = document.createElement(Xml.MODULES_KEY);
		Xml.addNamespaceAttr(modulesElement, XMLUtil.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
		root.appendChild(modulesElement);

		Map<String, Multimap<String, ObjectName>> moduleToInstances = mapInstancesToModules(instancesToMap);

		for (String localNamespace : moduleRuntimes.keySet()) {
			for (String moduleName : moduleRuntimes.get(localNamespace).keySet()) {
				Multimap<String, ObjectName> instanceToRbe = moduleToInstances.get(moduleName);

				if (instanceToRbe == null)
					continue;

				ModuleRuntime moduleRuntime = moduleRuntimes.get(localNamespace).get(moduleName);
				Element innerXml = moduleRuntime.toXml(localNamespace, instanceToRbe, document);
				modulesElement.appendChild(innerXml);
			}
		}

		return root;
	}

}
