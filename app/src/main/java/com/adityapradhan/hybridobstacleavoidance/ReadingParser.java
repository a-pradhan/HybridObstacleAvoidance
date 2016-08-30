package com.adityapradhan.hybridobstacleavoidance;

public class ReadingParser {
    private int startIndex, endIndex;
    private StringBuilder readings;
    private String parsedReading;
    private String partialReading;

    public ReadingParser() {
        readings = new StringBuilder();
        partialReading = "";
    }

    // returns null if a complete set of readings is not available
    public String parseReadings(String readingSet) {
        readings.append(readingSet);
        // find start and end of reading set
        startIndex = readings.indexOf("#");
        endIndex = readings.indexOf("\n");

        if (startIndex != -1) { // start symbol found
            if (endIndex > startIndex) {
                // reading set available
                parsedReading = readings.substring(startIndex + 1, endIndex);
                // delete readings from string builder
                readings.delete(startIndex, endIndex + 1);
                return parsedReading;

            } else {
                // endIndex smaller than startIndex
                if (endIndex == -1) {
                    // newline char not present in current string readings
                    partialReading = readings.substring(startIndex + 1, readings.length());
                    //readings.delete(startIndex,readings.length());
                    return null;
                } else {
                    // newline char from previous set of readings at start of
                    // StringBuilder
                    String endOfReading = readings.substring(endIndex + 1, startIndex);
                    parsedReading = partialReading + endOfReading;
                    //readings.delete(endIndex,startIndex);
                    return parsedReading;
                }

            }

        } else {

            return null;

        }
    }

    // Getter methods
    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getReadings() {
        return readings.toString();
    }
}
