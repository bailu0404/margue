/*
 * AbstractChannelHandler.java
 *
 * Created on 30. September 2006, 14:32
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
 * An abstract ChannelHandler with a ChannelReader, a ChannelWriter and a
 * HandlerAdapter
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public abstract class AbstractChannelHandler implements ChannelHandler {

    /**
     * the ChannelReader for this ChannelHandler
     */
    protected final ChannelReader reader;
    /**
     * the ChannelWriter for this ChannelHandler
     */
    protected final ChannelWriter writer;
    /**
     * the reference to the HandlerAdapter
     */
    protected HandlerAdapter handlerAdapter;

    /**
     * creates a new AbstractChannelHandler with the following properties:<br>
     * <ul>
     *    <li>
     *        channelReader: non-direct {@link ChannelReader} (initial
     * capacity = 1 kbyte, max capacity = 10 kbyte)
     *    </li>
     *    <li>
     *        channelWriter: non-direct {@link ChannelWriter}
     *    </li>
     *    <li>
     *        stores a reference to its {@link HandlerAdapter} for changing interest
     * ops
     *    </li>
     * </ul>
     */
    public AbstractChannelHandler() {
        reader = new ChannelReader(false/*direct*/,
                1024/*initial capacity*/, 10240/*max capacity*/);
        writer = new ChannelWriter(false/*direct*/);
    }

    /**
     * creates a new AbstractChannelHandler
     * @param directReading if <code>true</code>, a direct buffer is used for
     * reading, otherwise a non-direct buffer
     * @param initialReadingCapacity the initial capacity of the reading buffer
     * in byte
     * @param maxReadingCapacity the maximum capacity of the reading buffer in
     * byte
     * @param directWriting if <code>true</code>, a direct buffer is used for
     * buffering data from incomplete write operations, otherwise a non-direct
     * buffer
     */
    public AbstractChannelHandler(boolean directReading,
            int initialReadingCapacity, int maxReadingCapacity,
            boolean directWriting) {
        reader = new ChannelReader(directReading,
                initialReadingCapacity, maxReadingCapacity);
        writer = new ChannelWriter(directWriting);
    }

    @Override
    public void channelRegistered(HandlerAdapter handlerAdapter) {
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    public ChannelReader getChannelReader() {
        return reader;
    }

    @Override
    public ChannelWriter getChannelWriter() {
        return writer;
    }
}
