package org.tvrenamer.controller;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.time.LocalDate;

public class AirDateConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(LocalDate.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context)
    {
        LocalDate localDate = (LocalDate) value;
        writer.setValue(localDate.toString());
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context)
    {
        LocalDate localDate = LocalDate.parse(reader.getValue());
        return localDate;
    }

    // An entirely different type of conversion.
    public static long localDateToTimestamp(LocalDate ldate) {
        long epochDay = 1 + ldate.toEpochDay();
        return epochDay * 24L * 60L * 60L * 1000L;
    }
}
