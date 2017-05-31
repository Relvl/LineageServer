/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.commons.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author G1ta0
 */
public class ExProperties extends Properties {
    public static final String defaultDelimiter = "[\\s,;]+";
    private static final long serialVersionUID = 1L;

    public void load(final String fileName) throws IOException {
        load(new File(fileName));
    }

    public void load(final File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            load(is);
        }
    }

    public boolean getProperty(final String name, final boolean defaultValue) {
        boolean val = defaultValue;

        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            val = Boolean.parseBoolean(value);
        }

        return val;
    }

    public int getProperty(final String name, final int defaultValue) {
        int val = defaultValue;

        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            val = Integer.parseInt(value);
        }

        return val;
    }

    public long getProperty(final String name, final long defaultValue) {
        long val = defaultValue;

        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            val = Long.parseLong(value);
        }

        return val;
    }

    public double getProperty(final String name, final double defaultValue) {
        double val = defaultValue;

        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            val = Double.parseDouble(value);
        }

        return val;
    }

    public String[] getProperty(final String name, final String[] defaultValue) {
        return getProperty(name, defaultValue, defaultDelimiter);
    }

    public String[] getProperty(final String name, final String[] defaultValue, final String delimiter) {
        String[] val = defaultValue;
        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            val = value.split(delimiter);
        }

        return val;
    }

    public boolean[] getProperty(final String name, final boolean[] defaultValue) {
        return getProperty(name, defaultValue, defaultDelimiter);
    }

    public boolean[] getProperty(final String name, final boolean[] defaultValue, final String delimiter) {
        boolean[] val = defaultValue;
        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            final String[] values = value.split(delimiter);
            val = new boolean[values.length];
            for (int i = 0; i < val.length; i++)
                val[i] = Boolean.parseBoolean(values[i]);
        }

        return val;
    }

    public int[] getProperty(final String name, final int[] defaultValue) {
        return getProperty(name, defaultValue, defaultDelimiter);
    }

    public int[] getProperty(final String name, final int[] defaultValue, final String delimiter) {
        int[] val = defaultValue;
        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            final String[] values = value.split(delimiter);
            val = new int[values.length];
            for (int i = 0; i < val.length; i++)
                val[i] = Integer.parseInt(values[i]);
        }

        return val;
    }

    public long[] getProperty(final String name, final long[] defaultValue) {
        return getProperty(name, defaultValue, defaultDelimiter);
    }

    public long[] getProperty(final String name, final long[] defaultValue, final String delimiter) {
        long[] val = defaultValue;
        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            final String[] values = value.split(delimiter);
            val = new long[values.length];
            for (int i = 0; i < val.length; i++)
                val[i] = Long.parseLong(values[i]);
        }

        return val;
    }

    public double[] getProperty(final String name, final double[] defaultValue) {
        return getProperty(name, defaultValue, defaultDelimiter);
    }

    public double[] getProperty(final String name, final double[] defaultValue, final String delimiter) {
        double[] val = defaultValue;
        final String value;

        if ((value = super.getProperty(name, null)) != null) {
            final String[] values = value.split(delimiter);
            val = new double[values.length];
            for (int i = 0; i < val.length; i++)
                val[i] = Double.parseDouble(values[i]);
        }

        return val;
    }
}