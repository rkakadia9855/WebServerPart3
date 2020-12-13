import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.lang.*;
import java.text.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
// need for converting file into string
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Each Client Connection will be managed in a dedicated Thread
public class HTTP3Server implements Runnable {

	// code to be used for cookies; will need to gut orginal server1 code
	// read all lines of input
	public String ReadAllLines(InputStream inputStream) throws IOException {
		char c;
		// string builder allows you to essentially build a string
		// need to import Class StringBuilder to use(under java.lang maybe just use
		// java.lang.* when
		// importing(?))
		// see link for more on StringBuilder and its methods:
		// https://docs.oracle.com/javase/7/docs/api/java/lang/StringBuilder.html
		StringBuilder result = new StringBuilder();
		// System.out.println("reading chars:");
		do {
			c = (char) inputStream.read();
			result.append((char) c);
		} while (inputStream.available() > 0);
		// System.out.println("got string: %s:\n", result.toString());
		return result.toString();
	}

	// check that the date and time are valid
	public boolean isValidDate(String dateStr) {
		LocalDate date = null;
		try {
			// see where myFormatObj is used in AcceptConnections for my thoughts on it and
			// what we'll need to do to use it
			date = LocalDate.parse(dateStr, this.myFormatObj);
		} catch (DateTimeParseException e) {
			// e.printStackTrace();
			System.out.printf("date %s is not a valid date \n", dateStr);
			return false;
		}
		System.out.printf("date %s is valid\n", dateStr);
		return true;
	}

