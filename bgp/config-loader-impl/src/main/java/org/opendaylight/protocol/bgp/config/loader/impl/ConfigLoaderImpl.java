/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.config.loader.impl;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.concurrent.GuardedBy;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigLoader;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigLoaderImpl implements ConfigLoader, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoaderImpl.class);
    private static final String INTERRUPTED = "InterruptedException";
    private static final String EXTENSION = "-.*\\.xml";
    private static final String INITIAL = "^";
    @GuardedBy("this")
    private final Map<String, ConfigFileProcessor> configServices = new HashMap<>();
    private final SchemaContext schemaContext;
    private final Thread watcherThread;
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final String path;
    private final Function<String, Void> handleNewFile = filename -> {
        handleEvent(filename);
        return null;
    };

    public ConfigLoaderImpl(final SchemaContext schemaContext, final BindingNormalizedNodeSerializer bindingSerializer,
        final FileWatcher fileWatcher) {
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
        this.bindingSerializer = Preconditions.checkNotNull(bindingSerializer);
        this.path = Preconditions.checkNotNull(fileWatcher).getPathFile();
        this.watcherThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    fileWatcher.handleEvents(this.handleNewFile);
                }
            } catch (final Exception e) {
                LOG.warn(INTERRUPTED, e);
            }
        });
        this.watcherThread.start();
        LOG.info("Config Loader service initiated");
    }

    private void handleConfigFile(final ConfigFileProcessor config, final String filename) {
        final NormalizedNode<?, ?> dto;
        try {
            dto = parseDefaultConfigFile(config, filename);
        } catch (final Exception e) {
            LOG.warn("Failed to parse config file {}", filename, e);
            return;
        }
        LOG.info("Loading initial config {}", filename);
        config.loadConfiguration(dto);
    }

    private NormalizedNode<?, ?> parseDefaultConfigFile(final ConfigFileProcessor config, final String filename) throws Exception {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

        final InputStream resourceAsStream = new FileInputStream(new File(this.path, filename));
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLStreamReader reader = factory.createXMLStreamReader(resourceAsStream);

        final SchemaNode schemaNode = SchemaContextUtil.findDataSchemaNode(this.schemaContext, config.getSchemaPath());
        final XmlParserStream xmlParser = XmlParserStream.create(streamWriter, this.schemaContext, schemaNode);
        xmlParser.parse(reader);

        return result.getResult();
    }

    @Override
    public synchronized AbstractRegistration registerConfigFile(final ConfigFileProcessor config) {
        final String pattern = INITIAL + config.getSchemaPath().getLastComponent().getLocalName() + EXTENSION;
        this.configServices.put(pattern, config);

        final File[] fList = new File(this.path).listFiles();
        if (fList != null) {
            for (final File file : fList) {
                if (file.isFile()) {
                    final String filename = file.getName();
                    if (Pattern.matches(pattern, filename)) {
                        handleConfigFile(config, filename);
                    }
                }
            }
        }

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (ConfigLoaderImpl.this) {
                    ConfigLoaderImpl.this.configServices.remove(pattern);
                }
            }
        };
    }

    @Override
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return this.bindingSerializer;
    }

    private synchronized void handleEvent(final String filename) {
        this.configServices.entrySet().stream().filter(entry -> Pattern.matches(entry.getKey(), filename)).
            forEach(entry -> handleConfigFile(entry.getValue(), filename));
    }

    @Override
    public void close() throws Exception {
        this.watcherThread.interrupt();
    }
}
