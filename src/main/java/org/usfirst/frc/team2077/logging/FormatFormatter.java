package org.usfirst.frc.team2077.logging;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * the first 12 items in a format will be the following log record items
 *
 * <ol>
 *    <li>Level</li>
 *    <li>instant</li>
 *    <li>logger name</li>
 *    <li>message</li>
 *    <li>thrown name</li>
 *    <li>thread id</li>
 *    <li>class name</li>
 *    <li>class method name</li>
 *    <li>millis</li>
 *    <li>resource bundle</li>
 *    <li>resource bundle name</li>
 *    <li>sequence number</li>
 * </ol>
 *
 * utilizes {@link String#format(String, Object...)} internally for the format
 *
 * you'll want to utilize argument indices for most formats. %&lt;argument_index&gt;$[flags][width][.precision]conversion
 */
public class FormatFormatter extends Formatter {

    public static Logger getLogger() {
        // shouldn't do the following normally, but loggers should only be initialized 1 time so the cost isn't terrible
        String name = new Exception().getStackTrace()[1].getFileName().split("\\.")[0];
        Logger log = Logger.getLogger(name);

        Logger logger = log, lastLogger = null;

        while(logger != null && lastLogger != logger) {
            for(Handler handler : logger.getHandlers()) handler.setFormatter(new FormatFormatter());
            lastLogger = logger;
            logger = logger.getParent();
        }

        return log;
    }

    private final String format;
    private final boolean showStackTraces;

    public FormatFormatter() {
        this("[%1$-7s][%3$-15s][%2$tT.%2$tN]: %4$s%n", true);
    }

    public FormatFormatter(String format, boolean showStackTraces) {
        this.format = format;
        this.showStackTraces = showStackTraces;
    }


    @Override
    public String format(LogRecord record) {
        int len = 12;
        if(record.getParameters() != null) len += record.getParameters().length;
        Object[] args = new Object[len];
        args[0] = record.getLevel();
        args[1] = record.getInstant().atZone(ZoneId.systemDefault());
        args[2] = record.getLoggerName();
        args[3] = record.getMessage();
        args[4] = record.getThrown();
        args[5] = record.getThreadID();
        args[6] = record.getSourceClassName();
        args[7] = record.getSourceMethodName();
        args[8] = record.getMillis();
        args[9] = record.getResourceBundle();
        args[10] = record.getResourceBundleName();
        args[11] = record.getSequenceNumber();

        Object[] params = record.getParameters();
        if(params != null) System.arraycopy(params, 0, args, 12, params.length);

        String ret = String.format(format, args);
        if(record.getThrown() != null && showStackTraces) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(out);
            record.getThrown().printStackTrace(stream);
            String newLine = ret.endsWith("\n") ? "\n" : "";
            ret += out + newLine;

        }

        return ret;
    }
}
