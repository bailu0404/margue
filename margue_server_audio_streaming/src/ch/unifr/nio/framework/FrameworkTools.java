/*
 * Tools.java
 *
 * Created on 1. Oktober 2006, 11:43
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

package ch.unifr.nio.framework;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tools for the NIO framework
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public final class FrameworkTools {

    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    private static final ResourceBundle STRINGS =
            ResourceBundle.getBundle("ch/unifr/nio/framework/Strings");

    // enforce noninstantiability
    private FrameworkTools() {
    }

    /**
     * Splits a ByteBuffer into head an tail by duplicating the ByteBuffer and
     * setting the limit of the duplicate and the position of the ByteBuffer to
     * the correct positions.
     * @param byteBuffer the ByteBuffer to split (this will be the tail when the
     * function returns)
     * @param size the size of the head
     * @return the head of the splitted ByteBuffer
     */
    public static ByteBuffer splitBuffer(ByteBuffer byteBuffer, int size) {
        int newPosition = byteBuffer.position() + size;
        ByteBuffer head = byteBuffer.duplicate();
        head.limit(newPosition);
        byteBuffer.position(newPosition);
        return head;
    }

    /**
     * Copies a given ByteBuffer that is currently in "drain" mode, i.e. user
     * data is in range including [position() ... limit() - 1].
     * @param input the given ByteBuffer that is currently in "drain" mode, i.e.
     * user data is in range including [position() ... limit() - 1].
     * @return a copy of the given ByteBuffer
     */
    public static ByteBuffer copyBuffer(ByteBuffer input) {
        return copyBuffer(input, input.remaining());
    }

    /**
     * Copies a given ByteBuffer that is currently in "drain" mode, i.e. user
     * data is in range including [position() ... limit() - 1].
     * @param input the given ByteBuffer that is currently in "drain" mode, i.e.
     * user data is in range including [position() ... limit() - 1].
     * @param size the size of the copy
     * @return a copy of the given ByteBuffer
     */
    public static ByteBuffer copyBuffer(ByteBuffer input, int size) {
        int remaining = input.remaining();
        if (remaining < size) {
            throw new IllegalArgumentException("size (" + size +
                    ") was larger than remaining input (" + remaining + ")");
        }
        // allocate new buffer
        ByteBuffer copy = input.isDirect()
                ? ByteBuffer.allocateDirect(size)
                : ByteBuffer.allocate(size);

        // copy data from input to copy without modifying input
        // (restore position and limit after copying)
        int oldPosition = input.position();
        int oldLimit = input.limit();
        input.limit(oldPosition + size);
        copy.put(input);
        copy.flip();
        input.position(oldPosition);
        input.limit(oldLimit);

        return copy;
    }

    /**
     * outputs the stack trace of an exception
     * @param logger a logger that can be used for output
     * if logger is null, the stacktrace will be just printed to System.out
     * @param throwable a throwable
     */
    public static void handleStackTrace(Logger logger, Throwable throwable) {
        if (logger == null) {
            throwable.printStackTrace();
        } else {
            if (logger.isLoggable(Level.WARNING)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream printStream = new PrintStream(out);
                throwable.printStackTrace(printStream);
                printStream.flush();
                String stackTrace = out.toString();
                logger.warning(stackTrace);
            }
        }
    }

    /**
     * enlarges a given ByteBuffer that is currently in "fill" mode, i.e. user
     * data is in range including [0 ... position() - 1].
     * @param byteBuffer the given ByteBuffer that is currently in "fill" mode,
     * i.e. user data is in range including [0 ... position() - 1].
     * @param newSize the new size
     * @return the enlarged ByteBuffer
     */
    public static ByteBuffer enlargeBuffer(ByteBuffer byteBuffer, int newSize) {
        // allocate new buffer
        ByteBuffer newBuffer = byteBuffer.isDirect()
                ? ByteBuffer.allocateDirect(newSize)
                : ByteBuffer.allocate(newSize);
        // move data from old to new buffer
        byteBuffer.flip();
        newBuffer.put(byteBuffer);
        // leave newBuffer in "fill" mode
        return newBuffer;
    }

    /**
     * Appends the contents of <CODE>sources</CODE> to <CODE>destination</CODE>.
     * All buffers are supposed to be in "drain" mode, i.e. the user data
     * is in range including [position() ... limit() - 1].
     * The return value must always be evaluated because
     * <CODE>destination</CODE> may be enlarged in this method. In this case the
     * parameter reference to <CODE>destination</CODE> is invalid upon return.
     * @return the destination buffer (because of buffer enlargment this
     * reference may be different from the <CODE>destination</CODE> input
     * parameter
     * @param direct <CODE>true</CODE>, if new buffers must be allocated
     * directly, <CODE>false</CODE> otherwise
     * @param destination the destination buffer
     * @param sources the source buffers
     */
    public static ByteBuffer append(
            boolean direct, ByteBuffer destination, ByteBuffer... sources) {
        int remaining = 0;
        for (ByteBuffer source : sources) {
            remaining += source.remaining();
        }

        ByteBuffer returnBuffer = null;
        if (destination == null) {
            // create buffer
            if (direct) {
                returnBuffer = ByteBuffer.allocateDirect(remaining);
            } else {
                returnBuffer = ByteBuffer.allocate(remaining);
            }

        } else {
            // check if there is enough space left in destination
            returnBuffer = destination;
            returnBuffer.compact();
            if (returnBuffer.remaining() < remaining) {
                // we have to enlarge returnBuffer so that both the old and new
                // data fits into the new buffer
                int newCapacity = returnBuffer.position() + remaining;
                returnBuffer = enlargeBuffer(returnBuffer, newCapacity);
            }
        }

        for (ByteBuffer source : sources) {
            returnBuffer.put(source);
        }

        returnBuffer.flip();
        return returnBuffer;
    }

    /**
     * returns the string representation of a given bandwidth
     * @param bandwidth the bandwidth in byte/s
     * @param fractionDigits the number of fraction digits to display
     * @return the string representation of a given bandwidth
     */
    public static String getBandwidthString(
            long bandwidth, int fractionDigits) {
        return getDataVolumeString(bandwidth, fractionDigits) + "/s";
    }

    /**
     * returns a string representation of the given data volume
     * (automatically converts to KiB, MiB, GiB and TiB)
     * @param bytes the given data volume
     * @param fractionDigits the number of fraction digits
     * @return a string representation of the given data volume
     */
    public static String getDataVolumeString(long bytes, int fractionDigits) {
        if (bytes >= 1024) {
            numberFormat.setMaximumFractionDigits(fractionDigits);
            float kbytes = (float) bytes / 1024;
            if (kbytes >= 1024) {
                float mbytes = (float) bytes / 1048576;
                if (mbytes >= 1024) {
                    float gbytes = (float) bytes / 1073741824;
                    if (gbytes >= 1024) {
                        float tbytes = (float) bytes / 10995116277760f;
                        return numberFormat.format(tbytes) + " TiB";
                    }
                    return numberFormat.format(gbytes) + " GiB";
                }
                return numberFormat.format(mbytes) + " MiB";
            }
            return numberFormat.format(kbytes) + " KiB";
        }
        return numberFormat.format(bytes) + " " + STRINGS.getString("byte");
    }

    /**
     * converts the given <CODE>byteBuffer</CODE> to a hex string
     * @param byteBuffer the ByteBuffer to convert
     * @return the hex repserentation of <CODE>byteBuffer</CODE>
     */
    public static String toHex(ByteBuffer byteBuffer) {
        int position = byteBuffer.position();
        int remaining = byteBuffer.remaining();
        byte[] data = new byte[remaining];
        byteBuffer.get(data);
        byteBuffer.position(position);
        return toHex(data);
    }

    /**
     * converts a byte array to a hex string
     * @param data the byte array
     * @return the hex repserentation of the byte array
     */
    public static String toHex(byte[] data) {
        return toHex(data, data.length);
    }

    /**
     * converts a byte array to a hex string
     * @return the hex repserentation of the byte array
     * @param dataLength the number of bytes that should be converted to hex
     * @param data the byte array
     */
    public static String toHex(byte[] data, int dataLength) {
        // allocate the right amount of memory
        int hexBlocks = dataLength / 16;
        if ((dataLength % 16) != 0) {
            hexBlocks++;
        }
        // 72 = 16*3 Bytes for every byte, + 7 Bytes whitespaces +
        //      16 bytes plain text + 1 byte newline
        int stringLength = hexBlocks * 72;
        StringBuilder hexString = new StringBuilder(stringLength);

        // start the dirty work
        int blockIndex = 0;
        while (blockIndex < dataLength) {
            // calculate some array pointers
            int blockLength = Math.min(16, dataLength - blockIndex);
            int stopIndex = blockIndex + blockLength;

            // print hex line
            for (int i = blockIndex; i < stopIndex; i++) {
                hexString.append(hexChar[(data[i] & 0xF0) >>> 4]);
                hexString.append(hexChar[data[i] & 0x0F]);
                hexString.append(' ');
            }

            // fill hex line if too short
            for (int i = blockLength; i < 16; i++) {
                hexString.append("   ");
            }

            // space between hex and plain
            hexString.append("       ");

            // print plain line
            for (int i = blockIndex; i < stopIndex; i++) {
                if ((data[i] > 31) && (data[i] < 127)) {
                    // ASCII goes from 32 to 126
                    hexString.append((char) data[i]);
                } else if ((data[i] > -97) && (data[i] < 0)) {
                    // ISO 8859-1 printable starts from 160 ("+96" because of
                    // signed byte values)
                    hexString.append(iso8859_1Char[data[i] + 96]);
                } else {
                    // data[i] is NOT printable, we substitute it with a "."
                    hexString.append('.');
                }
            }

            // append newline
            hexString.append('\n');

            // move index
            blockIndex += blockLength;
        }
        return hexString.toString();
    }
    // table to convert a nibble to a hex char.
    private static char[] hexChar = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    private static char[] iso8859_1Char = {
        ' ', '¡', '¢', '£', '¤', '¥', '¦', '§',
        '¨', '©', 'ª', '«', '¬', '­', '®', '¯',
        '°', '±', '²', '³', '´', 'µ', '¶', '·',
        '¸', '¹', 'º', '»', '¼', '½', '¾', '¿',
        'À', 'Á', 'Â', 'Ã', 'Ä', 'Å', 'Æ', 'Ç',
        'È', 'É', 'Ê', 'Ë', 'Ì', 'Í', 'Î', 'Ï',
        'Ð', 'Ñ', 'Ò', 'Ó', 'Ô', 'Õ', 'Ö', '×',
        'Ø', 'Ù', 'Ú', 'Û', 'Ü', 'Ý', 'Þ', 'ß',
        'à', 'á', 'â', 'ã', 'ä', 'å', 'æ', 'ç',
        'è', 'é', 'ê', 'ë', 'ì', 'í', 'î', 'ï',
        'ð', 'ñ', 'ò', 'ó', 'ô', 'õ', 'ö', '÷',
        'ø', 'ù', 'ú', 'û', 'ü', 'ý', 'þ', 'ÿ'
    };
}
