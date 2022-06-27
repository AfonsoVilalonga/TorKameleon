package com.afonsovilalonga.Proxy.Utils;

import java.io.IOException;
import java.io.InputStream;


/**
 * Auxiliary methods to deal with HTTP requests and replies
 */
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
     * Parses the first line of the HTTP reply and returns an array
     * of three strings: reply[0] = version, reply[1] = number and reply[2] = result message
     * Example: input "HTTP/1.0 501 Not Implemented"
     * output reply[0] = "HTTP/1.0", reply[1] = "501" and reply[2] = "Not Implemented"
     * <p>
     * If the input is malformed, it returns something unpredictable
     */

    public static String[] parseHttpReply(String reply) {
        String[] result = {"", "", ""};
        int pos0 = reply.indexOf(' ');
        if (pos0 == -1) return result;
        result[0] = reply.substring(0, pos0).trim();
        pos0++;
        int pos1 = reply.indexOf(' ', pos0);
        if (pos1 == -1) return result;
        result[1] = reply.substring(pos0, pos1).trim();
        result[2] = reply.substring(pos1 + 1).trim();
        return result;
    }

    public static String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
            return "text/html";
        else
            return "text/plain";
    }
}
