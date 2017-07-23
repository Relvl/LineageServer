package net.sf.l2j.commons.serialize.converter;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.Optional;

/**
 * @author Johnson / 19.07.2017
 */
public class BooleanFalseOnlyConverter extends StdConverter<Boolean, Boolean> {
    @Override
    public Boolean convert(Boolean value) {
        return Optional.ofNullable(value).filter(bo -> !bo).orElse(null);
    }
}
