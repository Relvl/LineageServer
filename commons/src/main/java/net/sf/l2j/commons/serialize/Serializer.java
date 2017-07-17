package net.sf.l2j.commons.serialize;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author Johnson / 04.06.2017
 */
public final class Serializer {
    public static final ObjectMapper MAPPER = new XmlMapper();

    static {
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.configure(MapperFeature.AUTO_DETECT_CREATORS, true);
        MAPPER.configure(MapperFeature.AUTO_DETECT_FIELDS, false);
        MAPPER.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
        MAPPER.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
        MAPPER.configure(MapperFeature.AUTO_DETECT_SETTERS, false);
    }

    private Serializer() {}
}
