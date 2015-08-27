/*
 * AbstractClientSocketChannelHandler.java
 *
 * Created on 30.03.2008, 18:05:37
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

/**
 * An abstract ChannelHandler for client socket connections.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public abstract class AbstractClientSocketChannelHandler
        extends AbstractChannelHandler implements ClientSocketChannelHandler {
    
    /**
     * creates a new AbstractClientSocketChannelHandler
     */
    public AbstractClientSocketChannelHandler() {
    }
    
    /**
     * creates a new AbstractClientSocketChannelHandler
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
    public AbstractClientSocketChannelHandler(boolean directReading,
            int initialReadingCapacity, int maxReadingCapacity,
            boolean directWriting) {
        super(directReading, initialReadingCapacity, maxReadingCapacity,
                directWriting);
    }
}
