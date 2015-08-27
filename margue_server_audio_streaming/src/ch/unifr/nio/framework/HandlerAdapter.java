/*
 * HandlerAdapter.java
 *
 * Created on 30. September 2006, 12:59
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

import ch.unifr.nio.framework.transform.ChannelReader;
import ch.unifr.nio.framework.transform.ChannelWriter;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An adapter class that shields conctrete channel handlers from the dirty
 * details of NIO (dispatcher, selection keys, ...)
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class HandlerAdapter extends Thread {

    // framework references
    private final Dispatcher dispatcher;
    private final ChannelHandler channelHandler;
    private final ChannelReader channelReader;
    private final ChannelWriter channelWriter;
    // NIO
    private final SelectionKey key;
    // @GuardedBy("this")
    private int cachedInterestOps;
    // @GuardedBy("this")
    private int cachedReadyOps;
    // @GuardedBy("this")
    private boolean opsCached;
    private final static Logger logger =
            Logger.getLogger(HandlerAdapter.class.getName());
    private final String debugName;
    // debugging stuff
//    private long runCounter;
//    private long readableCounter;
//    private long writableCounter;

    /**
     * Creates a new instance of HandlerAdapter
     * @param debugName a descriptive name for debugging purposes
     * @param key the selection key of the channel this handler must deal with
     * @param dispatcher the central dispatcher
     * @param channelHandler the concrete channel handler
     */
    public HandlerAdapter(Dispatcher dispatcher, ChannelHandler channelHandler,
            SelectionKey key, String debugName) {

        super("HandlerAdapter");

        // keep some references
        this.dispatcher = dispatcher;
        this.channelHandler = channelHandler;
        this.key = key;
        this.debugName = debugName;

        channelReader = channelHandler.getChannelReader();
        channelWriter = channelHandler.getChannelWriter();

        // !!! the HandlerAdapter needs to cache the initial interest ops or
        // otherwise removeInterestOps() does not work correctly before the
        // first call to cacheOps() !!!
        cachedInterestOps = key.interestOps();
    }

    @Override
    public void run() {
//        runCounter++;
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, debugName + " output handling");
        }
        try {
            // support for ClientSocketChannelHandler
            if ((cachedReadyOps & SelectionKey.OP_CONNECT) != 0) {
                SelectableChannel channel = key.channel();
                if (!(channel instanceof SocketChannel)) {
                    throw new IllegalStateException("SelectionKey is " +
                            "connectable but channel is no SocketChannel!");
                }
                if (!(channelHandler instanceof ClientSocketChannelHandler)) {
                    throw new IllegalStateException(
                            "SelectionKey is connectable but handler is no " +
                            "ClientSocketChannelHandler!");
                }
                SocketChannel socketChannel = (SocketChannel) channel;

                // The TimeoutHandler may call socketChannel.close()
                // at the same time in another thread.
                // We synchronize both threads on the intrinsic lock of the
                // socketChannel.
                synchronized (socketChannel) {
                    if (socketChannel.isOpen()) {
                        ClientSocketChannelHandler clientSocketChannelHandler =
                                (ClientSocketChannelHandler) channelHandler;
                        try {
                            socketChannel.finishConnect();
                            clientSocketChannelHandler.connectSucceeded();
                            cachedInterestOps = SelectionKey.OP_READ;
                        } catch (IOException ex) {
                            cachedReadyOps = 0;
                            key.cancel();
                            clientSocketChannelHandler.connectFailed(ex);
                        }
                    }
                }
            }

            // only relevant for previously incomplete write operations
            if ((cachedReadyOps & SelectionKey.OP_WRITE) != 0) {
//                writableCounter++;
                if (channelWriter.drain()) {
                    // we wrote the complete buffered output data and can now
                    // turn off write selection (we are no longer interested in
                    // OP_WRITE)
                    removeInterestOps(SelectionKey.OP_WRITE);
                }
            }

            // input handling
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, debugName + " input handling");
            }
            if ((cachedReadyOps & SelectionKey.OP_READ) != 0) {
//                readableCounter++;
                channelReader.read();
                if (channelReader.isClosed()) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, debugName +
                                " input closed -> removing read interest");
                    }
                    // For some reason it is no longer possible to receive data
                    // from channelReader. Turn off read selection (we are no
                    // longer interested in OP_READ).
                    removeInterestOps(SelectionKey.OP_READ);
                    // the concrete channel handler must be notified
                    channelHandler.inputClosed();
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ex) {
//            FrameworkTools.handleStackTrace(logger, ex);
            try {
                closeChannel();
            } catch (IOException ex2) {
                FrameworkTools.handleStackTrace(logger, ex2);
            }
            channelHandler.channelException(ex);

        } finally {
            synchronized (this) {
                // The calls to channelHandler above may have called back
                // this.closeChannel().
                // In this case "key" is ivalid and we don't need to resume
                // selection.
                if (key.isValid()) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, debugName +
                                " resuming selection with " +
                                interestToString(cachedInterestOps));
                    }
                    dispatcher.setInterestOps(key, cachedInterestOps);
                }
                opsCached = false;
            }
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, debugName + " done");
        }
    }

    /**
     * The dispatcher calls this method to prevent further channel selection.
     * Therefore we must cache the current interestOps so that we can restore
     * them at the end of run().
     * @throws java.nio.channels.CancelledKeyException  if the key was cancelled
     */
    public synchronized void cacheOps() throws CancelledKeyException {
        cachedInterestOps = key.interestOps();
        cachedReadyOps = key.readyOps();
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, debugName + " starting with " +
                    interestToString(cachedInterestOps));
        }
        // remove all interestOps to stop further selection on channel
        // (will be restored at the end of run())
        key.interestOps(0);
        opsCached = true;
    }

    /**
     * takes caching into account when removing interest ops from the channel
     * @param interestOps the interest ops to remove
     */
    public synchronized void removeInterestOps(int interestOps) {
        // check, if interestOps are there at all
        if ((cachedInterestOps & interestOps) == 0) {
            // none of interestOps are set, so nothing can be removed!?
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, debugName + ": " +
                        interestToString(interestOps) + " not set");
            }
            return;
        }

        // update cache
        cachedInterestOps &= ~interestOps;
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, debugName + ": cachedInterestOps set to " +
                    interestToString(cachedInterestOps));
        }

        if (!opsCached) {
            dispatcher.removeInterestOps(key, interestOps);
        }
    }

    /**
     * takes caching into account when adding interest ops to the channel
     * @param interestOps the interest ops to add
     */
    public synchronized void addInterestOps(int interestOps) {
        // we _always_ cache the interest ops within the handler so that we
        // can return early if there are no changes needed...
        // the member opsCached is true when the HandlerAdapter is
        // running and the channel should not be registered again at the
        // dispatcher in any case!
        // check, if interestOps are already there
        if ((cachedInterestOps & interestOps) == interestOps) {
            // interestOps are already there...
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, debugName + ": " +
                        interestToString(interestOps) + " was already there");
            }
            return;
        }

        // update cache
        cachedInterestOps |= interestOps;
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, debugName + ": cachedInterestOps set to " +
                    interestToString(cachedInterestOps));
        }

        if (!opsCached) {
            // forward change to "real" interestOps if not cached
            dispatcher.setInterestOps(key, cachedInterestOps);
        }
    }

    /**
     * closes the channel
     * @throws java.io.IOException if closing the channel fails
     */
    public void closeChannel() throws IOException {
        cachedReadyOps = 0;
        dispatcher.closeChannel(key);
    }

    /**
     * returns the Channel we are dealing with
     * (This somehow breaks encapsulation of all the ugly details but there
     * seems to be no simpler way to provide access to all features of the
     * Socket class...)
     * @return the Channel we are dealing with
     */
    public synchronized Channel getChannel() {
        return key.channel();
    }
    
    /**
     * returns a String representation of an interest set
     * @param interest an interest set
     * @return a String representation of an interest set
     */
    public static final String interestToString(int interest) {
        StringBuilder stringBuilder = new StringBuilder();
        if ((interest & SelectionKey.OP_ACCEPT) != 0) {
            stringBuilder.append("OP_ACCEPT ");
        }
        if ((interest & SelectionKey.OP_CONNECT) != 0) {
            stringBuilder.append("OP_CONNECT ");
        }
        if ((interest & SelectionKey.OP_READ) != 0) {
            stringBuilder.append("OP_READ ");
        }
        if ((interest & SelectionKey.OP_WRITE) != 0) {
            stringBuilder.append("OP_WRITE ");
        }
        if (stringBuilder.length() == 0) {
            stringBuilder.append("NO INTEREST");
        }
        return stringBuilder.toString();
    }
    // debugging stuff
//    public long getReadableCounter() {
//        return readableCounter;
//    }
//
//    public long getWritableCounter() {
//        return writableCounter;
//    }
//
//    public long getRunCounter() {
//        return runCounter;
//    }
}
