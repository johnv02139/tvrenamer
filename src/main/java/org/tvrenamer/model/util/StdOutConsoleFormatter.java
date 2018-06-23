package org.tvrenamer.model.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class StdOutConsoleFormatter extends Formatter {

    @Override
    public String format(LogRecord rec) {
        StringBuilder buffer = new StringBuilder(1000);

        // Level
        if (rec.getLevel() == Level.SEVERE) {
            buffer.append("SVRE ");
        } else if (rec.getLevel() == Level.WARNING) {
            buffer.append("WARN ");
        } else if (rec.getLevel() == Level.CONFIG) {
            buffer.append("CNFG ");
        } else if (rec.getLevel() == Level.FINER) {
            buffer.append("FINR ");
        } else if (rec.getLevel() == Level.FINEST) {
            buffer.append("FNST ");
        } else {
            buffer.append(rec.getLevel()).append(" ");
        }

        // Date
        String formatString = "[kk:mm:ss,SSS] ";
        SimpleDateFormat sdf = new SimpleDateFormat(formatString);
        Date date = new Date(rec.getMillis());
        buffer.append(sdf.format(date));

        // Class name (not package), method name
        // buffer.append(rec.getSourceClassName().substring(rec.getSourceClassName().lastIndexOf(".") + 1)).append("#");
        // buffer.append(rec.getSourceMethodName()).append(" ");

        // Message
        buffer.append(rec.getMessage());

        // Stacktrace
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable throwable = rec.getThrown();
        if (throwable != null) {
            StringWriter sink = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sink, true));
            buffer.append("\n").append(sink.toString());
        }

        // Note: No need to add a newline as that is added by the Handler
        return buffer.toString();
    }
}
