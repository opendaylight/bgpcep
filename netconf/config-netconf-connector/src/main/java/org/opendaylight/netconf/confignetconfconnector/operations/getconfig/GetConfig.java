/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.confignetconfconnector.operations.getconfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.netconf.confignetconfconnector.mapping.config.Config;
import org.opendaylight.netconf.confignetconfconnector.mapping.config.InstanceConfig;
import org.opendaylight.netconf.confignetconfconnector.mapping.config.ModuleConfig;
import org.opendaylight.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.netconf.confignetconfconnector.operations.Datastore;
import org.opendaylight.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public class GetConfig extends AbstractConfigNetconfOperation {

	public static final String GET_CONFIG = "get-config";

	private final YangStoreSnapshot yangStoreSnapshot;
	private final Optional<String> maybeNamespace;

	private final TransactionProvider transactionProvider;

	private static final Logger logger = LoggerFactory.getLogger(GetConfig.class);

	public GetConfig(YangStoreSnapshot yangStoreSnapshot, Optional<String> maybeNamespace, TransactionProvider transactionProvider,
			ConfigRegistryClient configRegistryClient, String netconfSessionIdForReporting) {
		super(configRegistryClient, netconfSessionIdForReporting);
		this.yangStoreSnapshot = yangStoreSnapshot;
		this.maybeNamespace = maybeNamespace;
		this.transactionProvider = transactionProvider;
	}

	public static Datastore fromXml(XmlElement xml) {
		xml.checkName(GET_CONFIG);
		xml.checkNamespace(XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

		XmlElement sourceElement = xml.getOnlyChildElement(Xml.SOURCE_KEY, XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
		XmlElement sourceNode = sourceElement.getOnlyChildElement();
		String sourceParsed = sourceNode.getName();
		logger.debug("Setting source datastore to '{}'", sourceParsed);
		Datastore sourceDatastore = Datastore.valueOf(sourceParsed);

		// Filter option - unsupported
		if (xml.getChildElements(Xml.FILTER).size() != 0)
			throw new UnsupportedOperationException("Unsupported option " + Xml.FILTER + " for " + GET_CONFIG);

		return sourceDatastore;

	}

	private Element getResponseInternal(final Document document, final ConfigRegistryClient configRegistryClient, final Datastore source)
			throws NetconfDocumentedException {
		Element dataElement = document.createElement(Xml.DATA_KEY);
		final Set<ObjectName> instances = Datastore.getInstanceQueryStrategy(source, this.transactionProvider).queryInstances(
				configRegistryClient);
		final Config configMapping = new Config(transform(configRegistryClient, yangStoreSnapshot.getModuleMXBeanEntryMap()));
		dataElement = configMapping.toXml(instances, this.maybeNamespace, document, dataElement);

		logger.info("{} operation successful", GET_CONFIG);

		return dataElement;
	}

	// TODO refactor ... duplicate code
	private Map<String, Map<String, ModuleConfig>> transform(final ConfigRegistryClient configRegistryClient,
			Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries) {
		return Maps.transformEntries(mBeanEntries,
				new Maps.EntryTransformer<String, Map<String, ModuleMXBeanEntry>, Map<String, ModuleConfig>>() {

					@Override
					public Map<String, ModuleConfig> transformEntry(String arg0, Map<String, ModuleMXBeanEntry> arg1) {
						return Maps.transformEntries(arg1, new Maps.EntryTransformer<String, ModuleMXBeanEntry, ModuleConfig>() {

							@Override
							public ModuleConfig transformEntry(String key, ModuleMXBeanEntry value) {
								return new ModuleConfig(key, new InstanceConfig(configRegistryClient, value.getAttributes()), value.getProvidedServices().values());
							}
						});
					}
				});
	}

	@Override
	protected String getOperationName() {
		return GET_CONFIG;
	}

	@Override
	public Element handle(Document document, XmlElement xml) throws NetconfDocumentedException {
		Datastore source;
		try {
			source = fromXml(xml);
		} catch (final IllegalArgumentException e) {
			logger.warn("Rpc error: {}", ErrorTag.bad_attribute, e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(ErrorTag.bad_attribute.name(), e.getMessage());
			throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.rpc, ErrorTag.bad_attribute, ErrorSeverity.error, errorInfo);
		} catch (final IllegalStateException e) {
			logger.warn("Rpc error: {}", ErrorTag.missing_attribute, e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(ErrorTag.missing_attribute.name(), "Missing datasource attribute value");
			throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.rpc, ErrorTag.missing_attribute, ErrorSeverity.error, errorInfo);
		} catch (final UnsupportedOperationException e) {
			logger.warn("Unsupported", e);
			final Map<String, String> errorInfo = new HashMap<>();
			errorInfo.put(ErrorTag.operation_not_supported.name(), "Unsupported option for get");
			throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_not_supported, ErrorSeverity.error, errorInfo);
		}
		return getResponseInternal(document, configRegistryClient, source);
	}
}
