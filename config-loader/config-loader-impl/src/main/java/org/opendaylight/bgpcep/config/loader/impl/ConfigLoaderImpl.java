/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.impl;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.concurrent.GuardedBy;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
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
import org.xml.sax.SAXException;

public final class ConfigLoaderImpl implements ConfigLoader, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoaderImpl.class);
    private static final String INTERRUPTED = "InterruptedException";
    private static final String EXTENSION = "-.*\\.xml";
    private static final String INITIAL = "^";
    @GuardedBy("this")
    private final Map<String, ConfigFileProcessor> configServices = new HashMap<>();
    private final SchemaContext schemaContext;
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final String path;
    private final Thread watcherThread;
    @GuardedBy("this")
    private boolean closed = false;

    public ConfigLoaderImpl(final SchemaContext schemaContext, final BindingNormalizedNodeSerializer bindingSerializer,
            final FileWatcher fileWatcher) {
        this.schemaContext = requireNonNull(schemaContext);
        this.bindingSerializer = requireNonNull(bindingSerializer);
        this.path = requireNonNull(fileWatcher.getPathFile());
        this.watcherThread = new Thread(new ConfigLoaderImplRunnable(requireNonNull(fileWatcher.getWatchService())));
    }

    public void init() {
        this.watcherThread.start();
        LOG.info("Config Loader service initiated");
    }

    private void handleConfigFile(final ConfigFileProcessor config, final String filename) {
        final NormalizedNode<?, ?> dto;
        try {
            dto = parseDefaultConfigFile(config, filename);
        } catch (final IOException | XMLStreamException e) {
            LOG.warn("Failed to parse config file {}", filename, e);
            return;
        }
        LOG.info("Loading initial config {}", filename);
        config.loadConfiguration(dto);
    }

    private NormalizedNode<?, ?> parseDefaultConfigFile(final ConfigFileProcessor config, final String filename)
            throws IOException, XMLStreamException {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

        try (InputStream resourceAsStream = new FileInputStream(new File(this.path, filename))) {
            final XMLInputFactory factory = XMLInputFactory.newInstance();
            final XMLStreamReader reader = factory.createXMLStreamReader(resourceAsStream);

            final SchemaNode schemaNode = SchemaContextUtil
                    .findDataSchemaNode(this.schemaContext, config.getSchemaPath());
            try (XmlParserStream xmlParser = XmlParserStream.create(streamWriter, this.schemaContext, schemaNode)) {
                xmlParser.parse(reader);
            } catch (final URISyntaxException | XMLStreamException | IOException | ParserConfigurationException
                    | SAXException e) {
                LOG.warn("Failed to parse xml", e);
            } finally {
                reader.close();
            }
        }

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


    @Override
    public synchronized void close() throws Exception {
        LOG.info("Config Loader service closed");
        this.closed = true;
        this.watcherThread.interrupt();
    }

    private class ConfigLoaderImplRunnable implements Runnable {
        @GuardedBy("this")
        private final WatchService watchService;

        ConfigLoaderImplRunnable(final WatchService watchService) {
            this.watchService = watchService;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                handleChanges();
            }
        }

        private synchronized void handleChanges() {
            final WatchKey key;
            try {
                key = this.watchService.take();
            } catch (final InterruptedException | ClosedWatchServiceException e) {
                if (!ConfigLoaderImpl.this.closed) {
                    LOG.warn(INTERRUPTED, e);
                    Thread.currentThread().interrupt();
                }
                return;
            }

            if (key != null) {
                for (final WatchEvent<?> event : key.pollEvents()) {
                    handleEvent(event.context().toString());
                }
                final boolean reset = key.reset();
                if (!reset) {
                    LOG.warn("Could not reset the watch key.");
                }
            }
        }

        private synchronized void handleEvent(final String filename) {
            ConfigLoaderImpl.this.configServices.entrySet().stream()
                    .filter(entry -> Pattern.matches(entry.getKey(), filename))
                    .forEach(entry -> handleConfigFile(entry.getValue(), filename));
        }
    }
}
