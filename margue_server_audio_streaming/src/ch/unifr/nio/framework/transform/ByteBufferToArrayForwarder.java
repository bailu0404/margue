/*
 * ByteBufferToArrayForwarder.java
 *
 * Created on 24.03.2008, 16:12:12
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
 * A forwarder that forwards a ByteBuffer as a ByteBuffer array holding this one
 * ByteBuffer.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class ByteBufferToArrayForwarder
        extends AbstractForwarder<ByteBuffer, ByteBuffer[]> {

    private static final Logger logger =
            Logger.getLogger(ByteBufferToArrayForwarder.class.getName());
    private static final ByteBuffer[] array = new ByteBuffer[1];

    @Override
    public synchronized void forward(ByteBuffer input) throws IOException {
        if (nextForwarder == null) {
            logger.log(Level.SEVERE, "no nextForwarder => data lost!");
        } else {
            array[0] = input;
            nextForwarder.forward(array);
        }
    }
}
