/*
 * FramingInputForwarder.java
 *
 * Created on 28.06.2008, 16:34:56
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Forwarder that unframes messages by removing a length header
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class FramingInputForwarder
        extends AbstractForwarder<ByteBuffer, ByteBuffer> {

    private final static Logger logger =
            Logger.getLogger(FramingInputForwarder.class.getName());
    private final int HEADER_SIZE;
    private ByteBuffer buffer;
    private boolean determineLength;
    private int currentLength;
    private final AtomicLong headerCounter = new AtomicLong();
    private final AtomicLong dataCounter = new AtomicLong();

    /**
     * creates a new FramingInputForwarder
     * @param headerSize the size of the length header
     */
    public FramingInputForwarder(int headerSize) {
        HEADER_SIZE = headerSize;
        determineLength = true;
    }

    @Override
    public synchronized void forward(ByteBuffer input) throws IOException {
        if (nextForwarder == null) {
            logger.log(Level.SEVERE, "no nextForwarder => data lost!");
            return;
        }

        if ((buffer != null) && (buffer.hasRemaining())) {
            logger.log(Level.FINEST, "appending input to buffer");
            buffer = FrameworkTools.append(false/*direct*/, buffer, input);
            unframe(buffer);
        } else {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "unframing the input buffer directly: " + input);
            }
            unframe(input);
            if (input.hasRemaining()) {
                buffer = FrameworkTools.append(false/*direct*/, buffer, input);
            }
        }
    }

    /**
     * returns how much header bytes have been filtered out
     * @return how much header bytes have been filtered out
     */
    public long getHeaderCounter() {
        return headerCounter.get();
    }

    /**
     * resets the header counter back to zero and returns how much header bytes
     * have been filtered out
     * @return how much header bytes have been filtered out
     */
    public long getAndResetHeaderCounter() {
        return headerCounter.getAndSet(0);
    }

    /**
     * returns how much data has been forwarded
     * @return how much data has been forwarded
     */
    public long getDataCounter() {
        return dataCounter.get();
    }

    /**
     * resets the data counter back to zero and returns how much data has been
     * forwarded
     * @return how much data has been forwarded
     */
    public long getAndResetDataCounter() {
        return dataCounter.getAndSet(0);
    }

    private void unframe(ByteBuffer buffer) throws IOException {
        while (setFrameBorders(buffer)) {
            ByteBufferForwardingMode.DIRECT.forwardBufferHead(
                    buffer, currentLength, nextForwarder);
        }
    }

    private boolean setFrameBorders(ByteBuffer buffer) {
        if (determineLength) {
            int remaining = buffer.remaining();
            if (remaining < HEADER_SIZE) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                            "incomplete header (header is " + HEADER_SIZE +
                            " byte but only " + remaining +
                            " bytes are currently available)");
                }
            } else {
                // read and evaluate the length
                currentLength = 0;
                int shift = (HEADER_SIZE - 1) * 8;
                for (int i = 0; i < HEADER_SIZE; i++) {
                    currentLength |= ((buffer.get() & 0xFF) << shift);
                    shift -= 8;
                }
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                            "currentLength = " + currentLength + " byte");
                }
                determineLength = false;

                // accounting
                headerCounter.addAndGet(HEADER_SIZE);
            }
        }

        if (!determineLength) {
            int remaining = buffer.remaining();
            if (remaining < currentLength) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                            "incomplete message (message is " + currentLength +
                            " byte but only " + remaining +
                            " bytes are currently available)");
                }
            } else {
                determineLength = true;

                // accounting
                dataCounter.addAndGet(currentLength);
                return true;
            }
        }
        return false;
    }
}
