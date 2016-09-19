package com.adityapradhan.hybridobstacleavoidance;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Aditya on 9/10/2016.
 * Class for logging sensor and filtered results to CSV
 */
public class CSVExport {
    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    String fileName = "AnalysisData.csv";
    String filePath = baseDir + File.separator + fileName;
    File f = new File(filePath);
    private CSVWriter writer;


    public CSVExport() {
    }

    public void export() {

        if (f.exists() && !f.isDirectory()) {
            FileWriter mFileWriter = null;
            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        } else {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String[] data = {"1", "2", "3"};

        writer.writeNext(data);

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}



