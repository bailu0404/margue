/*
 * DummyTrafficOutputForwarder.java
 *
 * Created on 28.07.2008, 12:48:43
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A forwarder that generates dummy traffic when there are no meaningful
 * messages to be sent.
 * To distinguish between meaningful and dummy traffic this forwarder uses an
 * internal FramingOutputForwarder to frame messages of different types.
 * The first byte of the single messages marks the message type (DATA or DUMMY).
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class DummyTrafficOutputForwarder
        extends AbstractForwarder<ByteBuffer[], ByteBuffer[]>
        implements TrafficShaper {

    /**
     * used for framing/unframing of DATA messages
     */
    public static final byte DATA = 0;
    /**
     * used for framing/unframing of DUMMY messages
     */
    public static final byte DUMMY = 1;
    private static final Logger logger =
            Logger.getLogger(DummyTrafficOutputForwarder.class.getName());
    private static final int PREFIX_SIZE = 1;
    private final ByteBuffer DATA_PREFIX;
    private final ByteBuffer DUMMY_PREFIX;
    // buffers all incoming (unframed) data
    private final BufferForwarder unframedBufferForwarder;
    // adds the corresponding "DATA" or "DUMMY" prefixes
    private final ByteBufferPrefixForwarder bufferPrefixForwarder;
    private final ByteBufferArrayPrefixForwarder arrayPrefixForwarder;
    // frames all messages (adds a binary length header)
    private final FramingOutputForwarder framingForwarder;
    // buffers frames
    private final BufferArrayForwarder bufferArrayForwarder;
    // the maximum size of a message we can send with the given header size
    private final int MAX_MESSAGE_SIZE;
    // the sum of all header sizes (framing and type prefix)
    private final int ALL_HEADER_SIZE;
    private boolean active;
    private final AtomicLong dataCounter = new AtomicLong();
    private final AtomicLong dummyCounter = new AtomicLong();
    // the one and only ByteBuffer that is allocated for dummy messages
    private static ByteBuffer dummyBuffer = ByteBuffer.allocate(0);
    private int id;
    private static int static_id;

    /**
     * creates a new DummyTrafficOutputForwarder
     * @param framingHeaderSize the size of the framing header
     * (determines the maximum package size = 2^(8*framingHeaderSize) - 1)
     */
    public DummyTrafficOutputForwarder(int framingHeaderSize) {

        // used to separate different DummyTrafficOutputForwarders in log files
        id=static_id++;

        // we use one additional byte for the message type prefix
        ALL_HEADER_SIZE = framingHeaderSize + PREFIX_SIZE;

        // create inner forwarding hierarchy:
        //  unframed -> bufferprefix -
        //                            |-> framing -> bufferArray -> next
        //               arrayprefix -
        bufferArrayForwarder = new BufferArrayForwarder();
        framingForwarder = new FramingOutputForwarder(framingHeaderSize,
                ByteBufferForwardingMode.COPY);
        framingForwarder.setNextForwarder(bufferArrayForwarder);
        unframedBufferForwarder = new BufferForwarder(
                ByteBufferForwardingMode.DUPLICATE);
        bufferPrefixForwarder = new ByteBufferPrefixForwarder(
                ByteBufferForwardingMode.DUPLICATE);
        bufferPrefixForwarder.setNextForwarder(framingForwarder);
        arrayPrefixForwarder = new ByteBufferArrayPrefixForwarder(
                ByteBufferForwardingMode.DUPLICATE);
        arrayPrefixForwarder.setNextForwarder(framingForwarder);
        unframedBufferForwarder.setNextForwarder(bufferPrefixForwarder);

        // we need one more byte for the message type prefix DATA or DUMMY
        MAX_MESSAGE_SIZE = framingForwarder.getMaxSize() - PREFIX_SIZE;

        // create the DATA and DUMMY prefixes
        DATA_PREFIX = ByteBuffer.allocate(PREFIX_SIZE);
        DATA_PREFIX.put(DATA);
        DATA_PREFIX.flip();
        DUMMY_PREFIX = ByteBuffer.allocate(PREFIX_SIZE);
        DUMMY_PREFIX.put(DUMMY);
        DUMMY_PREFIX.flip();

        // hardwire both prefix forwarder to DATA_PREFIX
        // (DUMMY_PREFIX is used dynamically)
        bufferPrefixForwarder.setPrefix(DATA_PREFIX);
        arrayPrefixForwarder.setPrefix(DATA_PREFIX);

        setActive(false);
    }

    @Override
    public synchronized void forward(ByteBuffer[] inputs) throws IOException {

        int inputLength = (inputs == null) ? 0 : inputs.length;
        if (inputLength == 0) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        "id " + id + ": nothing to forward, input was: " +
                        Arrays.toString(inputs));
            }
            return;
        }

        if (active) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "id " + id +
                        ": forwarder is active -> buffering input");
            }
            // add all input to the unframedBufferForwarder and wait for a call
            // to sendPackage()
            unframedBufferForwarder.forward(inputs);

        } else {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "id " + id +
                        ": forwarder is not active -> no buffering");
            }

            // calculate the sum of all input buffer lengths
            int inputSize = 0;
            for (ByteBuffer byteBuffer : inputs) {
                inputSize += byteBuffer.remaining();
            }

            if (inputSize > MAX_MESSAGE_SIZE) {
                // we have to send several DATA messages
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "id " + id + ": Input is " +
                            FrameworkTools.getDataVolumeString(inputSize, 1) +
                            " but the maximum message size is only " +
                            FrameworkTools.getDataVolumeString(
                            MAX_MESSAGE_SIZE, 1) + ". Input must be " +
                            "scattered (which is a slow operation). It is " +
                            "recommended to increase the framingHeaderSize!");
                }
                scatter(inputs);

            } else {
                // we can send a single DATA message
                if (inputLength == 1) {
                    bufferPrefixForwarder.forward(inputs[0]);
                } else {
                    arrayPrefixForwarder.forward(inputs);
                }
            }

            bufferArrayForwarder.flush();
        }
    }

    @Override
    public synchronized void setNextForwarder(
            AbstractForwarder<ByteBuffer[], ?> nextForwarder) {
        super.setNextForwarder(nextForwarder);
        bufferArrayForwarder.setNextForwarder(nextForwarder);
    }

    @Override
    public synchronized void sendPackage(long packageSize) throws IOException {
        if (!active) {
            String errorMessage = "id " + id +
                    ": DummyTrafficOutputForwarder is inactive, " +
                    "can not send dummy packages";
            logger.severe(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // determine available space
        int alreadyFramed = bufferArrayForwarder.remaining();
        long availableSpace = packageSize - alreadyFramed;
        if (logger.isLoggable(Level.FINEST)) {
            // do not show negative numbers in log
            // (doesn't matter in the rest of the code...)
            availableSpace = Math.max(availableSpace, 0);
            logger.log(Level.FINEST,
                    "\n\tid " + id + ":" +
                    "\n\tpackage size = " + packageSize + " byte" +
                    "\n\talready framed data = " + alreadyFramed + " byte" +
                    "\n\tavailable space = " + availableSpace + " byte");
        }

        // create new DATA messages as long as we have enough space and data
        while ((availableSpace > 0) && unframedBufferForwarder.hasRemaining()) {
            // enforce MAX_MESSAGE_SIZE upper limit
            long messageSize = Math.min(
                    availableSpace - ALL_HEADER_SIZE, MAX_MESSAGE_SIZE);
            int remainingUnframed = unframedBufferForwarder.remaining();
            messageSize = Math.min(messageSize, remainingUnframed);
            // every DATA package must contain at least one byte payload
            // (otherwise we can have situations where we only send headers
            // all the time)
            messageSize = Math.max(messageSize, 1);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "\n\tid " + id + ":" +
                        "\n\tavailable space = " + availableSpace + " byte" +
                        "\n\tavailable unframed data = " + remainingUnframed +
                        " byte" +
                        "\n\tadding a DATA package with " + messageSize +
                        " byte payload");
            }

            dataCounter.addAndGet(messageSize);
            unframedBufferForwarder.forward(messageSize);
            availableSpace -= (messageSize + ALL_HEADER_SIZE);
        }

        // create new DUMMY messages as long as we have enough space
        while (availableSpace > 0) {
            long tmpMessageSize = Math.min(
                    availableSpace - ALL_HEADER_SIZE, MAX_MESSAGE_SIZE);
            tmpMessageSize = Math.max(tmpMessageSize, 0);
            int messageSize;
            if (tmpMessageSize > Integer.MAX_VALUE) {
                logger.warning("id " + id + ": had to reset message size (" +
                        tmpMessageSize + ") to Integer.MAX_VALUE (" +
                        Integer.MAX_VALUE + ')');
                messageSize = Integer.MAX_VALUE;
            } else {
                messageSize = (int) tmpMessageSize;
            }
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "\n\tid " + id + ":" +
                        "\n\tavailable space = " + availableSpace + " byte" +
                        "\n\tadding a DUMMY package with " + messageSize +
                        " byte payload");
            }

            // we could test here if messageSize > 0 and only then increase the
            // counter and allocate the ByteBuffer but because the probability
            // of messageSize == 0 here is very low and the next both statements
            // are so cheap, constantly checking is probably the more expensive
            // operation in the long run...
            dummyCounter.addAndGet(messageSize);
            bufferPrefixForwarder.forward(
                    DUMMY_PREFIX, getDummyBuffer(messageSize));
            availableSpace -= (messageSize + ALL_HEADER_SIZE);
        }

        // finally send the package
        bufferArrayForwarder.sendPackage(packageSize);
    }

    @Override
    public synchronized void setActive(boolean active) {
        this.active = active;
        if (!active) {
            try {
                // flush buffered unframed data
                if (unframedBufferForwarder.hasRemaining()) {
                    int remaining = unframedBufferForwarder.remaining();
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "id " + id + ": flushing " +
                                remaining + " byte in unframedBufferForwarder");
                    }
                    unframedBufferForwarder.forward(remaining);
                    dataCounter.addAndGet(remaining);
                }
                // flush buffered frames
                if (bufferArrayForwarder.hasRemaining()) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "id " + id + ": flushing " +
                                bufferArrayForwarder.remaining() +
                                " byte in bufferArrayForwarder");
                    }
                    bufferArrayForwarder.flush();
                }
            } catch (IOException ex) {
                FrameworkTools.handleStackTrace(logger, ex);
            }
        }
    }

    /**
     * returns <code>true</code>, if this forwarder generates dummy traffic,
     * <code>false</code> otherwise
     * @return <code>true</code>, if this forwarder generates dummy traffic,
     * <code>false</code> otherwise
     */
    public synchronized boolean isActive() {
        return active;
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
     * returns how much protocol data has been forwarded
     * @return how much protocol data has been forwarded
     */
    public long getProtocolCounter() {
        return bufferPrefixForwarder.getPrefixCounter() +
                arrayPrefixForwarder.getPrefixCounter() +
                framingForwarder.getHeaderCounter();
    }

    /**
     * resets the protocol counter back to zero and returns how much protocol
     * data has been forwarded
     * @return how much protocol data has been forwarded
     */
    public long getAndResetProtocolCounter() {
        return bufferPrefixForwarder.getAndResetPrefixCounter() +
                arrayPrefixForwarder.getAndResetPrefixCounter() +
                framingForwarder.getAndResetHeaderCounter();
    }

    private void scatter(ByteBuffer[] inputs) throws IOException {

        for (int i = 0, length = inputs.length; i < length;) {

            ByteBuffer input = inputs[i];
            int inputSize = input.remaining();

            if (inputSize < MAX_MESSAGE_SIZE) {

                // forward several inputs at once
                for (int j = i + 1; j < length; j++) {
                    ByteBuffer nextInput = inputs[j];
                    int nextRemaining = nextInput.remaining();
                    inputSize += nextRemaining;
                    int overHead = inputSize - MAX_MESSAGE_SIZE;

                    if (overHead > 0) {
                        // nextInput is too large, split it up
                        int headSize = nextRemaining - overHead;
                        ByteBuffer head =
                                FrameworkTools.splitBuffer(nextInput, headSize);
                        int lastIndex = j - i;
                        int arraySize = lastIndex + 1;
                        ByteBuffer[] array = new ByteBuffer[arraySize];
                        System.arraycopy(inputs, i, array, 0, lastIndex);
                        array[lastIndex] = head;
                        arrayPrefixForwarder.forward(array);
                        i += lastIndex;
                        break; // inner for loop

                    } else if (overHead == 0) {
                        // nextInput just fits in
                        int arraySize = j - i + 1;
                        forwardArray(arraySize, inputs, i);
                        i += arraySize;
                        break; // inner for loop
                    }
                }

                if (inputSize < MAX_MESSAGE_SIZE) {
                    // The sum of all trailing inputs did not reach
                    // MAX_MESSAGE_SIZE.
                    // We can now send everything we have.
                    int arraySize = length - i;
                    if (arraySize == 1) {
                        bufferPrefixForwarder.forward(input);
                    } else {
                        forwardArray(arraySize, inputs, i);
                    }
                    i += arraySize;
                }

            } else if (inputSize > MAX_MESSAGE_SIZE) {
                // cut off and forward input head
                ByteBuffer head =
                        FrameworkTools.splitBuffer(input, MAX_MESSAGE_SIZE);
                bufferPrefixForwarder.forward(head);
            // current input still contains data, so no i++ here...

            } else {
                // inputSize == MAX_MESSAGE_SIZE
                // send a single message
                bufferPrefixForwarder.forward(input);
                i++;
            }
        }
    }

    private void forwardArray(int arraySize, ByteBuffer[] inputs, int i)
            throws IOException {
        ByteBuffer[] array = new ByteBuffer[arraySize];
        System.arraycopy(inputs, i, array, 0, arraySize);
        arrayPrefixForwarder.forward(array);
    }

    private static ByteBuffer getDummyBuffer(int size) {
        if (dummyBuffer.remaining() < size) {
            dummyBuffer = ByteBuffer.allocate(size);
        }
        ByteBuffer duplicate = dummyBuffer.duplicate();
        duplicate.limit(size);
        return duplicate;
    }
}
