/*
 * ChannelReader.java
 *
 * Created on 30.03.2008, 15:59:28
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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that handles many details when reading from a channel.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class ChannelReader extends AbstractForwarder<Void, ByteBuffer> {

    private final static Logger logger =
            Logger.getLogger(ChannelReader.class.getName());
    /**
     * the channel where to read data from
     */
    private ReadableByteChannel channel;
    /**
     * counts how many bytes have been read from the channel
     */
    private final AtomicLong readCounter = new AtomicLong();
    /**
     * Holds the buffer data. Is normally in "drain" mode, i.e. user data is in
     * range including [position() ... limit() - 1].
     */
    private ByteBuffer buffer;
    /**
     * indicates if the associated data stream is closed
     */
    private boolean closed;
    private final int maxCapacity;

    /**
     * creates a new ChannelReader
     * @param direct <CODE>true</CODE>, if the buffer must be allocated
     * directly, <CODE>false</CODE> otherwise
     * @param initialCapacity the initial capacity of the buffer (it is
     * automatically enlarged if no space is left when filling from the channel)
     * @param maxCapacity The maximum capacity for the buffer. This parameter
     * will be checked when enlarging the buffer while filling it with new data
     * from the channel.
     */
    public ChannelReader(boolean direct, int initialCapacity, int maxCapacity) {
        this.maxCapacity = maxCapacity;
        if (direct) {
            buffer = ByteBuffer.allocateDirect(initialCapacity);
        } else {
            buffer = ByteBuffer.allocate(initialCapacity);
        }
        buffer.flip();
    }

    /**
     * sets the Channel to read from
     * @param channel the Channel to read from
     */
    public synchronized void setChannel(SelectableChannel channel) {
        this.channel = (ReadableByteChannel) channel;
    }

    /**
     * reads data from <CODE>channel</CODE> and forwards it to the
     * transformation chain
     * @return true, if new data was read from the channel, otherwise false
     * @throws java.io.IOException if filling the buffer fails
     */
    public synchronized boolean read() throws IOException {
        buffer.compact();
        int tmpCounter = 0;
        for (int bytesRead = 1; bytesRead > 0;) {
            // check if there is still space left in the buffer
            if (!buffer.hasRemaining()) {
                // we must not grow larger than maxCapacity!
                int oldCapacity = buffer.capacity();
                if (oldCapacity < maxCapacity) {
                    // we need to enlarge the buffer ByteBuffer
                    // (try simple double size here)
                    int newCapacity = Math.min(oldCapacity * 2, maxCapacity);
                    buffer = FrameworkTools.enlargeBuffer(buffer, newCapacity);
                } else {
                    if (logger.isLoggable(Level.SEVERE)) {
                        logger.log(Level.SEVERE, "can not enlarge buffer, " +
                                "it already reached maxCapacity!");
                    }
                }
            }
            bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                closed = true;
            } else {
                tmpCounter += bytesRead;
            }
            break;
        }
        buffer.flip();
        readCounter.addAndGet(tmpCounter);

        if (logger.isLoggable(Level.FINEST)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tmpCounter);
            stringBuilder.append(" Bytes read, buffer: ");
            stringBuilder.append(buffer);
            if (nextForwarder == null) {
                stringBuilder.append("\n\tThere is no forwarder! Storing " +
                        "data here in ChannelReader...");
            } else {
                stringBuilder.append("\n\tnextForwarder: ");
                stringBuilder.append(nextForwarder.getClass().getName());
            }
            logger.log(Level.FINEST, stringBuilder.toString());
        }
        forward();

        return tmpCounter > 0;
    }

    /**
     * tries to forward all buffered data to the next forwarder
     * @throws java.io.IOException if an I/O exception occurs while forwarding
     */
    public synchronized void forward() throws IOException {
        if (buffer.hasRemaining()) {
            if (nextForwarder == null) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING,
                            "can not forward, nextForwarder is null");
                }
            } else {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                            "calling nextForwarder.forward(" + buffer + ')');
                }
                nextForwarder.forward(buffer);
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "nothing to forward, buffer is empty");
            }
        }
    }

    /**
     * returns how many bytes have been read from the channel
     * @return how many bytes have been read from the channel
     */
    public long getReadCounter() {
        return readCounter.get();
    }

    /**
     * resets the counter back to zero and returns how many bytes have been read
     * from the channel
     * @return how many bytes have been read from the channel
     */
    public long getAndResetReadCounter() {
        return readCounter.getAndSet(0);
    }

    /**
     * returns <CODE>true</CODE>, if the reader is closed, <CODE>false</CODE>
     * otherwise
     * @return <CODE>true</CODE>, if the reader is closed, <CODE>false</CODE>
     * otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns a reference to the internal buffer. This is necessary, e.g. if
     * there are no transformaions needed
     * @return a reference to the internal buffer
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public void forward(Void input) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }
}
