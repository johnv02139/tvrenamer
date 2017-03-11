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
        writer.startNode("airDate");
        writer.setValue(localDate.toString());
        writer.endNode();
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context)
    {
        reader.moveDown();
        LocalDate localDate = LocalDate.parse(reader.getValue());
        reader.moveUp();
        return localDate;
    }
}
