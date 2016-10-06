/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.config.loader.impl;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
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
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigLoaderImpl implements ConfigLoader, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoaderImpl.class);
    private static final String DEFAULT_APP_CONFIG_FILE_PATH = "etc" + File.separator + "opendaylight" + File.separator + "bgp";
    private static final String INTERRUPTED = "InterruptedException";
    private static final String EXTENSION = "-.*\\.xml";
    private static final String INITIAL = "^";
    private static final Path PATH = Paths.get(DEFAULT_APP_CONFIG_FILE_PATH);
    @GuardedBy("this")
    private final Map<String, ConfigFileProcessor> configServices = new HashMap<>();
    private final SchemaContext schemaContext;
    private final Thread watcherThread;
    private final BindingNormalizedNodeSerializer bindingSerializer;

    public ConfigLoaderImpl(final SchemaContext schemaContext, final BindingNormalizedNodeSerializer bindingSerializer) {
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
        this.bindingSerializer = Preconditions.checkNotNull(bindingSerializer);
        this.watcherThread = new Thread(new Watcher());
        this.watcherThread.start();
        LOG.info("Config Loader service initiated");
    }

    private class Watcher implements Runnable {
        @Override
        public void run() {
            WatchKey key;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    final WatchService watcher = FileSystems.getDefault().newWatchService();
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            try {
                                watcher.close();
                            } catch (final IOException e) {
                                LOG.warn(INTERRUPTED, e);
                            }
                        }
                    });

                    PATH.register(watcher, OVERFLOW, ENTRY_CREATE);
                    key = watcher.take();
                    key.pollEvents().forEach(event -> handleEvent(event.context().toString()));

                    final boolean reset = key.reset();
                    if (!reset) {
                        LOG.warn("Could not reset the watch key.");
                        break;
                    }
                }
            } catch (final Exception e) {
                LOG.warn(INTERRUPTED, e);
            }
        }
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

        final InputStream resourceAsStream = new FileInputStream(new File(DEFAULT_APP_CONFIG_FILE_PATH, filename));
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLStreamReader reader = factory.createXMLStreamReader(resourceAsStream);

        final XmlParserStream xmlParser = XmlParserStream.create(streamWriter, this.schemaContext,
            SchemaContextUtil.findDataSchemaNode(this.schemaContext, config.getSchemaPath()));
        xmlParser.parse(reader);

        return result.getResult();
    }

    public synchronized AbstractRegistration registerConfigFile(@Nonnull final ConfigFileProcessor config) {
        final String pattern = INITIAL + config.getSchemaPath().getLastComponent().getLocalName() + EXTENSION;
        configServices.put(pattern, config);

        final File[] fList = new File(DEFAULT_APP_CONFIG_FILE_PATH).listFiles();
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
        configServices.entrySet().stream().filter(entry -> Pattern.matches(entry.getKey(), filename)).
            forEach(entry -> handleConfigFile(entry.getValue(), filename));
    }

    @Override
    public void close() throws Exception {
        this.watcherThread.interrupt();
    }
}
