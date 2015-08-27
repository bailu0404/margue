/*
 * BufferSizeListener.java
 *
 * Created on 15. April 2007, 11:44
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
 * An interface for classes that are interested in the fill level of a
 * ByteBuffer.
 * This is needed especially for flow control mechanisms that must protect some
 * buffers against overflows by stopping data traffic and re-enable data traffic
 * when the buffer was successfully flushed.
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public interface BufferSizeListener {

    /**
     * gets called if the buffer fill level changed
     * @param source the source of the event
     * @param newLevel the new fill level of the buffer
     */
    void bufferSizeChanged(Object source, int newLevel);
}
