/*
 * NIOFormatter.java
 *
 * Created on 8. Januar 2006, 18:49
 *
 */

package ch.unifr.nio.framework;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A formatter for NIO log files
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class NIOFormatter extends Formatter {

    private static final NIOFormatter instance = new NIOFormatter();
    private static final String lineSeparator =
            System.getProperty("line.separator");
    private static final SimpleDateFormat timestampFormat =
            new SimpleDateFormat("yyyyMMdd HH:mm:ss:SSS");
    private static final FieldPosition dummyPosition = new FieldPosition(0);
    private static final Date date = new Date();

    /**
     * returns an instance of the NIOFormatter
     * @return an instance of the NIOFormatter
     */
    public static final NIOFormatter getInstance() {
        return instance;
    }

    /**
     * returns the current time as a formatted String
     * @return the current time as a formatted String
     */
    public synchronized static final String getTimeStamp() {
        date.setTime(System.currentTimeMillis());
        return timestampFormat.format(date);
    }

    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public synchronized String format(LogRecord record) {

        StringBuffer stringBuffer = new StringBuffer();

        // date & time
        date.setTime(record.getMillis());
        timestampFormat.format(date, stringBuffer, dummyPosition);
        stringBuffer.append(' ');

        // record source method
        String sourceClassName = record.getSourceClassName();
        if (sourceClassName == null) {
            stringBuffer.append(record.getLoggerName());
        } else {
            stringBuffer.append(sourceClassName);
        }
        String sourceMethodName = record.getSourceMethodName();
        if (sourceMethodName != null) {
            stringBuffer.append('.');
            stringBuffer.append(sourceMethodName);
            stringBuffer.append("()");
        }
        stringBuffer.append(lineSeparator);

        // log level
        stringBuffer.append(record.getLevel().getLocalizedName());
        stringBuffer.append(": ");

        // and finally the message itself
        String message = formatMessage(record);
        stringBuffer.append(message);
        stringBuffer.append(lineSeparator);
        stringBuffer.append(lineSeparator);

        // was there an Exception?
        Throwable throwable = record.getThrown();
        if (throwable != null) {
            try {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                throwable.printStackTrace(printWriter);
                printWriter.close();
                stringBuffer.append(stringWriter.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return stringBuffer.toString();
    }
}
