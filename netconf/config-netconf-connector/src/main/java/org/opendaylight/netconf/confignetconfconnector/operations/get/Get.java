/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.operations.get;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.netconf.confignetconfconnector.mapping.config.InstanceConfig;
import org.opendaylight.netconf.confignetconfconnector.mapping.runtime.InstanceRuntime;
import org.opendaylight.netconf.confignetconfconnector.mapping.runtime.ModuleRuntime;
import org.opendaylight.netconf.confignetconfconnector.mapping.runtime.Runtime;
import org.opendaylight.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Maps;

public class Get extends AbstractConfigNetconfOperation {

	public static final String GET = "get";

	private final YangStoreSnapshot yangStoreSnapshot;
	private static final Logger logger = LoggerFactory.getLogger(Get.class);

	public Get(YangStoreSnapshot yangStoreSnapshot, ConfigRegistryClient configRegistryClient, String netconfSessionIdForReporting) {
		super(configRegistryClient, netconfSessionIdForReporting);
		this.yangStoreSnapshot = yangStoreSnapshot;
	}

	private Map<String, Map<String, ModuleRuntime>> createModuleRuntimes(ConfigRegistryClient configRegistryClient,
			Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries) {
		Map<String, Map<String, ModuleRuntime>> retVal = Maps.newHashMap();

		for (String namespace : mBeanEntries.keySet()) {

			Map<String, ModuleRuntime> innerMap = Maps.newHashMap();
			Map<String, ModuleMXBeanEntry> entriesFromNamespace = mBeanEntries.get(namespace);
			for (String module : entriesFromNamespace.keySet()) {

				ModuleMXBeanEntry mbe = entriesFromNamespace.get(module);

				Map<RuntimeBeanEntry, InstanceConfig> cache = Maps.newHashMap();
				RuntimeBeanEntry root = null;
				for (RuntimeBeanEntry rbe : mbe.getRuntimeBeans()) {
					cache.put(rbe, new InstanceConfig(configRegistryClient, rbe.getYangPropertiesToTypesMap()));
					if (rbe.isRoot())
						root = rbe;
				}

				if (root == null)
					continue;

				InstanceRuntime rootInstanceRuntime = createInstanceRuntime(root, cache);
				ModuleRuntime moduleRuntime = new ModuleRuntime(module, rootInstanceRuntime);
				innerMap.put(module, moduleRuntime);
			}

			retVal.put(namespace, innerMap);
		}
		return retVal;
	}

	private InstanceRuntime createInstanceRuntime(RuntimeBeanEntry root, Map<RuntimeBeanEntry, InstanceConfig> cache) {
		Map<String, InstanceRuntime> children = Maps.newHashMap();
		for (RuntimeBeanEntry child : root.getChildren()) {
			children.put(child.getJavaNamePrefix(), createInstanceRuntime(child, cache));
		}

		return new InstanceRuntime(cache.get(root), children, createJmxToYangMap(root.getChildren()));
	}

	private Map<String, String> createJmxToYangMap(List<RuntimeBeanEntry> children) {
		Map<String, String> jmxToYangNamesForChildRbe = Maps.newHashMap();
		for (RuntimeBeanEntry rbe : children) {
			jmxToYangNamesForChildRbe.put(rbe.getJavaNamePrefix(), rbe.getYangName());
		}
		return jmxToYangNamesForChildRbe;
	}

	private static void checkXml(XmlElement xml) {
		xml.checkName(GET);
		xml.checkNamespace(XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

		// Filter option - unsupported
		if (xml.getChildElements(Xml.FILTER).size() != 0)
			throw new UnsupportedOperationException("Unsupported option " + Xml.FILTER + " for " + GET);
	}

	@Override
	protected String getOperationName() {
		return GET;
	}

	@Override
	protected Element handle(Document document, XmlElement xml) throws NetconfDocumentedException {
		try {
			checkXml(xml);
		} catch (final IllegalArgumentException e) {
			logger.warn("Error parsing xml", e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(ErrorTag.bad_attribute.name(), e.getMessage());
			throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.rpc, ErrorTag.bad_attribute, ErrorSeverity.error, errorInfo);
		} catch (final UnsupportedOperationException e) {
			logger.warn("Unsupported", e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(ErrorTag.operation_not_supported.name(), "Unsupported option for 'get'");
			throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_not_supported, ErrorSeverity.error, errorInfo);
		}
		final Set<ObjectName> runtimeBeans = configRegistryClient.lookupRuntimeBeans();
		final Map<String, Map<String, ModuleRuntime>> moduleMappings = createModuleRuntimes(configRegistryClient,
				yangStoreSnapshot.getModuleMXBeanEntryMap());
		final Runtime runtime = new Runtime(moduleMappings);

		final Element element = runtime.toXml(runtimeBeans, document);

		logger.info("{} operation successful", GET);

		return element;
	}
}
