package net.sf.l2j.commons.serialize;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.Optional;

/**
 * @author Johnson / 19.07.2017
 */
public class JsonBooleanFalseOnlyFilter extends StdConverter<Boolean, Boolean> {
    @Override
    public Boolean convert(Boolean value) {
        return Optional.ofNullable(value).filter(bo -> !bo).orElse(null);
    }
}
