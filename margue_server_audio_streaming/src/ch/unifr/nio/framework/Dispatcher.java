/*
 * Dispatcher.java
 *
 * Created on 30. September 2006, 12:50
 *
 * This file is part of the NIO Framework.
 *
 * The NIO Framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * The NIO Framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.unifr.nio.framework;

import ch.unifr.nio.framework.transform.ChannelWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The central dispatcher thread in a reactor pattern that manages registered
 * ChannelHandlers.<br>
 * The Dispatcher demultiplexes all I/O requests originating from the registered
 * SelectableChannels and calls the ChannelHandlers, if necessary.<br>
 * ChannelHandlers execution characteristics can be fine-tuned by specifying an
 * Executor.<br>
 * Because of the locking mechanisms in the NIO library all changes to interest
 * ops
 * are done here.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class Dispatcher extends Thread {

    private final Selector selector;
    private final ScheduledExecutorService scheduledExecutorService;
    private final MyThreadFactory myThreadFactory;
    private Executor executor;
    private final static Logger logger =
            Logger.getLogger(Dispatcher.class.getName());
    // debugging stuff
//    private final int HISTORY = 1000000;
//    private long selectionCounter;
//    private long[] selectionTimeStamps = new long[HISTORY];
//    private long emptySelectionCounter;
//    private boolean[] selectionTypes = new boolean[HISTORY];
//    private long setInterestCounter;
//    private long removeInterestCounter;
//    private long getInterestCounter;
//    private long addInterestCounter;
//    private long closeChannelCounter;
//    private long registerChannelCounter;
    /**
     * Creates a new Dispatcher
     * @throws java.io.IOException when opening a Selector failes
     */
    public Dispatcher() throws IOException {
        super(Dispatcher.class.getName());
        setDaemon(true);
        selector = Selector.open();
        myThreadFactory = new MyThreadFactory();
        scheduledExecutorService =
                Executors.newScheduledThreadPool(1, myThreadFactory);
    }

    /**
     * sets the executor that runs the handlers
     * @param executor the executor that runs the handlers
     */
    public synchronized void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void run() {
        // synchronization does not hurt here, this is only run once during the
        // lifetime of the Dispatcher thread
        synchronized (this) {
            // create default executor if none was specified via setExecutor()
            if (executor == null) {
                executor = Executors.newCachedThreadPool(myThreadFactory);
            }
            notifyAll();
        }

        try {
            while (true) {
                synchronized (this) {
                    // guard
                }
                int updatedKeys = selector.select();

                // fill some debug info
//                if (selectionCounter < selectionTimeStamps.length) {
//                    selectionTimeStamps[(int) selectionCounter] = System.currentTimeMillis();
//                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "\n\t" + updatedKeys +
                            " keys updated\n\t" + selectedKeys.size() +
                            " keys in selector's selected key set");
                }
                if (!selectedKeys.isEmpty()) {
//                    if (selectionCounter < selectionTimeStamps.length) {
//                        selectionTypes[(int) selectionCounter] = true;
//                    }
                    for (SelectionKey key : selectedKeys) {
                        Object attachment = key.attachment();
                        if (attachment instanceof HandlerAdapter) {
                            // run adapter in executor
                            HandlerAdapter adapter =
                                    (HandlerAdapter) attachment;
                            try {
                                adapter.cacheOps();
                                executor.execute(adapter);
                            } catch (CancelledKeyException ckException) {
                                // This may happen if another thread cancelled
                                // the selection key after we returned from
                                // select()
                                FrameworkTools.handleStackTrace(
                                        logger, ckException);
                            }
                        } else {
                            if (logger.isLoggable(Level.WARNING)) {
                                logger.log(Level.WARNING,
                                        "attachment is no HandlerAdapter: " +
                                        attachment);
                            }
                        }
                    }
//                } else {
//                    if (selectionCounter < selectionTimeStamps.length) {
//                        selectionTypes[(int) selectionCounter] = false;
//                    }
//                    emptySelectionCounter++;
                }
                selectedKeys.clear();
//                selectionCounter++;
            }
        } catch (IOException ex) {
            // Ouch! Should never happen...
            FrameworkTools.handleStackTrace(logger, ex);
        }
    }

    /**
     * registers a channel at the dispatcher with {@link SelectionKey#OP_READ SelectionKey.OP_READ}
     * @param channel the channel to register
     * @param channelHandler an ChannelHandler for this channel
     * @throws java.nio.channels.ClosedChannelException if the channel to
     * register is already closed
     */
    public void registerChannel(
            SelectableChannel channel, ChannelHandler channelHandler)
            throws ClosedChannelException {
        registerChannel(channel, channelHandler, SelectionKey.OP_READ);
    }

    /**
     * registers a channel at the dispatcher
     * @param channel the channel to register
     * @param channelHandler an ChannelHandler for this channel
     * @param interestOps the interest ops to start with
     * @throws java.nio.channels.ClosedChannelException if the channel to 
     * register is already closed
     */
    public synchronized void registerChannel(SelectableChannel channel,
            ChannelHandler channelHandler, int interestOps)
            throws ClosedChannelException {
//        registerChannelCounter++;
        selector.wakeup();

        // register
        SelectionKey key = channel.register(selector, interestOps);
        HandlerAdapter handlerAdapter =
                new HandlerAdapter(this, channelHandler, key,
                channelHandler.getClass().getName());
        key.attach(handlerAdapter);

        // setup all required cross references
        channelHandler.getChannelReader().setChannel(channel);
        if (channel instanceof WritableByteChannel) {
            WritableByteChannel writableByteChannel =
                    (WritableByteChannel) channel;
            ChannelWriter channelWriter = channelHandler.getChannelWriter();
            channelWriter.setChannel(writableByteChannel);
            channelWriter.setHandlerAdapter(handlerAdapter);
        }

        // !!! call this only after all required cross references are set !!!
        channelHandler.channelRegistered(handlerAdapter);
    }

    /**
     * Registers the given clientSocketChannelHandler for a non-blocking socket
     * connection operation.<p/>
     * This includes:
     * <ul>
     *    <li>Resolve the given host name within the Dispatcher's Executor threadpool.</li>
     *    <li>Create a SocketChannel and initiate a non-blocking connect to the given host and port.</li>
     * </ul>
     * @param host the host name of the target system
     * @param port the port of the target system
     * @param clientSocketChannelHandler the clientSocketChannelHandler for this
     * socket connection
     */
    public synchronized void registerClientSocketChannelHandler(String host,
            int port, ClientSocketChannelHandler clientSocketChannelHandler) {
        registerClientSocketChannelHandler(
                host, port, clientSocketChannelHandler, 0);
    }

    /**
     * Registers the given clientSocketChannelHandler for a non-blocking socket
     * connection operation.<p/>
     * This includes:
     * <ul>
     *    <li>Resolve the given host name within the Dispatcher's Executor threadpool.</li>
     *    <li>Create a SocketChannel and initiate a non-blocking connect to the given host and port.</li>
     * </ul>
     * @param host the host name of the target system
     * @param port the port of the target system
     * @param clientSocketChannelHandler the clientSocketChannelHandler for this
     * socket connection
     * @param timeout The timeout for the connection operation given in
     * milliseconds.
     * If a connect operation does not succeed within the given timeout,
     * ClientSocketChannelHandler.connectFailed() is called.
     */
    public synchronized void registerClientSocketChannelHandler(String host,
            int port, ClientSocketChannelHandler clientSocketChannelHandler,
            int timeout) {
        while (executor == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                FrameworkTools.handleStackTrace(logger, ex);
            }
        }
        executor.execute(
                new Resolver(host, port, clientSocketChannelHandler, timeout));
    }

    /**
     * cancels <CODE>selectionKey</CODE>, removes its attachment and closes its
     * channel
     * @param selectionKey the selection key of the channel to unregister
     * @throws java.io.IOException if closing the channel fails
     */
    public synchronized void closeChannel(SelectionKey selectionKey)
            throws IOException {
//        closeChannelCounter++;
        selector.wakeup();
        selectionKey.cancel();
        selectionKey.attach(null);
        selectionKey.channel().close();
    }

    /**
     * sets the interest ops of a selection key
     * @param key the selection key of the channel
     * @param interestOps the interestOps to use when resuming the selection
     */
    public synchronized void setInterestOps(SelectionKey key, int interestOps) {
//        setInterestCounter++;
        selector.wakeup();
        if (key.isValid()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "set interestOps to " +
                        HandlerAdapter.interestToString(interestOps));
            }
            key.interestOps(interestOps);
        } else {
            logger.log(Level.WARNING, "key is invalid");
        }
    }

    /**
     * removes interest ops from a SelectionKey
     * @param key the SelectionKey
     * @param interestOps the interest ops to remove
     */
    public synchronized void removeInterestOps(
            SelectionKey key, int interestOps) {
//        removeInterestCounter++;
        selector.wakeup();
        if (key.isValid()) {
            int newOps = key.interestOps() & ~interestOps;
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "set interestOps to " +
                        HandlerAdapter.interestToString(newOps));
            }
            key.interestOps(newOps);
        } else {
            logger.log(Level.WARNING, "key is invalid");
        }
    }

    /**
     * returns the interest ops of a SelectionKey without blocking
     * @param key the selection key
     * @return the interest ops of the selection key
     */
    public synchronized int getInterestOps(SelectionKey key) {
//        getInterestCounter++;
        selector.wakeup();
        return key.interestOps();
    }

    /**
     * adds interest ops to a SelectionKey
     * @param key the SelectionKey
     * @param interestOps the new interestOps
     */
    public synchronized void addInterestOps(SelectionKey key, int interestOps) {
        logger.log(Level.FINEST, "waking up selector");
        selector.wakeup();
        if (key.isValid()) {
            int newOps = key.interestOps() | interestOps;
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "set interestOps to " +
                        HandlerAdapter.interestToString(newOps));
            }
            key.interestOps(newOps);
        } else {
            logger.log(Level.WARNING, "key is invalid");
        }
    }

    private class Resolver implements Runnable {

        private final String hostName;
        private final int port;
        private final ClientSocketChannelHandler clientSocketChannelHandler;
        private final int timeout;

        public Resolver(String hostName, int port,
                ClientSocketChannelHandler clientSocketChannelHandler,
                int timeout) {
            this.hostName = hostName;
            this.port = port;
            this.clientSocketChannelHandler = clientSocketChannelHandler;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            InetSocketAddress address = new InetSocketAddress(hostName, port);
            if (address.isUnresolved()) {
                clientSocketChannelHandler.resolveFailed();
            } else {
                try {
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    boolean connected = socketChannel.connect(address);
                    if (connected) {
                        registerChannel(socketChannel,
                                clientSocketChannelHandler,
                                SelectionKey.OP_READ);
                        clientSocketChannelHandler.connectSucceeded();
                    } else {
                        registerChannel(socketChannel,
                                clientSocketChannelHandler,
                                SelectionKey.OP_CONNECT);
                        if (timeout > 0) {
                            TimeoutHandler timeoutHandler =
                                    new TimeoutHandler(socketChannel,
                                    clientSocketChannelHandler);
                            scheduledExecutorService.schedule(timeoutHandler,
                                    timeout, TimeUnit.MILLISECONDS);
                        }
                    }
                } catch (IOException ex) {
                    FrameworkTools.handleStackTrace(logger, ex);
                    clientSocketChannelHandler.connectFailed(ex);
                }
            }
        }
    }

    private static class TimeoutHandler implements Runnable {

        // use weak references so that the objects can be garbage collected
        private final WeakReference<SocketChannel> socketChannelReference;
        private final WeakReference<ClientSocketChannelHandler> handlerReference;

        public TimeoutHandler(SocketChannel socketChannel,
                ClientSocketChannelHandler clientSocketChannelHandler) {
            socketChannelReference =
                    new WeakReference<SocketChannel>(socketChannel);
            handlerReference = new WeakReference<ClientSocketChannelHandler>(
                    clientSocketChannelHandler);
        }

        @Override
        public void run() {
            // The HandlerAdapter may call socketChannel.finishConnect()
            // at the same time in another thread.
            // We synchronize both threads on the intrinsic lock of the
            // socketChannel.
            SocketChannel socketChannel = socketChannelReference.get();
            ClientSocketChannelHandler handler = handlerReference.get();
            if ((socketChannel != null) && (handler != null)) {
                synchronized (socketChannel) {
                    if (socketChannel.isConnectionPending()) {
                        try {
                            socketChannel.close();
                            handler.connectFailed(
                                    new ConnectException("Connection timeout"));
                        } catch (IOException ex) {
                            FrameworkTools.handleStackTrace(logger, ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * By implementing our own ThreadFactory we solve two problems:
     * 1) our threads get a nice name prefix so that we find them when debugging
     * 2) our threads become daemon threads so that applications may gracefully
     *    exit when using the NIO Framework
     */
    static class MyThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        MyThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            group = securityManager == null
                    ? Thread.currentThread().getThreadGroup()
                    : securityManager.getThreadGroup();
            namePrefix = "NIO Framework-pool-" +
                    poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(group, runnable,
                    namePrefix + threadNumber.getAndIncrement());
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            thread.setDaemon(true);
            return thread;
        }
    }//    public long getSelectionCounter() {
//        long returnValue = selectionCounter;
//        selectionCounter = 0;
//        return returnValue;
//    }
//
//    public long getEmptySelectionCounter() {
//        long returnValue = emptySelectionCounter;
//        emptySelectionCounter = 0;
//        return returnValue;
//    }
//
//    public long getSetInterestCounter() {
//        long returnValue = setInterestCounter;
//        setInterestCounter = 0;
//        return returnValue;
//    }
//
//    public long getRemoveInterestCounter() {
//        long returnValue = removeInterestCounter;
//        removeInterestCounter = 0;
//        return returnValue;
//    }
//
//    public long getGetInterestCounter() {
//        long returnValue = getInterestCounter;
//        getInterestCounter = 0;
//        return returnValue;
//    }
//
//    public long getAddInterestCounter() {
//        long returnValue = addInterestCounter;
//        addInterestCounter = 0;
//        return returnValue;
//    }
//
//    public long getCloseChannelCounter() {
//        long returnValue = closeChannelCounter;
//        closeChannelCounter = 0;
//        return returnValue;
//    }
//
//    public long getRegisterChannelCounter() {
//        long returnValue = registerChannelCounter;
//        registerChannelCounter = 0;
//        return returnValue;
//    }
//
//    public long[] getSelectionTimeStamps() {
//        long[] returnValue = selectionTimeStamps;
//        selectionTimeStamps = new long[HISTORY];
//        return returnValue;
//    }
//
//    public boolean[] getSelectionTypes() {
//        boolean[] returnValue = selectionTypes;
//        selectionTypes = new boolean[HISTORY];
//        return returnValue;
//    }
}
