# TCP File Downloader

A program that downloads a file from the Internet given a URL over TCP. Watch a gif of me interacting with it below!

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
&nbsp;
![Download](https://github.com/prempreetbrar/VideoConsumptionAndMood/assets/89614923/0a6f2c96-1b3b-449d-b823-91e6d80f8a98)
&nbsp;

2. Open the .pde file. When prompted, keep "VideoConsumptionAndMood-main" as the sketch folder:
&nbsp;
![Open](https://github.com/prempreetbrar/VideoConsumptionAndMood/assets/89614923/12cd7702-7f18-4666-86ee-0bf1fc02d9d2)
&nbsp;

3. Click the play button on the processing panel to start the sketch:
&nbsp;
![Play](https://github.com/prempreetbrar/VideoConsumptionAndMood/assets/89614923/68730272-ca0c-457c-9054-106f87ceb814)
&nbsp;

4. Have fun interacting with the visualization!
&nbsp;
![image](https://github.com/prempreetbrar/VideoConsumptionAndMood/assets/89614923/9ec08893-f401-4513-b5e6-eccb69e4f340)

