/*
 * ByteBufferForwardingMode.java
 *
 * Created on 01.02.2009, 21:35:37
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

package ch.unifr.nio.framework.transform;

import ch.unifr.nio.framework.FrameworkTools;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An enum of all known ByteBuffer forwarding modes .
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public enum ByteBufferForwardingMode {

    /**
     * ByteBuffers are forwarded directly.
     */
    DIRECT,
    /**
     * Duplicates of ByteBuffers are forwarded. This is reasonable in situations
     * where the content of a ByteBuffer can be shared between many instants but
     * every instant needs a different position and limit.
     */
    DUPLICATE,
    /**
     * Copies of ByteBuffers are forwarded.
     */
    COPY;
    private static final Logger logger =
            Logger.getLogger(ByteBufferForwardingMode.class.getName());

    /**
     * returns a reference to the byteBuffer according to the strategy
     * @param byteBuffer the ByteBuffer which reference has to be returned
     * according to the defined strategy
     * @return a reference to the byteBuffer according to the strategy
     */
    public ByteBuffer getByteBuffer(ByteBuffer byteBuffer) {
        switch (this) {
            case COPY:
                return FrameworkTools.copyBuffer(byteBuffer);
            case DIRECT:
                return byteBuffer;
            case DUPLICATE:
                return byteBuffer.duplicate();
            default:
                logger.log(Level.SEVERE,
                        "unknown ByteBufferForwardingMode \"" + this + "\"");
                return null;
        }
    }

    /**
     * forwards the head of a ByteBuffer
     * @param byteBuffer the ByteBuffer to forward
     * @param size the size of the head
     * @param forwarder the forwarder to use
     * @throws IOException if an I/O exception occurs while forwarding the head
     * of <tt>byteBuffer</tt>
     */
    public void forwardBufferHead(ByteBuffer byteBuffer, int size,
            AbstractForwarder<ByteBuffer, ?> forwarder) throws IOException {

        switch (this) {
            case COPY:
                forwarder.forward(FrameworkTools.copyBuffer(byteBuffer, size));
                break;

            case DIRECT:
                int oldLimit = byteBuffer.limit();
                int newLimit = byteBuffer.position() + size;
                byteBuffer.limit(newLimit);
                forwarder.forward(byteBuffer);
                // reset buffer
                if (byteBuffer.position() != newLimit) {
                    /**
                     * position() is much simpler than position(int newPosition)
                     * because correcting the position is only very seldomly
                     * necessary, we put the check here in front (only happens
                     * if next forwarders do not consume the message completely)
                     */
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, "buffer was not completely " +
                                "consumed by nextForwarder: " + byteBuffer);
                    }
                    byteBuffer.position(newLimit);
                }
                byteBuffer.limit(oldLimit);
                break;

            case DUPLICATE:
                forwarder.forward(FrameworkTools.splitBuffer(byteBuffer, size));
                break;

            default:
                logger.log(Level.SEVERE,
                        "unknown ByteBufferForwardingMode \"" + this + "\"");
        }
    }
}