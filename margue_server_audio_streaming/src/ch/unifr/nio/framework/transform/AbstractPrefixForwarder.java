/*
 * AbstractPrefixForwarder.java
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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Forwarder that adds a ByteBuffer prefix.
 * @param <I> the type of input this forwarder accepts
 * @param <O> the type of output this forwarder produces
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public abstract class AbstractPrefixForwarder<I, O>
        extends AbstractForwarder<I, O> {

    /**
     * the prefis that will be added
     */
    protected ByteBuffer prefix;
    /**
     * the forwarding mode that will be used for the prefix
     */
    protected ByteBufferForwardingMode prefixByteBufferForwardingMode;
    /**
     * counts how much prefix data volume has been generated
     */
    protected final AtomicLong prefixCounter = new AtomicLong();

    /**
     * sets the prefix to be used for the forward() operation
     * @param prefix
     */
    public synchronized void setPrefix(ByteBuffer prefix) {
        this.prefix = prefix;
    }

    /**
     * returns the ByteBufferForwardingMode that is used for the prefix
     * @return the ByteBufferForwardingMode that is used for the prefix
     */
    public synchronized ByteBufferForwardingMode getPrefixForwardingMode() {
        return prefixByteBufferForwardingMode;
    }

    /**
     * sets the ByteBufferForwardingMode that is used for the prefix
     * @param byteBufferForwardingMode the ByteBufferForwardingMode that is used
     * for the prefix
     */
    public synchronized void setPrefixForwardingMode(
            ByteBufferForwardingMode byteBufferForwardingMode) {
        prefixByteBufferForwardingMode = byteBufferForwardingMode;
    }

    /**
     * returns how much prefix data volume has been generated
     * @return how much prefix data volume has been generated
     */
    public long getPrefixCounter() {
        return prefixCounter.get();
    }

    /**
     * resets the prefix counter back to zero and returns how much prefix data
     * volume has been generated
     * @return how much prefix data volume has been generated
     */
    public long getAndResetPrefixCounter() {
        return prefixCounter.getAndSet(0);
    }

}
