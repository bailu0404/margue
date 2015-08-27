/*
 * FramingOutputForwarder.java
 *
 * Created on 28.06.2008, 14:42:56
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Forwarder that frames messages by creating a length header
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class FramingOutputForwarder
        extends AbstractForwarder<ByteBuffer[], ByteBuffer[]> {

    private final int HEADER_SIZE;
    private final int MAX_SIZE;
    private final ByteBuffer header;
    private final static Logger logger =
            Logger.getLogger(FramingOutputForwarder.class.getName());
    private final AtomicLong headerCounter = new AtomicLong();
    private ByteBufferForwardingMode headerByteBufferForwardingMode;

    /**
     * creates a new FramingOutputForwarder
     * @param headerSize the size of the length header (determines the maximum
     * package size = 2^(8*headerSize) - 1)
     * @param headerByteBufferForwardingMode the buffer forwarding mode for the
     * header ByteBuffer
     */
    public FramingOutputForwarder(int headerSize,
            ByteBufferForwardingMode headerByteBufferForwardingMode) {
        this.headerByteBufferForwardingMode = headerByteBufferForwardingMode;
        HEADER_SIZE = headerSize;
        header = ByteBuffer.allocate(HEADER_SIZE);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "\n" +
                    "\tHEADER_SIZE = " + HEADER_SIZE + '\n' +
                    "\theader: " + header);
        }
        MAX_SIZE = (int) Math.pow(2, 8 * HEADER_SIZE) - 1;
    }

    @Override
    public synchronized void forward(ByteBuffer[] inputs) throws IOException {
        if (nextForwarder == null) {
            logger.log(Level.SEVERE, "no nextForwarder, data lost!");

        } else {
            int remaining = 0;
            for (ByteBuffer input : inputs) {
                remaining += input.remaining();
            }

            // check input length
            if (remaining > MAX_SIZE) {
                throw new IOException("The input ByteBuffer is too large (" +
                        remaining + " byte). When using a header size of " +
                        HEADER_SIZE + " byte, the maximum message size is " +
                        MAX_SIZE + " byte.");
            }

            // fill length header
            header.clear();
            for (int i = 0, shift = (HEADER_SIZE - 1) * 8; i < HEADER_SIZE;
                    i++) {
                byte lengthByte = (byte) (remaining >> shift);
                header.put(lengthByte);
                shift -= 8;
            }
            header.flip();

            // create new array with additional header at index 0
            int length = inputs.length;
            ByteBuffer[] array = new ByteBuffer[1 + length];
            array[0] = headerByteBufferForwardingMode.getByteBuffer(header);
            System.arraycopy(inputs, 0, array, 1, length);
            if (logger.isLoggable(Level.FINEST)) {
                StringBuilder stringBuilder =
                        new StringBuilder("\tframed array:\n");
                for (ByteBuffer byteBuffer : array) {
                    stringBuilder.append("\t\t" + byteBuffer + '\n');
                }
                logger.log(Level.FINEST, stringBuilder.toString());
            }

            // account header & forward new array
            headerCounter.addAndGet(HEADER_SIZE);
            nextForwarder.forward(array);
        }
    }

    /**
     * returns how much header bytes have been generated
     * @return how much header bytes have been generated
     */
    public long getHeaderCounter() {
        return headerCounter.get();
    }

    /**
     * resets the header counter back to zero and returns how much header bytes
     * have been generated
     * @return how much header bytes have been generated
     */
    public long getAndResetHeaderCounter() {
        return headerCounter.getAndSet(0);
    }

    /**
     * returns the maximum size (in byte) a package can have using this
     * forwarder
     * @return the maximum size (in byte) a package can have using this
     * forwarder
     */
    public int getMaxSize() {
        return MAX_SIZE;
    }

    /**
     * returns the ByteBufferForwardingMode that is used for the header
     * @return the ByteBufferForwardingMode that is used for the header
     */
    public synchronized ByteBufferForwardingMode getHeaderForwardingMode() {
        return headerByteBufferForwardingMode;
    }

    /**
     * sets the ByteBufferForwardingMode that is used for the header
     * @param headerByteBufferForwardingMode the ByteBufferForwardingMode that
     * is used for the header
     */
    public synchronized void setHeaderForwardingMode(
            ByteBufferForwardingMode headerByteBufferForwardingMode) {
        this.headerByteBufferForwardingMode = headerByteBufferForwardingMode;
    }
}
