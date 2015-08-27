/*
 * DummyTrafficInputForwarder.java
 *
 * Created on 28.07.2008, 17:07:15
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
 * A forwarder that removes dummy messages from a data stream
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class DummyTrafficInputForwarder
        extends AbstractForwarder<ByteBuffer, ByteBuffer> {

    private static final Logger logger =
            Logger.getLogger(DummyTrafficInputForwarder.class.getName());
    private final FramingInputForwarder framingInputForwarder;
    private final AtomicLong headerCounter = new AtomicLong();
    private final AtomicLong dataCounter = new AtomicLong();
    private final AtomicLong dummyCounter = new AtomicLong();
    private final DummyTrafficFilterForwarder dummyTrafficFilterForwarder;

    /**
     * creates a new DummyTrafficInputForwarder
     * @param framingHeaderSize the size of the framing header
     */
    public DummyTrafficInputForwarder(int framingHeaderSize) {
        // create necessary internal forwarders
        framingInputForwarder = new FramingInputForwarder(framingHeaderSize);
        dummyTrafficFilterForwarder =
                new DummyTrafficFilterForwarder(framingHeaderSize);

        // connect them
        framingInputForwarder.setNextForwarder(dummyTrafficFilterForwarder);
    }

    @Override
    public void forward(ByteBuffer input) throws IOException {
        // push through internal forward hierarchy
        framingInputForwarder.forward(input);
    }

    @Override
    public synchronized void setNextForwarder(
            AbstractForwarder<ByteBuffer, ?> nextForwarder) {
        dummyTrafficFilterForwarder.setNextForwarder(nextForwarder);
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

    /**
     * returns how much dummy traffic has been forwarded
     * @return how much dummy traffic has been forwarded
     */
    public long getDummyCounter() {
        return dummyCounter.get();
    }

    /**
     * resets the dummy counter back to zero and returns how much dummy traffic
     * has been forwarded
     * @return how much dummy traffic has been forwarded
     */
    public long getAndResetDummyCounter() {
        return dummyCounter.getAndSet(0);
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

    private class DummyTrafficFilterForwarder
            extends AbstractForwarder<ByteBuffer, ByteBuffer> {

        private final int FRAMING_HEADER_SIZE;

        public DummyTrafficFilterForwarder(int framingHeaderSize) {
            FRAMING_HEADER_SIZE = framingHeaderSize;
        }

        @Override
        public synchronized void forward(ByteBuffer input) throws IOException {

            // accounting
            // If a ByteBuffer comes in here, the internal FramingInputForwarder
            // had to remove the framing header. Here we will remove a type
            // prefix byte (DATA or DUMMY).
            headerCounter.addAndGet(FRAMING_HEADER_SIZE + 1);

            // forwarding
            byte type = input.get();

            if (type == DummyTrafficOutputForwarder.DATA) {
                int remaining = input.remaining();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "received " + remaining +
                            " Byte DATA message: " + input);
                }
                dataCounter.addAndGet(remaining);
                if (nextForwarder == null) {
                    logger.log(
                            Level.SEVERE, "no nextForwarder => data lost!");
                } else {
                    if (input.hasRemaining()) {
                        nextForwarder.forward(input);
                    }
                }

            } else if (type == DummyTrafficOutputForwarder.DUMMY) {
                if (logger.isLoggable(Level.FINEST)) {
                    int remaining = input.remaining();
                    logger.log(Level.FINEST, "received " + remaining +
                            " Byte DUMMY message: " + input);
                }
                // "consume" message
                dummyCounter.addAndGet(input.remaining());
                input.position(input.limit());
            }
        }
    }
}
