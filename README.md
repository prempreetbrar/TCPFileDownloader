# TCP File Downloader

A program that downloads a file from the Internet given a URL over TCP. Watch a gif of me interacting with it below!
&nbsp;
![photo](https://github.com/prempreetbrar/TCPFileDownloader/assets/89614923/d2f1487f-fdbe-4837-9ac3-bb249496ef39)
&nbsp;

## Features
- Supports both `HTTP` and `HTTPS` URLs; if the protocol is `HTTP`, a regular `Socket` is used to establish a TCP connection; if the protocol is `HTTPS`,
  a secure connection is established with the server using an `SSLSocket`. 
- Operates in non-persistent HTTP mode; once the object is downloaded, the client closes the underlying TCP connection.
- URLs must be properly formatted with the following syntax: `protocol://hostname[:port]/pathname`, where `protocol` is either HTTP or HTTPS,
  `hostname` is the name of the web server, `[:port]` is an optional part which specifies the server port (default is 80 for HTTP and 443 for HTTPS),
  `pathname` is the path of the requested object on the specified host.
- Reads the server response line by line to first identify the end of the header fields (by looking for `\r\n`), and then reads the
  remainder of the server's response (the file itself) chunk-by-chunk using a buffer for improved performance. 

## If you want to start up the project on your local machine:
1. Download the code as a ZIP:
<br />
<br />
&nbsp;
![download](https://github.com/prempreetbrar/TCPFileDownloader/assets/89614923/9fb50273-25c4-45e9-8f30-a388c3cd38cb)
&nbsp;

3. Unzip the code:
&nbsp;
![file](https://github.com/prempreetbrar/TCPFileDownloader/assets/89614923/15238822-a37f-43e0-89f5-709719d7f99b)
&nbsp;

4. Open the folder in an IDE, such as VSCode:
&nbsp;
![code](https://github.com/prempreetbrar/TCPFileDownloader/assets/89614923/8eeffe74-0bb9-49f9-8e5f-45dbc5af300a)
&nbsp;

5. Obtain the URL of a file from the internet, copy it. Then, run the code by doing
   ```javac *.java
      java ClientDriver -u <url_copied_from_internet>
   ```
   &nbsp;
  ![photo](https://github.com/prempreetbrar/TCPFileDownloader/assets/89614923/c03f34ad-558d-4665-af01-2a1f7befaa20)
&nbsp;

6. Interact with the file as you please!
&nbsp;
![enjoy](https://github.com/prempreetbrar/TCPFileDownloader/assets/89614923/7641c504-2615-41f5-97c1-bf97e5fbaf86)

