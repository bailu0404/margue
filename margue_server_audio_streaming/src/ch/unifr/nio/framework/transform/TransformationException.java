/*
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

/**
 * An exception that occured while transformations.
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class TransformationException extends Exception {

    /**
     * Creates a new instance of <code>TransformationException</code> without
     * detail message.
     */
    public TransformationException() {
    }


    /**
     * Constructs an instance of <code>TransformationException</code> with the
     * specified detail message.
     * @param msg the detail message.
     */
    public TransformationException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>TransformationException</code> with the
     * specified cause.
     * @param cause the cause for this exception
     */
    public TransformationException(Throwable cause) {
        super(cause);
    }
}
