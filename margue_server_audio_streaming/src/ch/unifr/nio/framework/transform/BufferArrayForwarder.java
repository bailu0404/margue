/*
 * BufferArrayForwarder.java
 *
 * Created on 27.01.2009, 16:56:03
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A forwarder that buffers data and can forward chunks of data with a given
 * or a maximum size. This way it can be used as a component for traffic
 * shaping.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class BufferArrayForwarder
        extends AbstractForwarder<ByteBuffer[], ByteBuffer[]>
        implements TrafficShaper {

    private final static Logger logger =
            Logger.getLogger(BufferArrayForwarder.class.getName());
    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();

    @Override
    public synchronized void forward(ByteBuffer[] inputs) throws IOException {
        // add all input
        for (ByteBuffer input : inputs) {
            // but filter out empty inputs
            if (input != null && input.hasRemaining()) {
                buffers.add(input);
            }
        }
    }

    @Override
    public synchronized void sendPackage(long maxPackageSize)
            throws IOException {

        // cheap initial tests
        int size = buffers.size();
        if (size == 0) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "nothing to send");
            }
            return;
        }
        if (nextForwarder == null) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "no nextForwarder");
            }
            return;
        }

        // check how many buffers we may send
        int numberOfBuffers = 0;
        long packageSize = 0;
        int overhead = 0;
        do {
            ByteBuffer buffer = buffers.get(numberOfBuffers++);
            packageSize += buffer.remaining();
            overhead = (int) (packageSize - maxPackageSize);
        } while ((numberOfBuffers < size) && (overhead < 0));
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "numberOfBuffers = " + numberOfBuffers);
        }

        // check if the limit of the last buffer must be set
        ByteBuffer limitResetBuffer = null;
        int oldLimit = 0;
        if (packageSize > maxPackageSize) {
            limitResetBuffer = buffers.get(numberOfBuffers - 1);
            oldLimit = limitResetBuffer.limit();
            int newLimit = limitResetBuffer.position() +
                    limitResetBuffer.remaining() - overhead;
            limitResetBuffer.limit(newLimit);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "limit of last buffer was set: " + limitResetBuffer);
            }
        }

        // fill temporary array
        ByteBuffer[] tmpArray = new ByteBuffer[numberOfBuffers];
        for (int i = 0; i < numberOfBuffers; i++) {
            tmpArray[i] = buffers.get(i);
        }
        if (logger.isLoggable(Level.FINEST)) {
            StringBuilder stringBuilder =
                    new StringBuilder("\ttemporary array:\n");
            for (ByteBuffer byteBuffer : tmpArray) {
                stringBuilder.append("\t\t" + byteBuffer + '\n');
            }
            logger.log(Level.FINEST, stringBuilder.toString());
        }

        // send array
        nextForwarder.forward(tmpArray);

        // if necessary, restore limit of last buffer
        if (limitResetBuffer != null) {
            limitResetBuffer.limit(oldLimit);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "limit of last buffer was reset: " + limitResetBuffer);
            }
        }

        trim();
    }

    @Override
    public void setActive(boolean active) {
        // nothing to do here...
    }

    /**
     * returns the number of buffered bytes
     * @return the number of buffered bytes
     */
    public int remaining() {
        int remaining = 0;
        for (int i = 0, size = buffers.size(); i < size; i++) {
            remaining += buffers.get(i).remaining();
        }
        return remaining;
    }

    /**
     * A convenience method that returns <tt>true</tt>, when there are buffered
     * bytes available, <tt>false</tt> otherwise
     * @return <tt>true</tt>, when there are buffered
     * bytes available, <tt>false</tt> otherwise
     */
    public boolean hasRemaining() {
        return remaining() > 0;
    }

    /**
     * tries forwarding all buffered data to the next forwarder
     * @throws java.io.IOException if an I/O exception occurs
     */
    public synchronized void flush() throws IOException {
        // cheap initial tests
        if (buffers.size() == 0) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "nothing to send");
            }
            return;
        }
        if (nextForwarder == null) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "no nextForwarder");
            }
            return;
        }

        // fill temporary array
        ByteBuffer[] tmpArray = new ByteBuffer[buffers.size()];
        tmpArray = buffers.toArray(tmpArray);

        // send array
        nextForwarder.forward(tmpArray);

        trim();
    }

    private void trim() {
        // (remove all empty buffers from list)
        for (int i = buffers.size() - 1; i >= 0; i--) {
            if (!buffers.get(i).hasRemaining()) {
                buffers.remove(i);
            }
        }

        if (logger.isLoggable(Level.FINEST)) {
            StringBuilder stringBuilder =
                    new StringBuilder("\ttrimmed list:\n");
            for (int i = 0, j = buffers.size(); i < j; i++) {
                ByteBuffer byteBuffer = buffers.get(i);
                stringBuilder.append("\t\t" + byteBuffer + '\n');
            }
            logger.log(Level.FINEST, stringBuilder.toString());
        }
    }
}
