/*
 * SSLOutputForwarder.java
 *
 * Created on 23.03.2008, 19:55:44
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

import ch.unifr.nio.framework.BufferSizeListener;
import ch.unifr.nio.framework.BufferSizeListenerHandler;
import ch.unifr.nio.framework.FrameworkTools;
import ch.unifr.nio.framework.ssl.HandshakeNotifier;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

/**
 * A forwarder that encrypts outbound SSL traffic (plaintext -> ciphertext).
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class SSLOutputForwarder
        extends AbstractForwarder<ByteBuffer[], ByteBuffer> {

    private final AtomicLong plainTextCounter = new AtomicLong();
    private final static Logger logger =
            Logger.getLogger(SSLOutputForwarder.class.getName());
    private final SSLEngine sslEngine;
    // GuardedBy("this")
    private final ByteBuffer[] plainTextArray = new ByteBuffer[1];
    private ByteBuffer plainText,  cipherText;
    private boolean switchToInput;
    // buffer fill state monitoring
    private final BufferSizeListenerHandler bufferSizeListenerHandler;
    /**
     * The HandshakeNotifier is an utility class shared by an
     * SSLInputForwarder and its SSLOutputForwarder. It forwards "handshake
     * completed" events to a list of registered listeners.
     */
    private HandshakeNotifier handshakeNotifier;
    private SSLInputForwarder sslInputForwarder;

    /**
     * Creates a new instance of SSLOutputForwarder
     * @param sslEngine the given {@link javax.net.ssl.SSLEngine SSLEngine} used
     * for encryption of outbound data
     * @param plainTextBufferSize the initial size of the plaintext buffer
     */
    public SSLOutputForwarder(SSLEngine sslEngine, int plainTextBufferSize) {
        this.sslEngine = sslEngine;
        /**
         * The plainText ByteBuffer holds the unencrypted plaintext data. It is
         * normally in "drain" mode, i.e. user data is in range including
         * [position() ... limit() - 1].
         */
        plainText = ByteBuffer.allocate(plainTextBufferSize);
        plainText.flip();
        /**
         * The cipherText ByteBuffer holds the encrypted ciphertext date.
         */
        int cipherSize = sslEngine.getSession().getPacketBufferSize() + 1000;
        cipherText = ByteBuffer.allocate(cipherSize);
        cipherText.flip();

        bufferSizeListenerHandler = new BufferSizeListenerHandler(this);
    }

    /**
     * sets the corresponding SSLInputForwarder
     * @param sslInputForwarder the corresponding SSLInputForwarder
     */
    public void setSSLInputForwarder(
            SSLInputForwarder sslInputForwarder) {
        this.sslInputForwarder = sslInputForwarder;
    }

    /**
     * registers BufferSizeListener as event receiver.
     * @param listener the listener to be registered
     */
    public synchronized void addBufferSizeListener(
            BufferSizeListener listener) {
        bufferSizeListenerHandler.addBufferSizeListener(listener);
    }

    /**
     * removes a BufferSizeListener as event receiver.
     * @param listener the listener to be removed
     */
    public synchronized void removeBufferSizeListener(
            BufferSizeListener listener) {
        bufferSizeListenerHandler.removeBufferSizeListener(listener);
    }

    /**
     * sets the {@link ch.unifr.nio.framework.ssl.HandshakeNotifier 
     * HandshakeNotifier}
     * @param handshakeNotifier the {@link 
     * ch.unifr.nio.framework.ssl.HandshakeNotifier HandshakeNotifier}
     */
    public void setHandshakeNotifier(HandshakeNotifier handshakeNotifier) {
        this.handshakeNotifier = handshakeNotifier;
    }

    @Override
    public synchronized void forward(ByteBuffer[] inputs) throws IOException {
        // accounting
        for (ByteBuffer input : inputs) {
            plainTextCounter.addAndGet(input.remaining());
        }

        if (plainText.hasRemaining()) {
            // There is already some pending data.
            // We must append the input buffers to plainText.
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "plainText: " + plainText);
            }
            for (ByteBuffer input : inputs) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "appending input (" + input +
                            ") to plainText (" + plainText + ")");
                }
                plainText = FrameworkTools.append(false, plainText, input);
            }

        } else {
            // encrypt and forward input
            cipherText.compact();
            SSLEngineResult sslEngineResult = null;
            do {
                if (logger.isLoggable(Level.FINEST)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < inputs.length; i++) {
                        stringBuilder.append("\tinput ").append(i).append(": ").
                                append(inputs[i]);
                        if (i < (inputs.length - 1)) {
                            stringBuilder.append('\n');
                        }
                    }
                    logger.log(Level.FINEST,
                            "input:\n" + stringBuilder.toString());
                }
                sslEngineResult = sslEngine.wrap(inputs, cipherText);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "bytesProduced = " +
                            sslEngineResult.bytesProduced());
                }
            } while (continueWrapping(sslEngineResult, inputs));
            cipherText.flip();

            // if encryption was incomplete we must append the remaining data
            for (ByteBuffer input : inputs) {
                if (input.hasRemaining()) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST,
                                "appending " + input + " to plainText");
                    }
                    plainText = FrameworkTools.append(false, plainText, input);
                }
            }

            // forward
            if (cipherText.hasRemaining()) {
                if (nextForwarder == null) {
                    logger.log(
                            Level.SEVERE, "no nextForwarder => data lost!");
                } else {
                    nextForwarder.forward(cipherText);
                }
            }
        }

        cleanUp();
    }

    /**
     * returns the number of Bytes in the plaintext buffer
     * @return the number of Bytes in the plaintext buffer
     */
    public synchronized int getRemainingPlaintext() {
        return plainText.remaining();
    }

    /**
     * returns true, if the plaintext buffer has remaining data,
     * false othterwise
     * @return true, if the plaintext buffer has remaining data,
     * false othterwise
     */
    public synchronized boolean hasRemainingPlaintext() {
        return plainText.hasRemaining();
    }

    /**
     * returns how many bytes of plain text have been forwarded
     * @return how many bytes of plain text have been forwarded
     */
    public long getPlainTextCounter() {
        return plainTextCounter.get();
    }

    /**
     * resets the plain text counter back to zero and returns the amount of
     * forwarded plain text in byte
     * @return the amount of forwarded plain text in byte
     */
    public long getAndResetPlainTextCounter() {
        return plainTextCounter.getAndSet(0);
    }

    synchronized void drain() throws IOException {
        /**
         * !!! We must not test here if the plaintext buffer contains data
         * before encryption. It may be empty when handshaking!!!
         */
        cipherText.compact();
        SSLEngineResult sslEngineResult = null;
        plainTextArray[0] = plainText;
        do {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "plainText: " + plainText);
            }
            sslEngineResult = sslEngine.wrap(plainText, cipherText);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "bytesProduced = " +
                        sslEngineResult.bytesProduced());
            }
        } while (continueWrapping(sslEngineResult, plainTextArray));
        cipherText.flip();

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST,
                    "cipherText after encryption: " + cipherText);
        }

        // forward data, if necessary and possible
        if (cipherText.hasRemaining() && (nextForwarder != null)) {
            nextForwarder.forward(cipherText);
        }

        cleanUp();
    }

    /**
     * checks, if we can continue with wrap() operations at the SSLEngine
     * @return <CODE>true</CODE>, if we may continue wrapping,
     * <CODE>false</CODE> otherwise
     * @param sslEngineResult the current SSLEngine result of the last wrap()
     * operation
     * @param message the message to be encrypted (wrapped)
     */
    @SuppressWarnings("fallthrough")
    private boolean continueWrapping(
            SSLEngineResult sslEngineResult, ByteBuffer[] buffers) {

        SSLEngineResult.Status status = sslEngineResult.getStatus();
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "status: " + status);
        }

        switch (status) {
            case BUFFER_OVERFLOW:
                // just enlarge buffer and retry wrapping
                int newCapacity = cipherText.capacity() + 1000;
                cipherText =
                        FrameworkTools.enlargeBuffer(cipherText, newCapacity);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                            "enlarged cipherText: " + cipherText);
                }
                return true;

            case OK:
                // wrap() was successfully completed
                SSLEngineResult.HandshakeStatus handshakeStatus =
                        sslEngineResult.getHandshakeStatus();
                for (boolean checkHandshake = true; checkHandshake;) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST,
                                "handshakeStatus: " + handshakeStatus);
                    }
                    switch (handshakeStatus) {
                        case NEED_WRAP:
                            return true;

                        case NEED_UNWRAP:
                            switchToInput = true;
                            return false;

                        case NEED_TASK:
                            for (Runnable runnable =
                                    sslEngine.getDelegatedTask();
                                    runnable != null;) {
                                runnable.run();
                                runnable = sslEngine.getDelegatedTask();
                            }
                            handshakeStatus = sslEngine.getHandshakeStatus();
                            break;

                        case FINISHED:
                            if (handshakeNotifier != null) {
                                handshakeNotifier.fireHandshakeCompleted(
                                        sslEngine.getSession());
                            }
                            // there may already be some cipher text waiting in
                            // the sslInputForwarder...
                            switchToInput = true;
                        case NOT_HANDSHAKING:
                        default:
                            checkHandshake = false;
                    }
                }

                for (ByteBuffer buffer : buffers) {
                    if (buffer.hasRemaining()) {
                        return true;
                    }
                }
                return false;

            case CLOSED:
            default:
                // fall through here
                // INFO: BUFFER_UNDERFLOW can not happen here...
                return false;
        }
    }

    private void cleanUp() throws IOException {

        // detect plain text fill level changes
        int remaining = plainText.remaining();
        bufferSizeListenerHandler.updateLevel(remaining);

        // do we have to continue the handshake at the input forwarder?
        if (switchToInput) {
            switchToInput = false;
            if (sslInputForwarder == null) {
                throw new IOException("sslInputForwarder is not set! " +
                        "Use setSSLInputForwarder(...) directly after " +
                        "creating an SSLOutputForwarder.");
            }
            logger.log(Level.FINEST, "switching to sslInputForwarder");
            sslInputForwarder.continueHandshake();
        }
    }
}
