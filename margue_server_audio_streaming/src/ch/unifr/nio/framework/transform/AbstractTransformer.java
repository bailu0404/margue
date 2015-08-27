
/**
 * AbstractTransformer.java
 *
 * Created on 23.11.2008, 19:34:47
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

/**
 * An AbstractTransformer transforms objects.
 * @param <I> the input type of this AbstractTransformer
 * @param <O> the output type of this AbstractTransformer
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public abstract class AbstractTransformer<I, O>
        extends AbstractForwarder<I, O> {

    /**
     * transforms the input
     * @param input the input to transform
     * @return the transformed input, aka output
     * @throws TransformationException if an exception occured during
     * transformation
     */
    public abstract O transform(I input) throws TransformationException;
}
