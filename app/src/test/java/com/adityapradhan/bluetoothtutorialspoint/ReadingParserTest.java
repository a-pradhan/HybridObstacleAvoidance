package com.adityapradhan.bluetoothtutorialspoint;

import org.junit.Test;
import java.util.regex.Pattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the reading Parser test
 */
public class ReadingParserTest {


    @Test
    public void readingParser_NoStartChar_ReturnsNull() {
        ReadingParser parser = new ReadingParser();
        String parsedReading = parser.parseReadings("1,2,3\n");
        assertEquals(null, parsedReading);
        assertEquals(-1, parser.getStartIndex());
        assertEquals("1,2,3\n", parser.getReadings());
    }

    @Test
    public void readingParser_NoEndChar_ReturnsNull() {
        ReadingParser parser = new ReadingParser();
        String parsedReading = parser.parseReadings("1,2");
        assertEquals(null, parsedReading);
        assertEquals(-1, parser.getEndIndex());
    }

    @Test
    public void readingParser_FullReading_ReturnsString() {
        ReadingParser parser = new ReadingParser();
        String parsedReading = parser.parseReadings("#100,200,300\n");
        assertEquals("100,200,300", parsedReading);
    }

    @Test
    public void readingParser_JoinReading_ReturnsString() {
        ReadingParser parser = new ReadingParser();
        parser.parseReadings("#1,2,3\n#4,5,");
        // check first set of readings is deleted from StringBuilder
        assertEquals("#4,5,", parser.getReadings());
        String parsedReading = parser.parseReadings("6\n#7,8,9\n");
        assertEquals("4,5,6", parsedReading);
        assertEquals(6, parser.getEndIndex());
    }


}
