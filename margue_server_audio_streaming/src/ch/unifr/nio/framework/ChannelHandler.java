/*
 * ChannelHandler.java
 *
 * Created on 20. Februar 2007, 16:16
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

/**
 * An interface for a concrete channel handler
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public interface ChannelHandler {

    /**
     * Gets called as the last step of
     * {@link ch.unifr.nio.framework.Dispatcher#registerChannel Dispatcher.registerChannel()}
     * (useful for e.g. sending a welcome message)
     * @param handlerAdapter the HandlerAdapter to use for changing interest ops
     */
    void channelRegistered(HandlerAdapter handlerAdapter);

    /**
     * Returns the ChannelReader of this ChannelHandler
     * @return the ChannelReader of this ChannelHandler
     */
    ChannelReader getChannelReader();

    /**
     * Returns the ChannelWriter of this ChannelHandler
     * @return the ChannelWriter of this ChannelHandler
     */
    ChannelWriter getChannelWriter();

    /**
     * Gets called from the NIO Framework if no more input data is to be
     * expected from the corresponding SelectableChannel.<p/>
     * This may happen, e.g. when a SocketChannel has reached end-of-stream.
     */
    void inputClosed();

    /**
     * Gets called from the framework if an exception occurs while reading from
     * or writing to the corresponding SelectableChannel.
     * @param exception the exception that occured
     */
    void channelException(Exception exception);
}
