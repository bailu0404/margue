/*
 * SplitStringForwarder.java
 *
 * Created on 04.10.2008, 15:56:45
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A forwarder that can split incoming strings with a given delimiter
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class SplitStringForwarder
        extends AbstractForwarder<String, String> {

    private final static Logger logger =
            Logger.getLogger(SplitStringForwarder.class.getName());
    private String buffer;
    private String delimiter;
    private int delimiterLength;

    /**
     * creates a new SplitStringForwarder
     * @param delimiter the delimiter for splitting strings
     */
    public SplitStringForwarder(String delimiter) {
        setDelimiterPrivate(delimiter);
    }

    @Override
    public synchronized void forward(String input) throws IOException {
        if (nextForwarder == null) {
            logger.log(Level.SEVERE, "nextForwarder == null -> data lost");

        } else {
            if ((buffer != null) && (buffer.length() > 0)) {
                splitString(buffer + input);
            } else {
                splitString(input);
            }
        }
    }

    /**
     * sets the delimiter
     * @param delimiter the delimiter for splitting strings
     */
    public synchronized void setDelimiter(String delimiter) {
        setDelimiterPrivate(delimiter);
    }
    
    private void setDelimiterPrivate(String delimiter) {
        this.delimiter = delimiter;
        delimiterLength = delimiter.length();
    }

    private void splitString(String string) throws IOException {
        int offset = 0;
        for (int delimiterIndex = string.indexOf(delimiter, offset);
                delimiterIndex != -1;
                delimiterIndex = string.indexOf(delimiter, offset)) {
            String token = string.substring(offset, delimiterIndex);
            offset = delimiterIndex + delimiterLength;
            nextForwarder.forward(token);
        }
        buffer = string.substring(offset);
    }
}
