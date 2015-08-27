/*
 * AbstractForwarder.java
 *
 * Created on 03.02.2008, 13:24:42
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

/**
 * The base class for all forwarders in a forwarding hierarchy
 * @param <I> the type of input this forwarder accepts
 * @param <O> the type of output this forwarder produces
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public abstract class AbstractForwarder<I, O> {

    /**
     * the next forwarder in a forwarding hierarchy
     */
    protected AbstractForwarder<O, ?> nextForwarder;

    /**
     * accepts and forwards input
     * @param input the input to forward
     * @throws java.io.IOException if an I/O exception occured during
     * forwarding
     */
    public abstract void forward(I input) throws IOException;

    /**
     * sets the next forwarder in a forwarding hierarchy
     * @param nextForwarder the next forwarder in a forwarding hierarchy
     */
    public synchronized void setNextForwarder(
            AbstractForwarder<O, ?> nextForwarder) {
        this.nextForwarder = nextForwarder;
    }
}
