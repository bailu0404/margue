/*
 * StringToByteBufferTransformer.java
 *
 * Created on 03.03.2008, 16:45:20
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
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A transformer that transforms a String into a ByteBuffer.
 * 
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class StringToByteBufferTransformer
        extends AbstractTransformer<String, ByteBuffer> {

    private final static Logger logger =
            Logger.getLogger(StringToByteBufferTransformer.class.getName());
    private CharsetEncoder charsetEncoder;

    /**
     * creates a new StringToByteBufferTransformer that uses the default charset
     * for transforming
     */
    public StringToByteBufferTransformer() {
        this(Charset.defaultCharset());
    }

    /**
     * creates a new StringToByteBufferTransformer with a given charset to use
     * @param charset the Charset to use for transforming
     */
    public StringToByteBufferTransformer(Charset charset) {
        setCharsetPrivate(charset);
    }

    @Override
    public synchronized void forward(String input) throws IOException {
        if (nextForwarder == null) {
            logger.log(Level.SEVERE, "nextTransformer == null -> data lost");
        } else {
            try {
                ByteBuffer byteBuffer = transform(input);
                nextForwarder.forward(byteBuffer);
            } catch (TransformationException ex) {
                throw new IOException(ex);
            }
        }
    }

    /**
     * sets the charset to use for transforming
     * @param charset
     */
    public synchronized void setCharset(Charset charset) {
        setCharsetPrivate(charset);
    }

    private void setCharsetPrivate(Charset charset) {
        charsetEncoder = charset.newEncoder();
    }

    @Override
    public ByteBuffer transform(String input) throws TransformationException {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "transforming \"" + input + "\"");
        }
        CharBuffer charBuffer = CharBuffer.allocate(input.length());
        charBuffer.put(input);
        charBuffer.flip();
        try {
            return charsetEncoder.encode(charBuffer);
        } catch (CharacterCodingException ex) {
            throw new TransformationException(ex);
        }
    }
}
