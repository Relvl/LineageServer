package net.sf.l2j.commons.serialize.converter;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.Optional;

/**
 * @author Johnson / 23.07.2017
 */
public class IntegerNot100OnlyConverter extends StdConverter<Integer, Integer> {
    @Override
    public Integer convert(Integer value) { return Optional.ofNullable(value).filter(bo -> bo != 100).orElse(null); }
}
