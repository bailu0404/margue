/*
 * BufferForwarder.java
 *
 * Created on 10.07.2008, 15:24:41
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
 * A forwarder that buffers data and can forward chunks of data with a given
 * or a maximum size. This way it can be used as a component for traffic
 * shaping.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class BufferForwarder
        extends AbstractForwarder<ByteBuffer[], ByteBuffer>
        implements TrafficShaper {

    private final static Logger logger =
            Logger.getLogger(BufferForwarder.class.getName());
    private ByteBuffer buffer = ByteBuffer.allocate(0);
    private ByteBufferForwardingMode byteBufferForwardingMode;

    /**
     * creates a new BufferForwarder
     * @param byteBufferForwardingMode the ByteBufferForwardingMode to use
     */
    public BufferForwarder(ByteBufferForwardingMode byteBufferForwardingMode) {
        this.byteBufferForwardingMode = byteBufferForwardingMode;
    }

    @Override
    public synchronized void forward(ByteBuffer[] inputs) throws IOException {
        if (logger.isLoggable(Level.FINEST)) {
            for (ByteBuffer input : inputs) {
                logger.log(Level.FINEST, "buffering " + input);
            }
        }
        buffer = FrameworkTools.append(false, buffer, inputs);
    }

    @Override
    public void sendPackage(long maxPackageSize) throws IOException {
        long size = Math.min(maxPackageSize, buffer.remaining());
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "forwarding " + size + " byte");
        }
        forward(size);
    }

    @Override
    public void setActive(boolean active) {
        // nothing to do here...
    }

    /**
     * Tells whether or not the forwarder has remaining buffered data
     * @return  <tt>true</tt> if the forwarder has remaining buffered data,
     * <tt>false</tt> otherwise
     */
    public synchronized boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    /**
     * returns the number of buffered bytes
     * @return the number of buffered bytes
     */
    public synchronized int remaining() {
        return buffer.remaining();
    }

    /**
     * forwards a chunk of the buffered data to the next forwarder
     * @param size the size of the chunk to forward to the next forwarder
     * @throws IOException if there is an I/O exception
     */
    public synchronized void forward(long size) throws IOException {
        // initial checks
        if (nextForwarder == null) {
            throw new IOException("nextForwarder is not defined");
        }
        if (size < 0) {
            throw new IllegalArgumentException("can not forward negative " +
                    "amounts of data (requested amount was " + size + " byte)");
        }
        int remaining = buffer.remaining();
        if (size > remaining) {
            throw new IOException("can not forward " + size + " bytes, only " +
                    remaining + " bytes buffered");
        }
        if (size > Integer.MAX_VALUE) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "size > Integer.MAX_VALUE (" +
                        Integer.MAX_VALUE + "), several packages have to be " +
                        "forwarded");
            }
            for (long tmpRemaining = size; tmpRemaining > 0;) {
                int tmpSize = (int) Math.min(tmpRemaining, Integer.MAX_VALUE);
                byteBufferForwardingMode.forwardBufferHead(
                        buffer, tmpSize, nextForwarder);
                tmpRemaining -= tmpSize;
            }
        } else {
            byteBufferForwardingMode.forwardBufferHead(
                    buffer, (int) size, nextForwarder);
        }
    }

    /**
     * returns the ByteBufferForwardingMode
     * @return the ByteBufferForwardingMode
     */
    public ByteBufferForwardingMode getByteBufferForwardingMode() {
        return byteBufferForwardingMode;
    }

    /**
     * sets the ByteBufferForwardingMode
     * @param byteBufferForwardingMode the ByteBufferForwardingMode to set
     */
    public void setByteBufferForwardingMode(
            ByteBufferForwardingMode byteBufferForwardingMode) {
        this.byteBufferForwardingMode = byteBufferForwardingMode;
    }
}
