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

    // Used for parsing URL.                  protocol     hostname   port   pathname
    // The following regex was constructed with help from the website: https://regex101.com/
    private static final String URL_REGEX = "([a-zA-Z]+)://([^:/]+):?(\\d*)?/(\\S+)";
    
    private static final int EOF = -1;
    private static final int SUCCESSFUL_TERMINATION = 0;
    private static final int UNSUCCESSFUL_TERMINATION = -1;
    private static final int NO_PORT = 0;
    private static final String STRING_TO_BYTE_CHARSET = "US-ASCII";

    // currently, we lowercase the parsed protocol from the URL; however, we may change
    // that implementation in the future, in which case we do not want to have to change
    // every single string comparison in our program.
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final int HTTP_DEFAULT_PORT = 80;
    private static final int HTTPS_DEFAULT_PORT = 443;
    
    // url variables
    private String protocol;
    private String hostname;
    private int port;
    private String pathname;

    // connection variables
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * Default no-arg constructor
     */
	public WebClient() {
		protocol = null;
        hostname = null;
        port = NO_PORT;
        pathname = null;

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
            // if the protocol is HTTP, we need a Secure Socket Layer (SSL) Socket.
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
        * flush to ensure request is actually written to the socket; we can also shutdown
        * the output stream as no further requests need to be sent to the server
        */
        outputStream.flush();
        socket.shutdownOutput();
    } 

    
    private void readServerResponse() throws IOException {
        int b;

        while ((b = inputStream.read()) != EOF) {
            System.out.println(inputStream);
        }
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
            readServerResponse();
        } 
        catch (UnknownHostException e) {
            wasSuccessful = false;
            e.printStackTrace();
        } 
        catch (IOException e) {
            wasSuccessful = false;
            e.printStackTrace();
        }
        finally {
            closeGracefully(
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