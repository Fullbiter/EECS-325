import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * represents Threads used by proxyd
 *
 * @author   Kevin Nash (kjn33)
 * @version  2015.11.02
 */
public class ProxyThread extends Thread {
    
    /** the standard port number for web requests **/
    private static final int HTTP_PORT = 80;
    /** time to live for dns cache in seconds **/
    private static final int TIME_TO_LIVE = 30;
    /** 4 Kb buffer prescription **/
    private static final int BUFFER_SIZE = 8192;
    /** magic string for Carriage Return + Line Feed, marks line ends **/
    private static final String CR_LF = "\r\n";
    
    /** Socket used by the client **/
    private Socket clientSocket;
    /** Socket used by the server **/
    private Socket serverSocket;
    
    /**
     * constructs a ProxyThread with on provided socket
     * @param  sourceSocket  Socket for use by the client
     */
    public ProxyThread(Socket sourceSocket) {
        super("sourceThread");
        this.clientSocket = sourceSocket;
        this.serverSocket = null;
    }
    
    
    /**
     * parses, buffers, and sends requests from the client
     *   "        "      "    "   responses from the server
     */
    @Override
    public void run() {
        
        /** the client's request as a byte array **/
        byte[] request;
        /** the server's response as a byte array **/
        byte[] response;
        
        /** stream inbound from the client **/
        BufferedInputStream clientInbound;
        /** stream inbound from the server **/
        BufferedInputStream serverInbound;
        /** stream outbound to the client **/
        BufferedOutputStream clientOutbound;
        /** stream outbound to the server **/
        BufferedOutputStream serverOutbound;
        
        /** the client's request as a streamed buffer **/
        ByteArrayOutputStream requestStream;
        /** the server's response as a streamed buffer **/
        ByteArrayOutputStream responseStream;
        
        /** hostname for the server **/
        String hostname;
        /** address for the server **/
        InetAddress address;
        
        /** the current length of the body in bytes **/
        int bodyLength = 0;        
        
        try {
            clientInbound =
                new BufferedInputStream(clientSocket.getInputStream());
            clientOutbound =
                new BufferedOutputStream(clientSocket.getOutputStream());
            requestStream = new ByteArrayOutputStream();
            
            hostname = parseHeader(clientInbound, requestStream).toString();
            request = requestStream.toByteArray();
            
            // setup the web socket for http requests to the server
            address = InetAddress.getByName(hostname);
            this.serverSocket = new Socket(address.getHostAddress(), HTTP_PORT);
            this.serverSocket.setSoTimeout(TIME_TO_LIVE * 1000);
            
            // setup I/O streams for the server
            serverInbound =
                new BufferedInputStream(serverSocket.getInputStream());
            serverOutbound =
                new BufferedOutputStream(serverSocket.getOutputStream());
            responseStream = new ByteArrayOutputStream();
            
            // write the request to the server outbound stream
            serverOutbound.write(request);
            serverOutbound.flush();
            
            // retrieve the content-length field from the HTTP header
            String contentLength =
                parseHeader(serverInbound, responseStream).toString();
            // check for an empty content-length field
            if (!contentLength.isEmpty())
                bodyLength = Integer.parseInt(contentLength);
            
            // parse the response body
            parseBody(serverInbound, responseStream, bodyLength);
            
            // convert the response stream
            response = responseStream.toByteArray();
            
            // write the request to the client outbound stream
            clientOutbound.write(response);
            clientOutbound.flush();
            
            // close all streams
            requestStream.close();
            responseStream.close();
            clientOutbound.close();
            serverOutbound.close();
            clientInbound.close();
            serverInbound.close();
            
            // close both sockets
            serverSocket.close();
            clientSocket.close(); 
        }
        catch(IOException e) {
            System.out.println("133: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * retreives the content-length field of responses
     * retreives the host field of requests
     * @param   in   InputStream
     * @param   out  OutputStream
     * @return  StringBuffer  parsed version of the target field
     */
    private StringBuffer parseHeader (InputStream in, OutputStream out) {
        /** current line in String form **/        
        String line = "";
        /** array of field strings retrieved from the stream**/
        String[] lineSplits;
        /** buffer to segment into lines with CR_LF **/
        StringBuffer header = new StringBuffer("");
        /** buffer to contain either content-length or host fields **/
        StringBuffer targetField = new StringBuffer("");
        try {
            // loop until we reach the end of the header
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                // format line endings
                header.append(line + CR_LF);
                
                // current line is a content-length field
                if (line.contains("Content-Length: ")) {
                    lineSplits = line.split("Content-Length: ");
                    targetField.append(lineSplits[1]);
                }
                // current line is a host field
                else if (line.contains("Host: ")) {
                    lineSplits = line.split("Host: ");
                    targetField.append(lineSplits[1]);
                }
            }
            // format the final line ending
            header.append(CR_LF);
            // write the header, in byte array form, to out
            out.write(header.toString().getBytes(), 0, header.length());
        }
        catch (IOException e) {
            System.out.println("174: " + e.getMessage());
            e.printStackTrace();
        }
        return targetField;
    }
    
    /**
     * Less like readAllLines and more like readOneLine
     * @param   in      InputStream
     * @return  String  converted type of the provided line
     */
    private String readLine (InputStream in) {
        /** buffer to contain the output line **/
        StringBuffer line = new StringBuffer("");
        /** int form of a byte of data from the InputStream **/
        int byteOut = 0;
        
        try {
            // save first position, if we continue reset to saved position
            in.mark(1);
            if (in.read() == -1)
                return null;
            else
                in.reset();
            
            // loop until we reach the end of the line
            while ((byteOut = in.read()) > 0 && byteOut != '\r' && byteOut != '\n')
                line.append((char) byteOut);
            
            // test if truly end of the line
            if (byteOut == '\r') {
                in.mark(1);
                // test if CRLF
                if (!((byteOut = in.read()) == '\n'))
                    in.reset();
            }
        }
        catch (IOException e) {
            System.out.println("211: " + e.getMessage());
            e.printStackTrace();
        }
        return line.toString();
    }
    
    /**
     * Helper method to parse the body of a response separately from the header.
     * @param in - input stream
     * @param out - output stream, usually a ByteArrayOutputStream
     * @param bodySize - Number of bytes we should be parsing, 0 if content-length field no provided
     */
    private void parseBody (InputStream in, OutputStream out, int bodyLength) {
        /** whether we are given the length of the body **/
        boolean bodyLengthUnknown = (bodyLength > 0) ? false : true;
        /** simple buffer array for reading bytes **/
        byte[] buffer = new byte[BUFFER_SIZE];
        /** number of bytes written **/
        int byteCount = 0;
        /** int form of a byte of data from the InputStream **/
        int byteOut = 0;
        try {
            // loop until we reach the end of the body
            while ((bodyLengthUnknown || byteCount < bodyLength) &&
                   (byteOut = in.read(buffer)) >= 0) {
                out.write(buffer, 0, byteOut);
                byteCount += byteOut;
            }
            out.flush();
        }
        catch (IOException e) {
            System.out.println("241: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
