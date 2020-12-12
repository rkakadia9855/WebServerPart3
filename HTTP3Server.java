import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.lang.*;
import java.text.*;
import java.time.*;

// need for converting file into string
import java.nio.file.Files;
import java.nio.file.Paths;


// Each Client Connection will be managed in a dedicated Thread
public class HTTP3Server implements Runnable {

  // code to be used for cookies; will need to gut orginal server1 code
  // read all lines of input
  public String ReadAllLines(InputStream inputStream) throws IOException {
    char c;
    // string builder allows you to essentially build a string
    // need to import Class StringBuilder to use(under java.lang maybe just use java.lang.* when
    // importing(?))
    // see link for more on StringBuilder and its methods:
    // https://docs.oracle.com/javase/7/docs/api/java/lang/StringBuilder.html
    StringBuilder result = new StringBuilder();
    // System.out.println("reading chars:");
    do {
      c = (char) inputStream.read();
      result.append((char) c);
    } while (inputStream.available > 0);
    // System.out.println("got string: %s:\n", result.toString());
    return result.toString();
  }

  // check that the date and time are valid
  public boolean isValidDate(String dateStr){
		LocalDate date=null;
		try{
    //see where myFormatObj is used in AcceptConnections for my thoughts on it and what we'll need to do to use it
			date=LocalDate.parse(dateStr, this.myFormatObj);
		}catch(DateTimeParseException e){
    //e.printStackTrace();
			System.out.printf("date %s is not a valid date \n", dateStr);
			return false:
		}
		System.out.printf("date %s is valid\n", dateStr);
		return true;
	}

