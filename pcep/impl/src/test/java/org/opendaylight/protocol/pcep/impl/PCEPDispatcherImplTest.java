/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

public class PCEPDispatcherImplTest {
/*
    private static final int PORT = 4189;
    private static final InetSocketAddress CLIENT1_ADDRESS = new InetSocketAddress("127.0.0.10", PORT);
    private static final InetSocketAddress CLIENT2_ADDRESS = new InetSocketAddress("127.0.0.11", PORT);
    private static final short DEAD_TIMER = 120;
    private static final short KEEP_ALIVE = 30;

    private PCEPDispatcherImpl dispatcher;
    private PCEPDispatcherImpl dispatcher2;
    private PCEPDispatcherImpl disp2Spy;
    private MD5NioServerSocketChannelFactory scf;
    private MD5NioSocketChannelFactory cf;

    @Mock
    private KeyAccessFactory kaf;
    @Mock
    private Channel mockChannel;
    @Mock
    private KeyAccess mockKeyAccess;

    private PCCMock pccMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Open open = new OpenBuilder().setSessionId((short) 0).setDeadTimer(DEAD_TIMER).setKeepalive(KEEP_ALIVE)
            .build();
        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        final MessageRegistry msgReg = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
            .getMessageHandlerRegistry();
        this.dispatcher = new PCEPDispatcherImpl(msgReg, new DefaultPCEPSessionNegotiatorFactory(open, 0),
            eventLoopGroup, eventLoopGroup);

        Mockito.doReturn("mockChannel").when(this.mockChannel).toString();
        Mockito.doReturn(this.mockKeyAccess).when(this.kaf).getKeyAccess(Mockito.any(Channel.class));
        this.scf = new MD5NioServerSocketChannelFactory(this.kaf);
        this.cf = new MD5NioSocketChannelFactory(this.kaf);
        this.dispatcher2 = new PCEPDispatcherImpl(msgReg, new DefaultPCEPSessionNegotiatorFactory(open, 0), eventLoopGroup, eventLoopGroup, this.cf, this.scf);
        this.disp2Spy = Mockito.spy(this.dispatcher2);

        this.pccMock = new PCCMock(new DefaultPCEPSessionNegotiatorFactory(open, 0),
            new PCEPHandlerFactory(msgReg), new DefaultPromise<PCEPSessionImpl>(
            GlobalEventExecutor.INSTANCE));
    }

    @Test
    public void testCreateClientServer() throws InterruptedException, ExecutionException {
        final ChannelFuture futureChannel = this.dispatcher.createServer(new InetSocketAddress("0.0.0.0", PORT),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            });
        final PCEPSessionImpl session1 = this.pccMock.createClient(CLIENT1_ADDRESS,
            new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            }).get();

        final PCEPSessionImpl session2 = this.pccMock.createClient(CLIENT2_ADDRESS,
            new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            }).get();

        Assert.assertTrue(futureChannel.channel().isActive());
        Assert.assertEquals(CLIENT1_ADDRESS.getAddress().getHostAddress(), session1.getPeerPref().getIpAddress());
        Assert.assertEquals(DEAD_TIMER, session1.getDeadTimerValue().shortValue());
        Assert.assertEquals(KEEP_ALIVE, session1.getKeepAliveTimerValue().shortValue());

        Assert.assertEquals(CLIENT2_ADDRESS.getAddress().getHostAddress(), session2.getPeerPref().getIpAddress());
        Assert.assertEquals(DEAD_TIMER, session2.getDeadTimerValue().shortValue());
        Assert.assertEquals(KEEP_ALIVE, session2.getKeepAliveTimerValue().shortValue());

        session1.close();
        session2.close();
        Assert.assertTrue(futureChannel.channel().isActive());
    }

    @Test
    public void testCreateDuplicateClient() throws InterruptedException, ExecutionException {
        this.dispatcher.createServer(new InetSocketAddress("0.0.0.0", PORT),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            });
        final PCEPSessionImpl session1 = this.pccMock.createClient(CLIENT1_ADDRESS,
            new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            }).get();

        try {
            this.pccMock.createClient(CLIENT1_ADDRESS,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
                new PCEPSessionListenerFactory() {
                    @Override
                    public PCEPSessionListener getSessionListener() {
                        return new SimpleSessionListener();
                    }
                }).get();
            Assert.fail();
        } catch (final ExecutionException e) {
            Assert.assertTrue(e.getMessage().contains("A conflicting session for address"));
        } finally {
            session1.close();
        }
    }

    @Test
    public void testReconectClient() throws InterruptedException, ExecutionException {
        this.dispatcher.createServer(new InetSocketAddress("0.0.0.0", PORT),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            });
        final PCEPSessionImpl session1 = this.pccMock.createClient(CLIENT1_ADDRESS,
            new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            }).get();

        Assert.assertEquals(CLIENT1_ADDRESS.getAddress(), session1.getRemoteAddress());
        Assert.assertEquals(DEAD_TIMER, session1.getDeadTimerValue().shortValue());
        Assert.assertEquals(KEEP_ALIVE, session1.getKeepAliveTimerValue().shortValue());
        session1.close();

        final PCEPSessionImpl session2 = this.pccMock.createClient(CLIENT1_ADDRESS,
            new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            }).get();

        Assert.assertEquals(CLIENT1_ADDRESS.getAddress(), session1.getRemoteAddress());
        Assert.assertEquals(DEAD_TIMER, session2.getDeadTimerValue().shortValue());
        Assert.assertEquals(KEEP_ALIVE, session2.getKeepAliveTimerValue().shortValue());

        session2.close();
    }

    @Test
    public void testCustomizeBootstrap() {
        final KeyMapping keys = new KeyMapping();
        keys.put(this.CLIENT1_ADDRESS.getAddress(), new String("CLIENT1_ADDRESS").getBytes());
        keys.put(this.CLIENT2_ADDRESS.getAddress(), new String("CLIENT2_ADDRESS").getBytes());

        final ChannelFuture futureChannel = this.disp2Spy.createServer(new InetSocketAddress("0.0.0.0", PORT), keys,
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    return new SimpleSessionListener();
                }
            });
        Mockito.verify(this.disp2Spy).customizeBootstrap(Mockito.any(ServerBootstrap.class));
    }

    @After
    public void tearDown() {
        this.dispatcher.close();
    }

    private static class PCCMock extends PCEPAbstractDispatcher {

        private final PCEPSessionNegotiatorFactory negotiatorFactory;
        private final PCEPHandlerFactory factory;

        public PCCMock(final PCEPSessionNegotiatorFactory negotiatorFactory, final PCEPHandlerFactory factory,
                       final DefaultPromise<PCEPSessionImpl> defaultPromise) {
            super(GlobalEventExecutor.INSTANCE, new NioEventLoopGroup(), new NioEventLoopGroup());
            this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
            this.factory = Preconditions.checkNotNull(factory);
        }

        public Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final ReconnectStrategy strategy,
                                                    final PCEPSessionListenerFactory listenerFactory) {
            return super.createClient(address, strategy, new PCEPAbstractDispatcher.PipelineInitializer() {
                @Override
                public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                    ch.pipeline().addLast(PCCMock.this.factory.getDecoders());
                    ch.pipeline().addLast("negotiator",
                        PCCMock.this.negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
                    ch.pipeline().addLast(PCCMock.this.factory.getEncoders());
                }
            });
        }
    }
*/
}