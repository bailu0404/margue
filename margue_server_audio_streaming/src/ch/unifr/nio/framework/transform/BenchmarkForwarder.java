/*
 * BenchmarkForwarder.java
 *
 * Created on 27.04.2008, 16:47:31
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
import ch.unifr.nio.framework.FrameworkTools;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A forwarder that generates data as fast as possible.
 * It stops when incomplete write operations occur at the ChannelWriter and
 * resumes when the buffer at the ChannelWriter is empty again.
 * 
 * Optionally, the BenchmarkForwarder can monitor an SSLOutputForwarder and
 * stop when there is remaining plaintext.
 * 
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class BenchmarkForwarder
        extends AbstractForwarder<Void, ByteBuffer>
        implements BufferSizeListener {

    private final static Logger logger =
            Logger.getLogger(BenchmarkForwarder.class.getName());
    private ChannelWriter channelWriter;
    private SSLOutputForwarder sslOutputForwarder;
    private int channelLevel,  sslOutputLevel;
    private boolean noBufferedData = true;
    private boolean keepRunning = true;
    private final ByteBuffer buffer;

    /**
     * Creates a new BenchmarkForwarder
     * @param bufferSize the size of the buffer that will be used for the
     * benchmark
     * @param direct if true, the buffer will be allocated directly, if false,
     * the buffer will be allocated normally
     */
    public BenchmarkForwarder(int bufferSize, boolean direct) {
        if (direct) {
            buffer = ByteBuffer.allocateDirect(bufferSize);
        } else {
            buffer = ByteBuffer.allocate(bufferSize);
        }
    }

    @Override
    public synchronized void forward(Void input) throws IOException {
        // generate data as long as possible
        while (keepRunning && noBufferedData && (nextForwarder != null)) {
            buffer.clear();
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                        "calling nextForwarder.forward(" + buffer + ')');
            }
            nextForwarder.forward(buffer);
        }
    }

    /**
     * stops the forwarder, i.e. no more data is generated and forwarded
     * !!! DO NOT SYNCHRONIZE THIS CALL, forward() HOLDS THE LOCK MOST OF THE
     * TIME!!!
     */
    public void stop() {
        keepRunning = false;
    }

    /**
     * resumes the forwarder, i.e. new data is generated and forwarded
     */
    public void resume() {
        keepRunning = true;
    }

    /**
     * sets the channelWriter to monitor for incomplete write operations
     * @param channelWriter the channelWriter to monitor for incomplete write
     * operations
     */
    public synchronized void setChannelWriter(ChannelWriter channelWriter) {
        if (this.channelWriter != null) {
            // stop monitoring the old ChannelWriter
            this.channelWriter.removeBufferSizeListener(this);
        }
        this.channelWriter = channelWriter;
        channelWriter.addBufferSizeListener(this);
    }

    /**
     * sets the SSLOutputForwarder to monitor for incomplete encryptions
     * @param sslOutputForwarder the SSLOutputForwarder to monitor for
     * incomplete encryptions
     */
    public synchronized void setSSLOutputForwarder(
            SSLOutputForwarder sslOutputForwarder) {
        if (this.sslOutputForwarder != null) {
            // stop monitoring the old SSLOutputForwarder
            this.sslOutputForwarder.removeBufferSizeListener(this);
        }
        this.sslOutputForwarder = sslOutputForwarder;
        sslOutputForwarder.addBufferSizeListener(this);
    }

    @Override
    public synchronized void bufferSizeChanged(Object source, int newLevel) {
        if (source == channelWriter) {
            channelLevel = newLevel;
        } else if (source == sslOutputForwarder) {
            sslOutputLevel = newLevel;
        }
        if ((channelLevel == 0) && (sslOutputLevel == 0)) {
            try {
                logger.log(Level.FINEST, "can continue writing");
                noBufferedData = true;
                forward(null);
            } catch (IOException ex) {
                FrameworkTools.handleStackTrace(logger, ex);
            }
        } else {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "must stop writing:\n\t" +
                        "channelLevel = " + channelLevel + " byte, " +
                        "sslOutputLevel = " + sslOutputLevel + " byte");
            }
            noBufferedData = false;
        }
    }
}
