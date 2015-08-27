/*
 * ByteBufferArraySequenceForwarder.java
 *
 * Created on 03.11.2008, 13:41:39
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
 * A forwarder that forwards an array of ByteBuffers as a sequence of
 * ByteBuffers.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class ByteBufferArraySequenceForwarder
        extends AbstractForwarder<ByteBuffer[], ByteBuffer> {

    private static final Logger logger = Logger.getLogger(
            ByteBufferArraySequenceForwarder.class.getName());

    @Override
    public synchronized void forward(ByteBuffer[] inputs) throws IOException {
        if (nextForwarder == null) {
            logger.log(Level.SEVERE, "no nextForwarder => data lost!");
        } else {
            for (ByteBuffer input : inputs) {
                nextForwarder.forward(input);
            }
        }
    }
}