	// main loop that accepts connections from the client; this function calls the
	// other helper
	// methods within it and does what we're looking for with the cookies
	public void run() {
		updateRunningThreads(true);
		String allLines = null;
		HttpRequestParser parsedRequest;
		Hashtable<String, String> headers;
		String cookie, cookieName, cookieVal, cookieDecoded;
		String[] cookieArray;

		boolean keepGoing;
		boolean foundCookie;

		// get html files index.html and index_seen.html and convert to strings
		// assuming html files are in same directory
		Path fileNamei = Path.of("index.html");
		String newUserContentString = null;
		try {
			newUserContentString = Files.readString(fileNamei);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Path fileNameis = Path.of("index_seen.html");
		String oldUserContentString = null;
		try {
			oldUserContentString = Files.readString(fileNameis);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		LocalDateTime myDateObj = LocalDateTime.now();

		// was never shown myFormatObj but it's probably an object created to be used to
		// format the
		// date
		// can create myFormatObj in the manner shown in encoding/decoding example of
		// project
		// description
		// link that explains how the format() method works:
		// https://www.javatpoint.com/java-string-format
		String formattedDate = myDateObj.format(this.myFormatObj);
		// System.out.printf("formatted date+time %s \n", formattedDate);

		// will need to use code from encoding/decoding example in project description
		String encodedDateTime = null;
		try {
			encodedDateTime = URLEncoder.encode(formattedDate, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// System.out.printf("URL encoded date-time %s \n", encodedDateTime);

		try {
			String decodedDateTime = URLDecoder.decode(encodedDateTime, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// System.out.println("URL decoded date-time %s \n", decodedDateTime);

		// get the input and output streams for the socket
		OutputStream output = null;
		try {
			output = this.connect.getOutputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		InputStream input = null;
		try {
			input = this.connect.getInputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// use a printwriter so we can use print statements to the client
		PrintWriter writer = new PrintWriter(output, true);
		// prof. said httprequestparser() will return headers & bodies in a data struct
		// this is not a method that can be imported; it will need to be coded
		// found an example of this at the following link in the second answer:
		// https://stackoverflow.com/questions/13255622/parsing-raw-http-request
		parsedRequest = new HttpRequestParser();

		// get all the lines of input from the client as one long String
		try {
			allLines = this.ReadAllLines(input);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        // parse the request
                    try {
		  // parseRequest is from the HttpParser class in the other java file
		  try{
		  	parsedRequest.parseRequest(allLines);// external parse function
		  }
		  catch (IOException e) {
			System.out.printf("Malformed HTTP request \n");
			writer.flush();
			writer.close();
			return;
		  }
		  catch(Exception e) {
			  System.out.println(e.getMessage());
		  }
      } catch (Exception e) {
        System.out.printf("Malformed HTTP request \n");
        writer.flush();
		writer.close();
		return;
      }
        // find cookies
        // check if cookie name/value matches the name value we are looking for
      foundCookie = false;
      cookieDecoded = "";
        headers = parsedRequest._requestHeaders;// get HTTP headers as a hash table
        Set<String> keys = headers.keySet();
        for (String key : keys) {
          // some debugging code to print the HTTP headers
            System.out.printf("Header value of key: %s: val is %s\n", key, headers.get(key));

            if (key.equals("Cookie")) {
            // get the name and value of the cookies
            // if the name is correct, get the date
                cookie = headers.get(key);
                System.out.printf("Got cookie:%s\n", headers.get(key));
            // need a nested loop to split multiple cookies per line
            // split the variable/value pair within the line

                cookieArray = cookie.split("=");

                if (cookieArray.length < 2) {
                    continue;
                } else {
                    int i = 0;
                    while (i * 2 + 1 < cookieArray.length) {
                        cookieName = cookieArray[i * 2].trim();
                        cookieVal = cookieArray[i * 2 + 1].trim();
                        System.out.printf("got cookie name: %s : val: %s \n", cookieName, cookieVal);

                // try to parse the cookie header
                        if (cookieName.equals("lasttime") == true) {
                  // parse the cookie value
                            try {
                                cookieDecoded = URLDecoder.decode(cookieVal, "UTF-8");
                                System.out.printf("cookie decoded is %s \n", cookieDecoded);
                    // check if date is valid
                                if (isValidDate(cookieDecoded)) {
                                    foundCookie = true;
                                    System.out.printf("found valid date %s\n", cookieDecoded);
                                }
                                // send the response out the socket, choosing which one depending on if we had the cookie
                                // create temporary vaiable to out the header just for this response
                                if (foundCookie == true) {
                                    String oldUserContentString_send = oldUserContentString.replace("%YEAR-%MONTH-%DAY %HOUR-%MINUTE-%SECOND", cookieDecoded);

                                    writer.printf("%s", oldUserContentString_send);
                                } else {

                                    writer.printf("%s", newUserContentString);
                                }

                            } catch (Exception e) {
                                System.out.printf("decoding cookie value failed\n");
                    			foundCookie = false;// not needed, just a reminder
                			}
            			}
            			i++;
        			}

    			}
			}
		}



				// close the socket properly
		writer.flush();
		writer.close();
		updateRunningThreads(false);
	}

  // start of server 1 code needs to be gutted in order to work with code for cookies
  static final File WEB_ROOT = new File(".");
  static final String FILE_NOT_FOUND = "404 Not Found";
  static final String HTTP_NOT_SUPPORTED = "505 HTTP Version Not Supported";
  static final String OK = "200 OK";
  static final String NOT_MOD = "304 Not Modified";
  static final String BAD_REQ = "400 Bad Request";
  static final String FORBIDDEN = "403 Forbidden";
  static final String NOT_ALLOWED = "405 Method Not Allowed";
  static final String REQ_TIMEOUT = "408 Request Timeout";
  static final String LENGTH_REQUIRED = "411 Length Required";
  static final String INSERVERR = "500 Internal Server Error";
  static final String NOTIMP = "501 Not Implemented";
  static final String UNAVAILSERV = "503 Service Unavailable";
  static final int TIMEOUT = 5000;
  long timestart = 0;
  int testNum = 0;
  static int runningThreads = 0;
  DateTimeFormatter myFormatObj=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // verbose mode
  static final boolean verbose = true;

  // Client Connection via Socket Class
  private Socket connect;

  public HTTP3Server(Socket c) {
    connect = c;
  }

  public static synchronized void updateRunningThreads(boolean increase) {
    if (increase)
      runningThreads++;
    else
      runningThreads--;
  }

  private static synchronized int getRunningThreads() {
    return runningThreads;
  }

  public static void main(String[] args) {
    try {

      // get port from command line arg and convert to a useable integer
      int port = Integer.parseInt(args[0]);

      ServerSocket serverConnect = new ServerSocket(port);
      // serverConnect.setSoTimeout(TIMEOUT);
      System.out.println("Server started.\nListening for connections on port : " + port + " ...\n");

      ExecutorService execService = Executors.newCachedThreadPool();
      ThreadPoolExecutor pool = (ThreadPoolExecutor) execService;
      pool.setCorePoolSize(5);
      pool.setMaximumPoolSize(50);
      Thread thread;

      // we listen until user halts server execution
      while (true) {
        Socket socket = serverConnect.accept();
        try {
            socket.setSoTimeout(100000);
        } catch (SocketException e) {
            System.out.println("Failed to set timeout");
            System.out.println(e.getMessage());
        }
        HTTP3Server myServer = new HTTP3Server(serverConnect.accept());

        if (verbose) {
          System.out.println("Connection opened. (" + new Date() + ")");
        }

        if (getRunningThreads() < pool.getMaximumPoolSize()) {
          execService.submit(thread = new Thread(myServer));
        } else {
          DataOutputStream dataOut = new DataOutputStream(myServer.connect.getOutputStream());
          dataOut.writeBytes("HTTP/1.0 " + UNAVAILSERV);
          dataOut.flush();
          dataOut.close();
          myServer.connect.close();
        }
      }

    } catch (IOException e) {
      System.err.println("Server Connection error : " + e.getMessage());
    }
  }

private byte[] readFileData(File file, int fileLength) throws IOException {
    FileInputStream fileIn = null;
    byte[] fileData = new byte[fileLength];

    try {
        fileIn = new FileInputStream(file);
        fileIn.read(fileData);
    } finally {
        if (fileIn != null) 
            fileIn.close();
    }

    return fileData;
}

  // return supported MIME Types
private String getContentType(String fileRequested) {
    if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
        return "text/html";
    else if(fileRequested.endsWith(".txt"))
        return "text/plain";
    else if(fileRequested.endsWith(".gif"))
        return "image/gif";
    else if(fileRequested.endsWith(".jpeg") || fileRequested.endsWith(".jpg"))
        return "image/jpeg";
    else if(fileRequested.endsWith(".png"))
        return "image/png";
    else if(fileRequested.endsWith(".pdf"))
        return "application/pdf";
    else if(fileRequested.endsWith(".zip"))
        return "application/zip";
    else if(fileRequested.endsWith(".gz"))
        return "application/gzip";
    else 
        return "application/octet-stream";

}

private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
    File file = new File(WEB_ROOT, FILE_NOT_FOUND);
    int fileLength = (int) file.length();
    String content = "text/html";
    byte[] fileData = readFileData(file, fileLength);

    out.println("HTTP/1.1 404 File Not Found");
    out.println("Server: HTTP3Server");
    out.println("Date: " + new Date());
    out.println("Last Modified: n/a");
    out.println("Content-type: " + content);
    out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        
        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }

}
