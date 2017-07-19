package net.sf.l2j.commons.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;

/**
 * @author Johnson / 19.07.2017
 */
public class BooleanAsAttribute extends JsonSerializer<Boolean> {
    @Override
    public void serialize(Boolean value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        serializers.setAttribute("val", value);
        gen.writeEndObject();
    }

    @Override
    public JsonSerializer<Boolean> unwrappingSerializer(NameTransformer unwrapper) {
        return super.unwrappingSerializer(unwrapper);
    }

}
