import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;

/**
 * operates a HTTP proxy
 * 
 * I didn't chose a lowercase class name,
 * it was in the spec!
 *
 * @author   Kevin Nash (kjn33)
 * @version  2015.11.02
 */
public class proxyd {
    
    /** all active threads **/
    private static ArrayList<ProxyThread> threads;
    
    /** student-specific port number **/
    private static int port = 5054;
    
    /**
     * main method
     * @param   args  array of input arguments
     * @throws  IllegalArgumentException
     * @throws  IOException
     * @throws  NumberFormatException
     */
    public static void main(String[] args)
        throws IllegalArgumentException, IOException, NumberFormatException {
        
        // called with "-port" option and provided port number
        if (args.length == 2 && args[0].equals("-port")) {
            port = Integer.parseInt(args[1]);
            System.out.println("Using provided port (" + port + ")");
        }
        // called with unsupported number of arguments
        else if (args.length != 0) {
            throw new IllegalArgumentException();
        }
        // called with no arguments
        else {
            System.out.println("Using default port (" + port + ")");
        }
        threads = new ArrayList<ProxyThread>();
        serve(port);
    }
    
    /**
     * start Threads as needed
     * @param   port  port number on which to start a thread
     * @throws  IOException
     */
    private static void serve(int port) throws IOException {
        // create new socket with provided port
        ServerSocket socket = new ServerSocket(port);
        System.out.println("Thread started on port " + port);
        // start a new thread and keep it running
        while (true) {
            // add each new thread to the list of threads
            threads.add(new ProxyThread(socket.accept()));
            // run the new thread
            threads.get(threads.size() - 1).run();
        }
    }
}
