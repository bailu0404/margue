/*
 * SSLInputForwarder.java
 *
 * Created on 03.03.2008, 18:03:02
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

import ch.unifr.nio.framework.FrameworkTools;
import ch.unifr.nio.framework.ssl.HandshakeNotifier;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

/**
 * A forwarder that decrypts inbound SSL traffic (ciphertext -> plaintext).
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class SSLInputForwarder
        extends AbstractForwarder<ByteBuffer, ByteBuffer> {

    private final static Logger logger =
            Logger.getLogger(SSLInputForwarder.class.getName());
    private SSLOutputForwarder sslOutputForwarder;
    private final SSLEngine sslEngine;
    private final AtomicLong plainTextCounter;
    private ByteBuffer cipherText,  plainText;
    private boolean switchToOutput;
    /**
     * The HandshakeNotifier is an utility class shared by an
     * SSLInputForwarder and its SSLOutputForwarder. It forwards "handshake
     * completed" events to a list of registered listeners.
     */
    private HandshakeNotifier handshakeNotifier;

    /**
     * Creates a new instance of SSLInputForwarder
     * @param sslEngine the given {@link javax.net.ssl.SSLEngine SSLEngine} used
     * for decryption of inbound data
     */
    public SSLInputForwarder(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
        /**
         * SSLEngine.unwrap() is really dumb regarding buffer sizes and returns
         * BUFFER_OVERFLOW much too soon. Therefore we add some additional
         * buffer space so that reallocating buffers is not needed so early.
         */
        SSLSession session = sslEngine.getSession();
        int plainTextBufferSize = session.getApplicationBufferSize() + 1500;
        plainText = ByteBuffer.allocate(plainTextBufferSize);
        plainText.flip();

        // !!! IMPORTANT !!!
        // cipherText must not be null, because otherwise we will get an
        // exception during handshake
        cipherText = ByteBuffer.allocate(0);

        plainTextCounter = new AtomicLong();
    }

    /**
     * returns the plaintext ByteBuffer
     * @return the plaintext ByteBuffer
     */
    public ByteBuffer getPlainText() {
        return plainText;
    }

    /**
     * sets the corresponding SSLOutputForwarder
     * @param sslOutputForwarder the corresponding SSLOutputForwarder
     */
    public void setSSLOutputForwarder(
            SSLOutputForwarder sslOutputForwarder) {
        this.sslOutputForwarder = sslOutputForwarder;
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
    public synchronized void forward(ByteBuffer input) throws IOException {

        if (cipherText.hasRemaining()) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,
                        "there is already buffered cipherText: " + cipherText);
            }
            // there is already cipherText we could not decrypt
            // append the new input to cipherText
            cipherText = FrameworkTools.append(false, cipherText, input);
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,
                        "cipherText after appending input: " + cipherText);
            }
            // try decrypting the enlarged cipherText
            decrypt(cipherText);

        } else {
            // try direct decryption of input buffer without copying the data
            // over to the cipherText buffer
            decrypt(input);

            // add remaining input to ciphertext
            if (input.hasRemaining()) {
                cipherText = FrameworkTools.append(false, cipherText, input);
            }
        }

    // TODO: inform buffer size listeners about changes in ciphertext and
    // plaintext buffer
    }

    /**
     * returns the amount of produced plain text in byte
     * @return the amount of produced plain text in byte
     */
    public long getPlainTextCounter() {
        return plainTextCounter.get();
    }

    /**
     * resets the plain text counter back to zero and returns the amount of
     * produced plain text in byte
     * @return the amount of produced plain text in byte
     */
    public long getAndResetPlainTextCounter() {
        return plainTextCounter.getAndSet(0);
    }

    synchronized void continueHandshake() throws IOException {
        decrypt(cipherText);
    }

    private void decrypt(ByteBuffer input) throws IOException {
        // decrypt input
        int remainingBeforeUnwrap = plainText.remaining();
        plainText.compact();
        for (SSLEngineResult sslEngineResult =
                sslEngine.unwrap(input, plainText);
                continueUnwrapping(sslEngineResult, input);
                sslEngineResult = sslEngine.unwrap(input, plainText)) {
        }
        plainText.flip();
        int remainingAfterUnwrap = plainText.remaining();
        int producedPlainText = remainingAfterUnwrap - remainingBeforeUnwrap;
        if (logger.isLoggable(Level.FINEST)) {
            if (producedPlainText == 0) {
                logger.log(Level.FINEST, "did not produce any plain text");
            } else {
                logger.log(Level.FINEST, "produced plain text: " +
                        producedPlainText + " byte");
            }
        }

        if (switchToOutput) {
            switchToOutput = false;
            if (sslOutputForwarder == null) {
                throw new SSLException("sslOutputForwarder is not set! " +
                        "Use setSSLOutputForwarder(...) directly after " +
                        "creating an SSLInputForwarder.");
            }
            logger.log(Level.FINEST, "switching to sslOutputForwarder");
            sslOutputForwarder.drain();
        }
        plainTextCounter.addAndGet(producedPlainText);

        // forward plain text
        if (remainingAfterUnwrap > 0) {
            if (nextForwarder == null) {
                logger.log(Level.SEVERE, "no nextForwarder => data lost!");
            } else {
                nextForwarder.forward(plainText);
            }
        }
    }

    /**
     * checks, if unwrap() may continue
     * @param sslEngineResult the current SSLEngineResult
     * @return <CODE>true</CODE>, if unwrap() can continue,
     * <CODE>false</CODE> otherwise
     */
    @SuppressWarnings("fallthrough")
    private boolean continueUnwrapping(
            SSLEngineResult sslEngineResult, ByteBuffer inputBuffer) {

        SSLEngineResult.Status status = sslEngineResult.getStatus();
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "status: " + status);
        }

        switch (status) {
            case BUFFER_OVERFLOW:
                // we just enlarge the plainText Buffer and retry unwrapping
                int newCapacity = plainText.capacity() + 1500;
                plainText = FrameworkTools.enlargeBuffer(
                        plainText, newCapacity);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(
                            Level.FINEST, "enlarged plainText: " + plainText);
                }
                return true;

            case OK:
                // unwrap() was successfully completed
                SSLEngineResult.HandshakeStatus handshakeStatus =
                        sslEngineResult.getHandshakeStatus();
                for (boolean checkHandshake = true; checkHandshake;) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST,
                                "handshakeStatus: " + handshakeStatus);
                    }
                    switch (handshakeStatus) {
                        case NEED_UNWRAP:
                            return true;

                        case NEED_WRAP:
                            switchToOutput = true;
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
                            // there may already be some plain text waiting in
                            // the sslOutputForwarder
                            switchToOutput = true;
                        case NOT_HANDSHAKING:
                        default:
                            checkHandshake = false;
                    }
                }
                return inputBuffer.hasRemaining();

            case CLOSED:
                // inbound direction of sslEngine is closed now
                // TODO: someone needs to be notified of this...
                return false;

            case BUFFER_UNDERFLOW:  // we need more data from our peer
                if (logger.isLoggable(Level.FINEST)) {
                    if (inputBuffer.hasRemaining()) {
                        logger.log(Level.FINEST, "inputBuffer: " +
                                FrameworkTools.toHex(inputBuffer));
                    } else {
                        logger.log(Level.FINEST, "inputBuffer is empty");
                    }
                }
            default:
                return false;
        }
    }
}
