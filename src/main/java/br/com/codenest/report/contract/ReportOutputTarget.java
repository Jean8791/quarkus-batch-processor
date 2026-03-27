package br.com.codenest.report.contract;

import java.io.IOException;
import java.io.OutputStream;

public interface ReportOutputTarget {
    OutputStream open(String fileName) throws IOException;
}