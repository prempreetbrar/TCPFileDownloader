 /**
 * WebClient Class
 *
 * CPSC 441 - L01 - T01
 * Assignment 2
 * 
 * TA: Amir Shani
 * Student: Prempreet Brar
 * UCID: 30112576
 *
 * This class initiates a TCP connection to a remote server, downloads a requested resource
 * (file), and then writes the file to the local working directory. 
 */

import java.io.*;

import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebClient {
	private static final Logger logger = Logger.getLogger("WebClient"); // global logger

    // The following regex was constructed with help from the website: https://regex101.com/
    // Used for parsing URL.                  protocol     hostname   port   pathname
    private static final String URL_REGEX = "([a-zA-Z]+)://([^:/]+):?(\\d*)?/(\\S+)";
    private static final String STRING_TO_BYTE_CHARSET = "US-ASCII";

    // numbers chosen are arbitrary
    private static final int NO_PORT = 0;
    private static final int NO_BYTE = -1;

    // numbers follow standard Java convention
    private static final int SUCCESSFUL_TERMINATION = 0;
    private static final int UNSUCCESSFUL_TERMINATION = -1;

    /* currently, we lowercase the parsed protocol from the URL; however, we may change
       that implementation in the future, in which case we do not want to have to change
       every single string comparison in our program.
    */
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    // as specified in assignment
    private static final int HTTP_DEFAULT_PORT = 80;
    private static final int HTTPS_DEFAULT_PORT = 443;
    private static final String SUCCESS_STATUS_PHRASE = "OK";
    private static final int SUCCESS_STATUS_CODE = 200;
    
    // url variables
    private String protocol;
    private String hostname;
    private int port;
    private String pathname;
    private String filename;

    // connection variables
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private FileOutputStream fileOutputStream;
    private static final int EOF = -1;
    private static final int OFFSET = 0;
    // arbitrary
    private static final int BUFFER_SIZE = 4096;

    /**
     * Default no-arg constructor
     */
	public WebClient() {
		protocol = null;
        hostname = null;
        port = NO_PORT;
        pathname = null;
        filename = null;

        socket = null;
        inputStream = null;
        outputStream = null;
	}

     /**
     * Close all opened streams, sockets, and other resources before terminating the program.
     *
     * @param resources all resources which need to be closed
     */
    private void closeGracefully(Closeable... resources) {
        /*
         * We need to surround this with a try-catch block because the closing itself can raise
         * an IOException. In this case, if closing fails, there is nothing else we can do. We must also
         * ensure the resource is not null. This is because other parts of the program instantiate certain
         * resources to null before reassignment.
         */
        try {
            for (Closeable resource : resources) {
                if (resource != null) {
                    resource.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the provided URL to obtain the protocol, hostname, port, and pathname.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
    private void parseUrl(String url) {
        Pattern pattern = Pattern.compile(URL_REGEX);
        Matcher matcher = pattern.matcher(url);
        /*
         * The previous line merely created a matcher object; we must invoke the
         * find method to get the matcher to actually search the provided string. If
         * the find succeeds, then we know we have found all of our mandatory capturing
         * groups (protocol, hostname, pathname), and can perform an additional check
         * to see if we also found a port.
         */
        if (matcher.find()) {
            /* 
               Note that group 0 is the entire string; individual capture groups are
               indexed starting from 1. Since protocol and hostname are case-insensitive,
               let's just make them lowercase. 
             */
            protocol = matcher.group(1).toLowerCase();
            hostname = matcher.group(2).toLowerCase();
            String portAsString = matcher.group(3);

            // assign a default port if none is provided
            if (portAsString.isEmpty()) {
                switch (protocol) {
                    case HTTP:
                        port = HTTP_DEFAULT_PORT;
                        break;
                    case HTTPS:
                        port = HTTPS_DEFAULT_PORT;
                        break;
                }
            } else {
                port = Integer.valueOf(portAsString);
            }
            pathname = matcher.group(4);
            // the file is always the last parth of the pathname
            String[] subdirectories = pathname.split("/");
            filename = subdirectories[subdirectories.length - 1];
        }
    }

    /**
     * Establishes a connection with a host at a specific port number. Assumes the
     * hostname and port have been defined by some other part of the program.
     * @throws IOException 
     * @throws UnknownHostException 
     */  
    private void establishConnection() throws UnknownHostException, IOException {
        switch (protocol) {
            case HTTP:
                socket = new Socket(hostname, port);
                break;
            // if the protocol is HTTPS, we need a Secure Socket Layer (SSL) Socket.
            case HTTPS:
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = (SSLSocket) factory.createSocket(hostname, port);
                break;
        }
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    /**
     * Create a properly formatted HTTP GET request message.
     *
     * @return A request message that is ready to send to the server.
     */
    private String constructGetRequest() {
        String httpMethod = "GET";
        String httpVersion = "HTTP/1.1";
        String hostHeader = "Host: " + hostname + "\r\n";
        String connectionHeader = "Connection: close\r\n";

        /*
         * A request message has these three "components"; this is why the code is broken up
         * in a similar manner, but these could just as easily be combined into a single string.
         */
        String requestLine = String.format("%s /%s %s\r\n", httpMethod, pathname, httpVersion);
        String headerLines = hostHeader + connectionHeader;
        String endOfHeaderLines = "\r\n";
        String request = requestLine + headerLines + endOfHeaderLines;
        
        return request;
    }

    /**
     * Send a properly formatted HTTP GET request message.
     * 
     * @param getRequest Properly formatted GET request.
     * @throws IOException 
     */
    private void sendGetRequest(String getRequest) throws IOException {
        byte[] getRequestBytes = getRequest.getBytes(STRING_TO_BYTE_CHARSET);
        outputStream.write(getRequestBytes);
            
        /*
        * flush to ensure request is actually written to the socket.
        */
        outputStream.flush();
        System.out.println(getRequest);
    } 

    /**
     * Check if the response was successful and if so, read the payload.
     * 
     * @throws IOException 
     */
    private void readResponse() throws IOException {
        if (wasRequestSuccessful()) {
            readPayload();
        }
    }

    /**
     * Reads the response byte by byte, checking for the status code and
     * status phrase to see if the request was successful, and parsing all the
     * headers from the response. 
     * 
     * @throws IOException 
     * @return A boolean denoting whether or not the request was successful. 
     */
    private boolean wasRequestSuccessful() throws IOException {
        /*
            initially, we have not read any bytes from the response. We need
            both prevByte and currByte because we need to keep track of two
            bytes sequentially (to see if we've encountered \r\n).
        */
        int prevByte = NO_BYTE;
        int currByte = NO_BYTE;
        
        /*  the response status line is formatted as: protocol statusCode statusPhrase
            when we split on the status line, we use an index of 0 and 1 to access the code and phrase
            respectively
         */
        int STATUS_CODE = 1;
        int STATUS_PHRASE = 2;

        /*
         * We haven't yet read the response, so the current line and the response are empty. 
         * In addition, we need a separate boolean for knowing if we are on the firstLine, because this is how
         * we know to check for the status code and phrase. 
         */
        String currLine = "";
        boolean readFirstLine = false;
        String response = "";

        // assume the request was successful unless we find otherwise
        boolean requestWasSuccessful = true;

        while ((currByte = inputStream.read()) != EOF) {
            currLine += (char) currByte;

            /* 
             * Java implicitly converts the char to its ASCII value when
             * comparing with prevByte and currByte. This is why we are able 
             * to make this comparison.
             */
            if (prevByte == '\r' && currByte == '\n') {
                response += currLine;

                /*
                 * Check that both the status code and status phrase are what we expect. 
                 * We need to trim the status_phrase to get rid of extra white space before
                 * or after. We don't need to trim on the status_code since Integer.valueOf
                 * will automatically deal with that white space anyway. 
                 */
                if (!readFirstLine) {
                    String[] statusLine = currLine.split(" ");

                    if (Integer.valueOf(statusLine[STATUS_CODE]) != SUCCESS_STATUS_CODE ||
                        !(statusLine[STATUS_PHRASE].trim().equals(SUCCESS_STATUS_PHRASE))
                    ) {
                        /* 
                            if the request was unsuccessful, we set the boolean to false but
                            do not immediately break out of the loop. This is because we still 
                            need to finish reading all headers.
                        */
                        requestWasSuccessful = false;
                    }
                    readFirstLine = true;
                }
                
                /*
                 * If we've encountered a line consisting solely of \r\n, this means
                 * we've found the end of our header lines. We can exit the loop
                 * as we no longer want to parse the remainder of the response (doing
                 * so would mean we are reading the payload).
                 */
                if (currLine.equals("\r\n")) {
                    break;
                }

                /* 
                 * If you've found the end of a regular header line, then "reset"
                 * it to begin reading the next header line. This is to ensure that
                 * we do not add duplicate or redundant info to our response when
                 * printing to console. 
                 */
                currLine = "";
                prevByte = NO_BYTE;
                currByte = NO_BYTE;
            }

            prevByte = currByte;
        }

        System.out.println(response);
        return requestWasSuccessful;
    }

    /**
     * Reads the payload of the response and creates a local file.
     * 
     * @throws IOException 
     */
    private void readPayload() throws IOException {
        /*
        * The numBytes tells us how many bytes we actually read from the server; this may
        * be different from the buffer size (ie. if the number of bytes remaining is <
        * buffer.length). This is why we cannot specify buffer.length as the number of bytes being written
        * to the file, as we would get an IndexOutOfBounds exception when we reach the end.
        */
        int numBytes = 0;
        byte[] buffer = new byte[BUFFER_SIZE];            
        fileOutputStream = new FileOutputStream(filename);

        while ((numBytes = inputStream.read(buffer)) != EOF) {
            fileOutputStream.write(buffer, OFFSET, numBytes);
        }

        /*
        * we are only going to open the file after the entire file has been written; therefore, we can flush at the end. 
        * There is no urgency when outputting to file.
        */
        fileOutputStream.flush();
    }

    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
	public void getObject(String url) {
        boolean wasSuccessful = true;

        try {
            parseUrl(url);
            establishConnection();
            sendGetRequest(constructGetRequest());
            readResponse();
        } 
        catch (UnknownHostException e) {
            wasSuccessful = false;
            e.printStackTrace();
        } 
        catch (IOException e) {
            wasSuccessful = false;
            e.printStackTrace();
        }
        /*
         * We must always close our resources, regardless of whether or not 
         * the request was successful. We close them in reverse chronological 
         * order.
         */
        finally {
            closeGracefully(
                fileOutputStream,
                outputStream,
                inputStream,
                socket
            );
            if (wasSuccessful) {
                System.exit(SUCCESSFUL_TERMINATION);
            } 
            else {
                System.exit(UNSUCCESSFUL_TERMINATION);
            }
        }
    }

}