/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.mapping.runtime;

import java.util.Collection;
import java.util.Set;

import javax.management.ObjectName;

import org.opendaylight.netconf.util.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class ModuleRuntime {

	private final String moduleName;
	private final InstanceRuntime instanceRuntime;

	public ModuleRuntime(String moduleName, InstanceRuntime instanceRuntime) {
		this.moduleName = moduleName;
		this.instanceRuntime = instanceRuntime;
	}

	public InstanceRuntime getMbeanMapping() {
		return instanceRuntime;
	}

	private ObjectName findRoot(Collection<ObjectName> runtimeBeanOns) {
		for (ObjectName objectName : runtimeBeanOns) {
			if (objectName.getKeyPropertyList().size() == 3)
				return objectName;
		}
		throw new IllegalStateException("Root runtime bean not found among " + runtimeBeanOns);
	}

	public Element toXml(String namespace, Multimap<String, ObjectName> instances, Document document) {
		Element root = document.createElement(Xml.MODULE_KEY);
		Xml.addNamespaceAttr(root, namespace);

		Element nameElement = Xml.createTextElement(document, Xml.NAME_KEY, moduleName);
		root.appendChild(nameElement);

		for (String instanceName : instances.keySet()) {
			Element instance = document.createElement(Xml.INSTANCE_KEY);

			Element innerNameElement = Xml.createTextElement(document, Xml.NAME_KEY, instanceName);
			instance.appendChild(innerNameElement);

			Collection<ObjectName> runtimeBeanOns = instances.get(instanceName);
			ObjectName rootName = findRoot(runtimeBeanOns);

			Set<ObjectName> childrenRuntimeBeans = Sets.newHashSet(runtimeBeanOns);
			childrenRuntimeBeans.remove(rootName);

			instance.appendChild(instanceRuntime.toXml(rootName, childrenRuntimeBeans, document));

			root.appendChild(instance);
		}

		return root;
	}

}
