package com.afonsovilalonga.Utils;

import java.io.IOException;
import java.io.InputStream;

public class Http {

    /**
     * Reads one line from a HTTP header
     */
    public static String readLine(InputStream is) throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = is.read()) >= 0) {
            if (c == '\r') continue;
            if (c == '\n') break;
            sb.append((char) c);
        }
        return sb.toString();
    }


    /**
     * Parses the first line of the HTTP request and returns an array
     * of three strings: reply[0] = method, reply[1] = object and reply[2] = version
     * Example: input "GET index.html HTTP/1.0"
     * output reply[0] = "GET", reply[1] = "index.html" and reply[2] = "HTTP/1.0"
     * <p>
     * If the input is malformed, it returns something unpredictable
     */
    public static String[] parseHttpRequest(String request) {
        String[] error = {"ERROR", "", ""};
        String[] result = {"", "", ""};
        int pos0 = request.indexOf(' ');
        if (pos0 == -1) return error;
        result[0] = request.substring(0, pos0).trim();
        pos0++;
        int pos1 = request.indexOf(' ', pos0);
        if (pos1 == -1) return error;
        result[1] = request.substring(pos0, pos1).trim();
        result[2] = request.substring(pos1 + 1).trim();
        if (!result[1].startsWith("/")) return error;
        if (!result[2].startsWith("HTTP")) return error;
        return result;
    }

    /**
     * Parses an HTTP header returning an array with the name of the attribute header
     * in position 0 and its value in position 1
     * Example, for "Connection: Keep-alive", returns:
     * [0]->"Connection"; [1]->"Keep-alive"
     * <p>
     * If the input is malformed, it returns something unpredictable
     */
    public static String[] parseHttpHeader(String header) {
        String[] result = {"ERROR", ""};
        int pos0 = header.indexOf(':');
        if (pos0 == -1)
            return result;
        result[0] = header.substring(0, pos0).trim();
        result[1] = header.substring(pos0 + 1).trim();
        return result;
    }
}
