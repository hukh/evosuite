package com.examples.with.different.packagename.testcarver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

public class DateConverterTest9 {

    /**
     * Create the Converter with no default value.
     * @return A new Converter
     */
    protected DateTimeConverter makeConverter() {
        return new DateConverter();
    }
    
    /**
     * Create the Converter with a default value.
     * @param defaultValue The default value
     * @return A new Converter
     */
    protected DateTimeConverter makeConverter(Object defaultValue) {
        return new DateConverter(defaultValue);
    }

    /**
     * Return the expected type
     * @return The expected type
     */
    protected Class<?> getExpectedType() {
        return Date.class;
    }

    /**
     * Convert a Date or Calendar objects to the time in millisconds
     * @param date The date or calendar object
     * @return The time in milliseconds
     */
    long getTimeInMillis(Object date) {

        if (date instanceof java.sql.Timestamp) {
            // ---------------------- JDK 1.3 Fix ----------------------
            // N.B. Prior to JDK 1.4 the Timestamp's getTime() method
            //      didn't include the milliseconds. The following code
            //      ensures it works consistently accross JDK versions
            java.sql.Timestamp timestamp = (java.sql.Timestamp)date;
            long timeInMillis = ((timestamp.getTime() / 1000) * 1000);
            timeInMillis += timestamp.getNanos() / 1000000;
            return timeInMillis;
        }

        if (date instanceof Calendar) {
            return ((Calendar)date).getTime().getTime();
        } else {
            return ((Date)date).getTime();
        }
    }
    
    
	  /**
     * Test Conversion to the required type
     * @param converter The converter to use
     * @param expected The expected result
     * @param value The value to convert
     */
    void validConversion(Converter converter, Object expected, Object value) {
        String valueType = (value == null ? "null" : value.getClass().getName());
        String msg = "Converting '" + valueType + "' value '" + value + "'";
        try {
            Object result = converter.convert(getExpectedType(), value);
            Class<?> resultType = (result   == null ? null : result.getClass());
            Class<?> expectType = (expected == null ? null : expected.getClass());
            assertEquals("TYPE "  + msg, expectType, resultType);
            assertEquals("VALUE " + msg, expected, result);
        } catch (Exception ex) {
            fail(msg + " threw " + ex.toString());
        }
    }

    /**
     * Test Conversion to String
     * @param converter The converter to use
     * @param expected The expected result
     * @param value The value to convert
     */
    void stringConversion(Converter converter, String expected, Object value) {
        String valueType = (value == null ? "null" : value.getClass().getName());
        String msg = "Converting '" + valueType + "' value '" + value + "' to String";
        try {
            Object result = converter.convert(String.class, value);
            Class<?> resultType = (result   == null ? null : result.getClass());
            Class<?> expectType = (expected == null ? null : expected.getClass());
            assertEquals("TYPE "  + msg, expectType, resultType);
            assertEquals("VALUE " + msg, expected, result);
        } catch (Exception ex) {
            fail(msg + " threw " + ex.toString());
        }
    }

    /**
     * Test Conversion Error
     * @param converter The converter to use
     * @param value The value to convert
     */
    void invalidConversion(Converter converter, Object value) {
        String valueType = (value == null ? "null" : value.getClass().getName());
        String msg = "Converting '" + valueType + "' value '" + value + "'";
        try {
            Object result = converter.convert(getExpectedType(), value);
            fail(msg + ", expected ConversionException, but result = '" + result + "'"); 
        } catch (ConversionException ex) {
            // Expected Result
        }
    }

    /**
     * Parse a String value to the required type
     * @param value The String value to parse
     * @param pattern The date pattern
     * @param locale The locale to use (or null)
     * @return parsed Calendar value
     */
    Object toType(String value, String pattern, Locale locale) {
        Calendar calendar = toCalendar(value, pattern, locale);
        return toType(calendar);
    }

    /**
     * Parse a String value to a Calendar
     * @param value The String value to parse
     * @param pattern The date pattern
     * @param locale The locale to use (or null)
     * @return parsed Calendar value
     */
    Calendar toCalendar(String value, String pattern, Locale locale) {
        Calendar calendar = null;
        try {
            DateFormat format = (locale == null) 
                           ? new SimpleDateFormat(pattern)
                           : new SimpleDateFormat(pattern, locale);
            format.setLenient(false);
            format.parse(value);
            calendar = format.getCalendar();
        } catch (Exception e) {
            fail("Error creating Calendar value ='"
                    + value + ", pattern='" + pattern + "' " + e.toString());
        }
        return calendar;
    }

    /**
     * Convert a Calendar to a java.util.Date
     * @param calendar The calendar object to convert
     * @return The converted java.util.Date
     */
    Date toDate(Calendar calendar) {
        return calendar.getTime();
    }

    /**
     * Convert a Calendar to a java.sql.Date
     * @param calendar The calendar object to convert
     * @return The converted java.sql.Date
     */
    java.sql.Date toSqlDate(Calendar calendar) {
        return new java.sql.Date(getTimeInMillis(calendar));
    }

    /**
     * Convert a Calendar to a java.sql.Time
     * @param calendar The calendar object to convert
     * @return The converted java.sql.Time
     */
    java.sql.Time toSqlTime(Calendar calendar) {
        return new java.sql.Time(getTimeInMillis(calendar));
    }

    /**
     * Convert a Calendar to a java.sql.Timestamp
     * @param calendar The calendar object to convert
     * @return The converted java.sql.Timestamp
     */
    java.sql.Timestamp toSqlTimestamp(Calendar calendar) {
        return new java.sql.Timestamp(getTimeInMillis(calendar));
    }

    /**
     * Convert from a Calendar to the appropriate Date type
     * 
     * @param value The Calendar value to convert
     * @return The converted value
     */
    protected Object toType(Calendar value) {
        return value.getTime();
    }
    	
    /**
     * Test Converter with multiple patterns
     */
    @Test
    public void testMultiplePatterns() {
        String testString = null;
        Object expected = null;

        // Create & Configure the Converter
        String[] patterns = new String[] {"yyyy-MM-dd", "yyyy/MM/dd"};
        DateTimeConverter converter = makeConverter();
        converter.setPatterns(patterns);

        // First Pattern
        testString = "2006-10-28";
        expected = toType(testString, patterns[0], null);
        validConversion(converter, expected, testString);

        // Second pattern
        testString = "2006/10/18";
        expected = toType(testString, patterns[1], null);
        validConversion(converter, expected, testString);

        // Invalid Conversion
        invalidConversion(converter, "17/03/2006");
        invalidConversion(converter, "17.03.2006");

    }
}