/*
 * Acceptor.java
 *
 * Created on 30. September 2006, 17:35
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Thread that accepts new connections at a given port and registers the
 * connection at the Dispatcher with the appropriate handler.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public abstract class AbstractAcceptor extends Thread {

    /**
     * the central NIO framework dispatcher
     */
    protected final Dispatcher dispatcher;
    private final ServerSocketChannel serverSocketChannel;
    private static final Logger logger =
            Logger.getLogger(AbstractAcceptor.class.getName());

    /**
     * creates a new Acceptor
     *
     * @param socketAddress the socketAddress where to accept new connections
     * @param dispatcher the central dispatcher
     * @throws java.io.IOException if the Acceptor could not bind to the given
     * port
     */
    public AbstractAcceptor(Dispatcher dispatcher, SocketAddress socketAddress)
            throws IOException {
        super(AbstractAcceptor.class.getName());
        this.dispatcher = dispatcher;

        serverSocketChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(socketAddress);
    }

    @Override
    public void run() {
        try {
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "new connection accepted");
                }
                socketChannel.configureBlocking(false);
                ChannelHandler handler = getHandler(socketChannel);
                dispatcher.registerChannel(socketChannel, handler);
            }
        } catch (ClosedChannelException ex) {
            FrameworkTools.handleStackTrace(logger, ex);
        } catch (IOException ex) {
            FrameworkTools.handleStackTrace(logger, ex);
        }
    }

    /**
     * stops the acceptor
     */
    public void stopAcceptor() {
        interrupt();
    }

    /**
     * Returns the appropriate ChannelHandler for the given SocketChannel.
     * @param socketChannel the SocketChannel of a new connection
     * @return the appropriate ChannelHandler for the given SocketChannel
     */
    protected abstract ChannelHandler getHandler(SocketChannel socketChannel);
}
