/*
 * ChannelWriter.java
 *
 * Created on 1. Oktober 2006, 09:42
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

import ch.unifr.nio.framework.BufferSizeListener;
import ch.unifr.nio.framework.BufferSizeListenerHandler;
import ch.unifr.nio.framework.FrameworkTools;
import ch.unifr.nio.framework.HandlerAdapter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The last forwarder of an output forward hierarchy that writes into
 * a WritableChannel. It handles the details of channel specific write
 * operations (e.g. incomplete write operations, data buffering and operation 
 * interest handling).
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class ChannelWriter extends AbstractForwarder<ByteBuffer, Void> {

    /**
     * a counter for the written bytes
     */
    protected final AtomicLong writeCounter;
    /**
     * the channel where to write to
     */
    protected WritableByteChannel channel;
    /**
     * Holds the buffered data. Is normally in "drain" mode, i.e. user data is
     * in range including [position() ... limit() - 1].
     */
    // @GuardedBy("this")
    protected ByteBuffer buffer;
    private final static Logger logger =
            Logger.getLogger(ChannelWriter.class.getName());
    private final boolean direct;
    private HandlerAdapter handlerAdapter;
    // buffer fill state monitoring
    private final BufferSizeListenerHandler bufferSizeListenerHandler;

    /**
     * Creates a new instance of ChannelWriter
     * @param direct <CODE>true</CODE>, if the ByteBuffer must be allocated
     * directly,
     * <CODE>false</CODE> otherwise
     */
    public ChannelWriter(boolean direct) {
        this.direct = direct;
        writeCounter = new AtomicLong();
        bufferSizeListenerHandler = new BufferSizeListenerHandler(this);
    }

    /**
     * sets the writable channel
     * @param channel the writable channel
     */
    public synchronized void setChannel(WritableByteChannel channel) {
        this.channel = channel;
    }

    /**
     * sets the HandlerAdapter to use for changing interest ops
     * @param handlerAdapter the HandlerAdapter to use for changing interest ops
     */
    public synchronized void setHandlerAdapter(HandlerAdapter handlerAdapter) {
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    public synchronized void forward(ByteBuffer input) throws IOException {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("input: " + input);
        }
        if ((buffer == null) || !buffer.hasRemaining()) {
            // the buffer is empty
            // try writing the new data directly out the channel
            int bytesWritten = channel.write(input);
            writeCounter.addAndGet(bytesWritten);
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("bytesWritten = " + bytesWritten);
            }

            if (input.hasRemaining()) {
                // incomplete write
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                            "input.remaining() = " + input.remaining());
                }

                // append the remaining data to buffer
                buffer = FrameworkTools.append(direct, buffer, input);

                // register this ChannelWriter for future write operations
                handlerAdapter.addInterestOps(SelectionKey.OP_WRITE);
            }

        } else {
            // there is already some data in buffer
            // we must append the new data to buffer
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "buffer.remaining() = " + buffer.remaining());
            }
            buffer = FrameworkTools.append(direct, buffer, input);
        }

        detectFillLevelChanges();
    }

    @Override
    public void setNextForwarder(
            AbstractForwarder<Void, ?> nextForwarder) {
        throw new UnsupportedOperationException("ChannelWriter is always the " +
                "last component of a forwarding hierarchy");
    }

    /**
     * writes cached data to the channel
     * @return true, if draining was completed, false if there is remaining
     * buffered data when returning
     * @throws java.io.IOException
     */
    public synchronized boolean drain() throws IOException {
        if (buffer == null) {
            return true;

        } else {

            if (buffer.hasRemaining()) {

                int bytesWritten = channel.write(buffer);
                writeCounter.addAndGet(bytesWritten);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "bytesWritten = " + bytesWritten);
                }

                detectFillLevelChanges();
            }

            return !buffer.hasRemaining();
        }
    }

    /**
     * returns true, if there is unwritten data, false otherwise
     * @return true, if there is unwritten data, false otherwise
     */
    public synchronized boolean hasRemaining() {
        return (buffer == null) ? false : buffer.hasRemaining();
    }

    /**
     * returns the number of unwritten bytes remaining in the buffer
     * @return the number of unwritten bytes remaining in the buffer
     */
    public synchronized int remaining() {
        return (buffer == null) ? 0 : buffer.remaining();
    }

    /**
     * returns how many bytes have been written to the channel
     * @return how many bytes have been written to the channel
     */
    public long getWriteCounter() {
        return writeCounter.get();
    }

    /**
     * resets the counter back to zero and returns how many bytes have been
     * written to the channel
     * @return how many bytes have been written to the channel
     */
    public long getAndResetWriteCounter() {
        return writeCounter.getAndSet(0);
    }

    /**
     * registers BufferSizeListener as event receiver.
     * @param listener the listener to be registered
     */
    public synchronized void addBufferSizeListener(
            BufferSizeListener listener) {
        bufferSizeListenerHandler.addBufferSizeListener(listener);
    }

    /**
     * removes a BufferSizeListener as event receiver.
     * @param listener the listener to be removed
     */
    public synchronized void removeBufferSizeListener(
            BufferSizeListener listener) {
        bufferSizeListenerHandler.removeBufferSizeListener(listener);
    }

    private void detectFillLevelChanges() {
        if (buffer == null) {
            logger.log(Level.FINEST, "buffer == null");
        } else {
            int newLevel = buffer.remaining();
            bufferSizeListenerHandler.updateLevel(newLevel);
        }
    }
}
