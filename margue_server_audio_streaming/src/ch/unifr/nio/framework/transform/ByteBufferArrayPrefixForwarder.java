/*
 * ByteBufferArrayPrefixForwarder.java
 *
 * Created on 28.07.2008, 16:23:24
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Forwarder that prefixes a ByteBuffer array with another ByteBuffer.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class ByteBufferArrayPrefixForwarder
        extends AbstractPrefixForwarder<ByteBuffer[], ByteBuffer[]> {

    private static final Logger logger =
            Logger.getLogger(ByteBufferArrayPrefixForwarder.class.getName());

    /**
     * creates a new ByteBufferArrayPrefixForwarder
     * @param prefixByteBufferForwardingMode the buffer forwarding mode for the
     * prefix ByteBuffer
     */
    public ByteBufferArrayPrefixForwarder(
            ByteBufferForwardingMode prefixByteBufferForwardingMode) {
        this.prefixByteBufferForwardingMode = prefixByteBufferForwardingMode;
    }

    @Override
    public synchronized void forward(ByteBuffer[] input) throws IOException {
        this.forward(prefix, input);
    }

    /**
     * creates a ByteBuffer[] of a prefix and input and forwards the array to
     * the next forwarder
     * @param prefix the prefix
     * @param input the input
     * @throws java.io.IOException if an I/O exception occurs
     */
    public synchronized void forward(ByteBuffer prefix, ByteBuffer[] input)
            throws IOException {
        if (nextForwarder == null) {
            logger.log(Level.SEVERE, "no nextForwarder => data lost!");
        } else {
            prefixCounter.addAndGet(prefix.remaining());
            int inputLength = input.length;
            ByteBuffer[] newArray = new ByteBuffer[inputLength + 1];
            newArray[0] = prefixByteBufferForwardingMode.getByteBuffer(prefix);
            System.arraycopy(input, 0, newArray, 1, inputLength);
            nextForwarder.forward(newArray);
            if (prefixByteBufferForwardingMode ==
                    ByteBufferForwardingMode.DIRECT) {
                prefix.rewind();
            }
        }
    }
}