  // main loop that accepts connections from the client; this function calls the other helper
  // methods within it and does what we're looking for with the cookies
  int AcceptConnections() {

    String allLines;
    HttpRequestParser parsedRequest;
    Hashtable<String, String> headers;
    String cookie, cookieName, cookieVal, cookieDecoded;
    String[] cookieArray;

    boolean keepGoing;
    boolean foundCookie;

    try (ServerSocket serverSocket = new ServerSocket(port)) {

      System.out.println("Server is listening on port " + port);
      keepGoing = true;
      while (keepGoing == true) {
        Socket socket = ServerSocket.accept();
        try {
          socket.setSoTimeout(100000);
        } catch (SocketException e) {
          System.out.println("Failed to set timeout");
          System.out.println(e.getMessage());
          return -1;
        }
        // some debugging output
        System.out.println("New client connected");
        LocalDateTime myDateObj = LocalDateTime.now();
        // was never shown myFormatObj but it's probably an object created to be used to format the
        // date
        // can create myFormatObj in the manner shown in encoding/decoding example of project
        // description
        // link that explains how the format() method works:
        // https://www.javatpoint.com/java-string-format
        String formattedDate = myDateObj.format(this.myFormatObj);
        // System.out.printf("formatted date+time %s \n", formattedDate);

        // will need to use code from encoding/decoding example in project description
        String encodedDateTime = URLEncoder.encode(formattedDate, "UTF-8");
        // System.out.printf("URL encoded date-time %s \n", encodedDateTime);

        String decodedDateTime = URLDecoder.decode(encodedDateTime, "UTF-8");
        // System.out.println("URL decoded date-time %s \n", decodedDateTime);

        // get the input and output streams for the socket
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        // use a printwriter so we can use print statements to the client
        PrintWriter writer = new PrintWriter(output, true);
        // prof. said httprequestparser() will return headers & bodies in a data struct
        // this is not a method that can be imported; it will need to be coded
        // found an example of this at the following link in the second answer:
        // https://stackoverflow.com/questions/13255622/parsing-raw-http-request
        parsedRequest = new HttpRequestParser();

        // get all the lines of input from the client as one long String
        allLines = this.ReadAllLines(input);

        // parse the request
        try {
          // parseRequest is from the HttpParser class in the other java file
          parsedRequest.parseRequest(allLines);// external parse function
        } catch (IOException e) {
          System.out.printf("Malformed HTTP request \n");
          writer.flush();
          writer.close();
          continue;// go back to the top of the main accept loop
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

        // send the response out the socket, choosing which one depending on if we had the cookie
        // create temporary vaiable to out the header just for this response

        String headerOutputString_send = headerOutputString.replace("%LASTTIME%", encodedDateTime);
        if (foundCookie == true) {

          headerOutputString_send = headerOutputString_send.replace("%CONTENTLEN%",
              String.valueOf(oldUserContentString.length()));
          String oldUserContentString_send =
              oldUserContentString.replace("%LASTVISIT%", cookieDecoded);
          writer.printf("%s", headerOutputString_send);
          writer.printf("\r\n");
          writer.printf("%s", oldUserContentString_send);
        } else {
          headerOutputString_send = headerOutputString_send.replace("%CONTENTLEN%",
              String.valueOf(newUserContentString.length()));
          writer.printf("%s", headerOutputString_send);
          writer.printf("\r\n");
          writer.printf("%s", newUserContentString);
        }

        // close the socket properly
        writer.flush();
        writer.close();
        socket.close();
      }
    } catch (IOException ex) {
      System.out.println("I?O error: " + ex.getMessage());
    }
    return 1;
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

  // verbose mode
  static final boolean verbose = true;

  // Client Connection via Socket Class
  private Socket connect;

public HTTP3Server(Socket c) {
	connect = c;
}

public static synchronized void updateRunningThreads(boolean increase) {
	if(increase)
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

@Override
public void run() {
	updateRunningThreads(true);
		// we manage our particular client connection
	BufferedReader in = null; PrintWriter out = null; DataOutputStream dataOut = null;
	String fileRequested = null;

	try {

		timestart = System.currentTimeMillis();
			//connect.setSoTimeout(TIMEOUT);

			// we read characters from the client via input stream on the socket
		in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
		out = new PrintWriter(connect.getOutputStream());
			// create outputstream to send data to client
		dataOut = new DataOutputStream(connect.getOutputStream());
		boolean contentLengthPresent = false;
		boolean contentTypePresent = false;

			// get first line of the request from the client
		String input = in.readLine();
		if (input == null) {
			out.println("no client request");
			dataOut.write(("HTTP/1.0 " + REQ_TIMEOUT+"\r\n").getBytes());
			dataOut.flush();
		}
		else {
				// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
				//String method = parse.nextToken().toUpperCase(); 
				String method = parse.nextToken();// we get the HTTP method of the client
				System.out.println("Test number: "+testNum);
	 			// we get file requested
				fileRequested = parse.nextToken().toLowerCase();
				//get HTTP version. Also check if the token exists
				String httpver = "";
				boolean checkModified = false;
				System.out.println("Num tokens: "+parse.countTokens()); // Remove later
				if(parse.hasMoreTokens())
					httpver = parse.nextToken();
				// read the next line in client's request
				String anotherLine = in.readLine();
				boolean firsttime = true;
				String encodedCookieVal;
				if(anotherLine != null) {
					parse = new StringTokenizer(anotherLine);
					System.out.println("another line read.");
				}
				String checkModifiedDateStringVal = "";
				if(parse.hasMoreTokens() && parse.nextToken().equals("If-Modified-Since:")) {
					checkModified = true;
					System.out.println("checked modified set to true on line 103");
					while(parse.hasMoreTokens()){
						checkModifiedDateStringVal = checkModifiedDateStringVal + parse.nextToken() + " ";
					}
				}
				else {
					parse = new StringTokenizer(anotherLine);
					if(parse.hasMoreTokens() && parse.nextToken().equals("Cookie:")) {
						firsttime = false;
						if(parse.hasMoreTokens()) 
							encodedCookieVal = parse.nextToken();
					}
					else {
						anotherLine = in.readLine();
						while(anotherLine != null) {
							parse = new StringTokenizer(anotherLine);
							if(parse.hasMoreTokens() && parse.nextToken().equals("Cookie:")) {
								firsttime = false;
								if(parse.hasMoreTokens()) 
									encodedCookieVal = parse.nextToken();
								break;
							}
							anotherLine = in.readLine();
						} 
					}
				}

				checkModifiedDateStringVal = checkModifiedDateStringVal.trim();
				Date checkModifiedDateVal = null;
				boolean correctDate = true;
				boolean needToModify = true;
				if(checkModified) {
					try {
						checkModifiedDateVal = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z").parse(checkModifiedDateStringVal);
					}
					catch(ParseException e) {
						System.err.println(e + " .Invalid date provided for check modified");
						System.out.println("date string: "+checkModifiedDateStringVal);
						correctDate = false;
						needToModify = true;
						System.out.println("correct date set to false on line 122");
					}
				}
				if(correctDate && checkModified) {
					if((new Date(System.currentTimeMillis()).compareTo(checkModifiedDateVal)) < 0) {
						needToModify = true;
						correctDate = false;
						System.out.println("correct date set to false on line 130");
					}
				}
				
				// we support only GET, HEAD, and POST methods, we check
				// we support only GET, HEAD, and POST methods, we do not implement DELETE, LINK, PUT,or UNLINK
				//check if the method is GET
				if(method.equals("GET")){
					//check http version
					//if not HTTP/1.0 but HTTP/1.something then return HTTP_NOT_SUPPORTED
					if(!httpver.equals("HTTP/1.0") && httpver.contains("HTTP/1.")){

					//print error message for debugging
						out.println("HTTP/1.0 " + HTTP_NOT_SUPPORTED);
					//send error message to client
						dataOut.writeBytes("HTTP/1.0 " + HTTP_NOT_SUPPORTED);

					//flush data output stream
						dataOut.flush();

	//if httpver isn't http/1.0 or some higher 1. something version return Bad request
					}else if(!httpver.equals("HTTP/1.0") && !httpver.contains("HTTP/1.")){
					//print error message for debug
						out.println("HTTP/1.0 " +BAD_REQ);
					//send err message to client
						dataOut.writeBytes("HTTP/1.0 "+BAD_REQ);
					//flush dataoutput stream
						dataOut.flush();
					}else{
					//preform get and send message to client

					// remove the / at the first character of the string. causes path recognition problem
						fileRequested = fileRequested.substring(1, fileRequested.length());

						LocalDateTime myDateObj = LocalDateTime.now();
						DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
						String formattedDate = myDateObj.format(myFormatObj);

						String encodedDateTime = URLEncoder.encode(formattedDate, "UTF-8");
						System.out.printf("URL encoded date-time %s \n", encodedDateTime);

						String decodedDateTime = URLDecoder.decode(encodedDateTime, "UTF-8");
						System.out.printf("URL decoded date-time %s \n", decodedDateTime);







					//first turn fileRequested into a file
						File filereq = new File(fileRequested);
						System.out.println("File requested text: "+fileRequested);

						if(filereq.exists()) {

							if(!(filereq.canRead())) {
								out.println("HTTP/1.0 "+FORBIDDEN);
								dataOut.writeBytes("HTTP/1.0 "+FORBIDDEN);
							}
							else {
							//get when file was last modified
								long lastmod=filereq.lastModified();
							//convert to last mod date
								Date datemod=new Date(lastmod);
							//creating DateFormat for converting time from local timezone to GMT
								DateFormat converter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
							//getting GMT timezone, you can get any timezone e.g. UTC
								converter.setTimeZone(TimeZone.getTimeZone("GMT"));
								Calendar cal = Calendar.getInstance();
							cal.add(Calendar.YEAR, 1); // to get previous year add -1
							Date nextYear = cal.getTime();

							if(correctDate && checkModified) {
								System.out.println("Entered if statement");
								int temp = datemod.compareTo(checkModifiedDateVal);
								System.out.println(temp + " is the comparison result");
								if(temp > 0) {
									needToModify = true;
									System.out.println(checkModifiedDateVal.toString() + " check modified");
									System.out.println("need to modify remained true on line 190");
								}
								else {
									needToModify = false;
									System.out.println(checkModifiedDateVal.toString() + " check modified");
								}
							}
							System.out.println("check modified is "+checkModified);
							System.out.println("correct date is "+correctDate);

							/*if(needToModify) {
								out.println("HTTP/1.0 "+OK);
								out.println("Content-Type: " + getContentType(fileRequested) );
								out.println("Content-Length: " + filereq.length());

								out.println("Last-Modified: " + converter.format(datemod));
								out.println("Content-Encoding: identity");
								out.println("Allow: GET, POST, HEAD");
								out.println("Expires: "+converter.format(nextYear));

								System.out.println("HTTP/1.0 "+OK);
								System.out.println("Content-Type: " + getContentType(fileRequested) );
								System.out.println("Content-Length: " + filereq.length());

								System.out.println("Last-Modified: " + converter.format(datemod));
								System.out.println("Content-Encoding: identity");
								System.out.println("Allow: GET, POST, HEAD");
								System.out.println("Expires: "+converter.format(nextYear));
								

								byte[] payloadData = readFileData(filereq, (int) filereq.length());
								System.out.println(payloadData);

								dataOut.write(("HTTP/1.0 "+OK+"\r\n").getBytes());
								dataOut.write(("Content-Type: "+getContentType(fileRequested)+"\r\n").getBytes());
								dataOut.write(("Content-Length: "+filereq.length()+"\r\n").getBytes());
								dataOut.write(("Last-Modified: "+converter.format(datemod)+"\r\n").getBytes());
								dataOut.write(("Content-Encoding: identity\r\n").getBytes());
								dataOut.write(("Allow: GET, POST, HEAD\r\n").getBytes());
								dataOut.write(("Expires: "+converter.format(nextYear)+"\r\n").getBytes());
								dataOut.write(("\r\n").getBytes());
								dataOut.write(payloadData, 0, (int) filereq.length());
							}
							else {
								out.println("HTTP/1.0 "+NOT_MOD);
								out.println("Expires: "+converter.format(nextYear));
								System.out.println("HTTP/1.0 "+NOT_MOD);
								System.out.println("Expires: "+converter.format(nextYear));
								dataOut.write(("HTTP/1.0 "+NOT_MOD+"\r\n").getBytes());
								dataOut.write(("Expires: "+converter.format(nextYear)+"\r\n").getBytes());
							} */
							if(firsttime) {
								byte[] payloadData = readFileData(filereq, (int) filereq.length());
								System.out.println(payloadData);

								dataOut.write(("HTTP/1.0 "+OK+"\r\n").getBytes());
								dataOut.write(("Content-Type: "+getContentType(fileRequested)+"\r\n").getBytes());
								dataOut.write(("Set-Cookie: lasttime="+encodedDateTime+"\r\n").getBytes());
							}
							else {
								filereq = new File("index-seen.html");
								byte[] payloadData = readFileData(filereq, (int) filereq.length());
								System.out.println(payloadData);
								dataOut.write(("HTTP/1.0 "+OK+"\r\n").getBytes());
								dataOut.write(("Content-Type: "+getContentType(fileRequested)+"\r\n").getBytes());
								dataOut.write(("Set-Cookie: lasttime="+encodedDateTime+"\r\n").getBytes());
							}
						}
					} else {
						out.println("HTTP/1.0 "+FILE_NOT_FOUND);
						dataOut.writeBytes("HTTP/1.0 "+FILE_NOT_FOUND);
						System.out.println(filereq + " doesn't exist");
					}

					//flush output
					dataOut.flush();

				}

			}
			dataOut.write(("HTTP/1.0 "+OK+"\r\n").getBytes());
			dataOut.write(("Content-Type: text/html"+"\r\n").getBytes());
			dataOut.write(("Content-Length: "+filereq.length()+"\r\n").getBytes());
						/*	dataOut.write(("Last-Modified: "+converter.format(datemod)+"\r\n").getBytes());
						dataOut.write(("Content-Encoding: identity\r\n").getBytes()); */
						dataOut.write(("Allow: GET, POST, HEAD\r\n").getBytes());
						dataOut.write(("Expires: "+converter.format(nextYear)+"\r\n").getBytes());
						dataOut.write(("\r\n").getBytes());
						dataOut.write(payloadData, 0, (int) filereq.length());
					}
				} else {
					System.out.println("HTTP/1.0 "+FILE_NOT_FOUND);
					dataOut.writeBytes("HTTP/1.0 "+FILE_NOT_FOUND);
					System.out.println(filereq + " doesn't exist");
				}

					//flush output
				dataOut.flush();
				updateRunningThreads(false);
			}



  // if method is none of the above return 400 Bad Request
  }else{
  // error message print on server side for debugging
  out.println("HTTP/1.0 "+BAD_REQ);

  // send error message to client
  dataOut.writeBytes("HTTP/1.0 "+BAD_REQ);

  // flush dataoutput stream
  dataOut.flush();}

  }


  }catch(

  FileNotFoundException fnfe)
  {
    try {
      fileNotFound(out, dataOut, fileRequested);
    } catch (IOException ioe) {
      System.err.println(FILE_NOT_FOUND + ": " + ioe.getMessage());
    }

  }catch(SocketTimeoutException e)
  {
    try {
      dataOut.write(("HTTP/1.0 " + REQ_TIMEOUT + "\r\n").getBytes());
      dataOut.flush();
    } catch (IOException ioe) {
      System.err.println(ioe);
    }
    System.err.println(REQ_TIMEOUT);
    System.out.println("Time started: " + timestart);
    System.out.println("Time ended: " + System.currentTimeMillis());
  }catch(
  IOException ioe)
  {
    System.err.println(INSERVERR + ": " + ioe);
  }finally
  {
    try {
      in.close();
      out.close();
      dataOut.close();
      connect.close(); // we close socket connection
    } catch (Exception e) {
      System.err.println("Error closing stream : " + e.getMessage());
    }

    if (verbose) {
      System.out.println("Connection closed.\n");
    }
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

  // helper method to convert a file to a string; for use with index.html and index_seen.html; for
  // more details/example refer to:
  // https://howtodoinjava.com/java/io/java-read-file-to-string-examples/
  private static String htmltostring(String filePath) {

		String content = "";
		try{
			content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		return content;
	}

}
