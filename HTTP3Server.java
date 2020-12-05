import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.text.*;
import java.time.*;


// Each Client Connection will be managed in a dedicated Thread
public class HTTP1Server implements Runnable{
	
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
	
	public HTTP1Server(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {

			//get port from command line arg and convert to a useable integer
			int port=Integer.parseInt(args[0]);

			ServerSocket serverConnect = new ServerSocket(port);
		//	serverConnect.setSoTimeout(TIMEOUT);
			System.out.println("Server started.\nListening for connections on port : " + port + " ...\n");
			
			// we listen until user halts server execution
			while (true) {
				HTTP1Server myServer = new HTTP1Server(serverConnect.accept());
				
				if (verbose) {
					System.out.println("Connection opened. (" + new Date() + ")");
				}
				
				// add first 5 threads into arraylist
				ArrayList<Thread> threadList = new ArrayList<>();
				for (int i = 0; i < 5; i++) {
					Thread thread = new Thread(myServer);
					threadList.add(thread);
				}
				//iterate arraylist to count for running threads, remove terminated extra threads if there are more than 5 threads
				for (int i = 0; i < threadList.size(); i++) {
					runningThreads = 0;
					if (threadList.get(i).isAlive()) {
						runningThreads++;
					}
					else {
						if (i >= 5) {
							try {
								threadList.get(i).join();
								threadList.remove(threadList.get(i));
							}
							catch (Exception e) {
								System.err.println(e.getMessage());
							}
						}
					}
				}
				//start threads
				if (runningThreads < 5) {
					for (Thread thread: threadList) {
						if (!thread.isAlive()) {
							thread.start();
						}
					}
				}
				else if (runningThreads < 50) {
					Thread newThread = new Thread();
					threadList.add(newThread);
					newThread.start();
				}
				else {
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
	//check if the method is HEAD
				}else if(method.equals("HEAD")){
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

							//print for debug
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

							dataOut.write(("HTTP/1.0 "+OK+"\r\n").getBytes());
							dataOut.write(("Content-Type: "+getContentType(fileRequested)+"\r\n").getBytes());
							dataOut.write(("Content-Length: "+filereq.length()+"\r\n").getBytes());
							dataOut.write(("Last-Modified: "+converter.format(datemod)+"\r\n").getBytes());
							dataOut.write(("Content-Encoding: identity\r\n").getBytes());
							dataOut.write(("Allow: GET, POST, HEAD\r\n").getBytes());
							dataOut.write(("Expires: "+converter.format(nextYear)+"\r\n").getBytes());
							dataOut.write(("\r\n").getBytes());
						}
					} else {
						out.println("HTTP/1.0 "+FILE_NOT_FOUND);
						dataOut.writeBytes("HTTP/1.0 "+FILE_NOT_FOUND);
						System.out.println(filereq + " doesn't exist");
					}

					//flush output
						dataOut.flush();

					}
	//check if the method is POST
				}else if(method.equals("POST")){
					int itr = 0;
					int lastIndexes = 0;
					String contentType = "";
					int contentLength = 0;
					//decode 7th(last line of request)
					//assign to data to decode to string
					String encoded="";
					for(int i = 0; i < 7; i++) {
						System.out.println("itr num "+ itr+": ");
						itr++;
						if(anotherLine != null) {
							System.out.println("another line read.");
							parse = new StringTokenizer(anotherLine);
							String tmp = "";
							if(parse.hasMoreTokens())
								tmp = parse.nextToken();
							if(parse != null) {
								if( tmp.equals("Content-Length:")) {
									contentLengthPresent = true;
									System.out.println("content length present");
									if(parse.hasMoreTokens()) {
										try{
											String temp123 = parse.nextToken();
											contentLength = Integer.parseInt(temp123);
										}
										catch(Exception e) {
											System.out.println("can't parse int");
										}
									}
									while(parse.hasMoreTokens()) {
										tmp += parse.nextToken();
									}
									System.out.println("Line: "+tmp);
								}
								else if(tmp.equals("Content-Type:")) {
									contentTypePresent = true;
									System.out.println("content type present");
									if(parse.hasMoreTokens())
										contentType = parse.nextToken();
									tmp += contentType;
									while(parse.hasMoreTokens()) {
										tmp += parse.nextToken();
									}
									System.out.println("Line: "+tmp);
								}
								else {
									if(!(tmp.equals("POST") || tmp.equals("From:") || 
									tmp.equals("User-Agent:") || tmp.equals("Content-Type:") ||
									tmp.equals("Content-Length:") || lastIndexes!=0)) {
								//	if(anotherLine.equals("")) {
										lastIndexes = 1;
										System.out.println("Blank line detected");
										System.out.println("Line: "+anotherLine);
									}
									else if(lastIndexes == 1) {
										System.out.println("Line: "+tmp);
										encoded = tmp;
										System.out.println("Encoded = "+encoded); 
										lastIndexes = 2;
									}
									else
										System.out.println("Line: "+anotherLine);
								}
							}
							System.out.println("exiting if statement");
						}
						System.out.println("trying to read another line. lastindexes is "+lastIndexes);
						if(lastIndexes == 0)
							anotherLine = in.readLine();
						else if(lastIndexes == 1 && contentLength != 0)
							anotherLine = in.readLine();
						else if(lastIndexes == 2)
							break;
						System.out.println("tried to read another line at end of itr "+itr);
					}
					lastIndexes = 0;

									int sizecheck=encoded.length();
					char[] decode=new char[sizecheck];
					//create variable for index of decoded
					int decodei=0;
					for(int i=0; i<sizecheck; i++){
					
						//if the character at the index is not ! add to decoded char array
						if(!(encoded.charAt(i)=='!')){

							decode[decodei]=encoded.charAt(i);
							//increase decode index
							decodei++;
						}

						//if char at index is ! check if previous index was ! if so add to decoded
						if(encoded.charAt(i)=='!'){

							//for cases like x=!! which should be x=! when decoded so you would add !
							if(encoded.charAt(i-1)=='!'){

								decode[decodei]=encoded.charAt(i);

								//increase decode index
								decodei++;
							}

							//if previous checking char is not ! do not add !
						}
					
					}

					//convert decode char array to string
					String decoded="";
					decoded=decoded.copyValueOf(decode,0,sizecheck);

					//used for debugging and making sure encoded was properly decoded
					System.out.println("The string to be decoded is: "+encoded);
					System.out.println("The decoded string is: "+decoded);
					
				/*	while(anotherLine != null) {
						System.out.println("itr num "+ itr+": ");
						itr++;
							System.out.println("another line read.");
							parse = new StringTokenizer(anotherLine);
							String tmp = "";
							if(parse.hasMoreTokens())
								tmp = parse.nextToken();
							if(parse != null) {
								if(parse.hasMoreTokens() && tmp.equals("Content-Length:")) {
									contentLengthPresent = true;
									System.out.println("content length present");
								}
								else if(parse.hasMoreTokens() && tmp.equals("Content-Type:")) {
									contentTypePresent = true;
									System.out.println("content type present");
								}
								else {
									System.out.println(tmp);
								}
							}
						anotherLine = in.readLine();
					} */
					System.out.println("loop exited");
					
					//find out how if there is more than one parameter in decoded then break into parameters for use in cgi
					//keep track of number of parameters
					int varnum=0;
					int size=0;
					if(decoded!=null||(!decoded.equals(""))&&decoded.contains("&")){
						size=2;
					}else if(decoded!=null||(!decoded.equals(""))){
					
						size=1;

					}
					String[]evnp=new String[size];
					if(decoded!=null||(!decoded.equals(""))){
						
						
						if(decoded.contains("&")){

							varnum=2;
							//get index of &
							int parmsepindex=decoded.indexOf("&");

							//get the full first param(i.e. x=!)
							String fullp1=decoded.substring(0,parmsepindex);
							//get full second param
							String fullp2=decoded.substring(parmsepindex+1);
							evnp[0]=fullp1;
							evnp[1]=fullp2;

						}else{

							varnum=1;
							evnp[0]=decoded;
						}
					}
	//check http version
					//if not HTTP/1.0 but HTTP/1.something then return HTTP_NOT_SUPPORTED
					if(!httpver.equals("HTTP/1.0") && httpver.contains("HTTP/1.")){

					//print error message for debugging
					System.out.println("HTTP/1.0 " + HTTP_NOT_SUPPORTED);
					//send error message to client
					dataOut.writeBytes("HTTP/1.0 " + HTTP_NOT_SUPPORTED);

					//flush data output stream
					dataOut.flush();

	//if httpver isn't http/1.0 or some higher 1. something version return Bad request
					}else if(!httpver.equals("HTTP/1.0") && !httpver.contains("HTTP/1.")){
					//print error message for debug
						System.out.println("HTTP/1.0 " +BAD_REQ);
					//send err message to client
						dataOut.writeBytes("HTTP/1.0 "+BAD_REQ);
					//flush dataoutput stream
						dataOut.flush();
					}else{
					//preform get and send message to client

					// remove the / at the first character of the string. causes path recognition problem
					fileRequested = fileRequested.substring(1, fileRequested.length());
					//first turn fileRequested into a file
					File filereq = new File(fileRequested);
					System.out.println("File requested text: "+fileRequested);

					if(!(fileRequested.endsWith(".cgi"))) {
							System.out.println("HTTP/1.0 "+NOT_ALLOWED);
							dataOut.writeBytes("HTTP/1.0 "+NOT_ALLOWED);
					}
					else if(!contentLengthPresent) {
						System.out.println("HTTP/1.0 "+LENGTH_REQUIRED);
						dataOut.writeBytes("HTTP/1.0 "+LENGTH_REQUIRED);
					}
					else if(!contentTypePresent) {
						System.out.println("HTTP/1.0 "+INSERVERR);
						dataOut.writeBytes("HTTP/1.0 "+INSERVERR);
					}
					else if(!(contentType.equals("application/x-www-form-urlencoded"))) {
						System.out.println("HTTP/1.0 "+INSERVERR);
						dataOut.writeBytes("HTTP/1.0 "+INSERVERR);
					}
					else if(filereq.exists()) {

					/*	if(!(filereq.canRead())) {
							out.println("HTTP/1.0 "+FORBIDDEN);
							dataOut.writeBytes("HTTP/1.0 "+FORBIDDEN);
						} */
						if(!(filereq.canExecute())) {
							System.out.println("HTTP/1.0 "+FORBIDDEN);
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

							//print for debug
							System.out.println("HTTP/1.0 "+OK);
							System.out.println("Content-Type: text/html" );
							System.out.println("Content-Length: " + filereq.length());

							/*System.out.println("Last-Modified: " + converter.format(datemod));
							System.out.println("Content-Encoding: identity"); */
							System.out.println("Allow: GET, POST, HEAD");
							System.out.println("Expires: "+converter.format(nextYear));

						/*	System.out.println("HTTP/1.0 "+OK);
							System.out.println("Content-Type: " + getContentType(fileRequested) );
							System.out.println("Content-Length: " + filereq.length());
							System.out.println("Last-Modified: " + converter.format(datemod));
							System.out.println("Content-Encoding: identity");
							System.out.println("Allow: GET, POST, HEAD");
							System.out.println("Expires: "+converter.format(nextYear)); */
							

							byte[] payloadData = readFileData(filereq, (int) filereq.length());
							System.out.println(payloadData);
							
							//setup for running cgi file on server side
						/*	try {
								//cmd should be equal to fileRequested
								String cmd = fileRequested;
								String content = null;
								Process process = Runtime.getRuntime().exec(cmd, new String[] {"i lvoe u", "hahahah"});
								BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
								while ((content = stdInput.readLine()) != null) {
			   					System.out.println(content);
							}
								System.out.println("process run successfully.");
							} catch (Exception ex) {
								ex.printStackTrace();
							} */

							try {
								String cmd = fileRequested;
								String content = null;
								//use if no parameters
								Process process=Runtime.getRuntime().exec(cmd);;
								if(varnum==0){
									process = Runtime.getRuntime().exec(cmd);
								}else if(varnum>0){
									process = Runtime.getRuntime().exec(cmd, evnp);
								}
								//reader will get msg from subprocess
								BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
								//writer will send msg from main process to subprocess
								 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
								writer.write("haha");
								 writer.flush();
								 while ((content = reader.readLine()) != null) {
								 	System.out.println(content);
								}
								int i;
								StringBuffer sb = new StringBuffer();
								while ((i = reader.read()) != -1) {
								   sb.append((char)i);
								}
								reader.close();
								System.out.println(sb.toString());
								System.out.println("process run successfully.");
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							//if no parameters
							/*if(varnum==0){
								ProcessBuilder pb = new ProcessBuilder(fileRequested);
								  // startinf the process 
        							Process process = pb.start(); 
          
        							// for reading the ouput from stream 
        							BufferedReader stdInput = new BufferedReader(new
         							InputStreamReader(process.getInputStream())); 
        							String s = null; 
        							while ((s = stdInput.readLine()) != null) 
        							{ 
            							System.out.println(s); 
        							} 
							}else if(varnum==1){
							
								ProcessBuilder pb=new ProcessBuilder(fileRequested, "-c", "echo %"+varname+"%");
								Map<String, String> environment = processBuilder.environment();
								environment.put(varname, varval);
								Process p = processBuilder.start();
    								String line;
    								BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
    								while ((line = r.readLine()) != null) {
        								System.out.println(line);
    								}
    								r.close();
								
							}else if(varnum==2){
						
								ProcessBuilder pb=new ProcessBuilder(fileRequested, "-c", "echo %"+fp1varname+"%", "-c", "echo %"+fp2varname+"%");
								Map<String, String> environment = processBuilder.environment();
								environment.put(fp1varname, fp1val);
								environment.put(fp2varname, fp2val);
								Process p = processBuilder.start();
    								String line;
    								BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
    								while ((line = r.readLine()) != null) {
        								System.out.println(line);
    								}
    								r.close();
							}*/

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

					}
					//check if the method is not implemented DELETE
				}else if(method.equals("DELETE")){
	//print err message
					out.println("HTTP/1.0 " + NOTIMP);
					//send message to client
					dataOut.writeBytes("HTTP/1.0 " + NOTIMP);
					//flush data stream
					dataOut.flush();
					
	//check if the method is not implemented LINK
				}else if(method.equals("LINK")){
	//print err message
					out.println("HTTP/1.0 " + NOTIMP);
					//send message to client
					dataOut.writeBytes("HTTP/1.0 " + NOTIMP);
					//flush data stream
					dataOut.flush();

	//check if method is not implemented UNLINK
				}else if(method.equals("UNLINK")){
	//print err message
					out.println("HTTP/1.0 " + NOTIMP);
					//send message to client
					dataOut.writeBytes("HTTP/1.0 " + NOTIMP);
					//flush data stream
					dataOut.flush();

	//check if method is not implemented PUT
				}else if(method.equals("PUT")){
	//print err message
					out.println("HTTP/1.0 " + NOTIMP);
					//send message to client
					dataOut.writeBytes("HTTP/1.0 " + NOTIMP);
					//flush data stream
					dataOut.flush();

	//if method is none of the above return 400 Bad Request
				}else{
					//error message print on server side for debugging
					out.println("HTTP/1.0 "+ BAD_REQ);

					//send error message to client
					dataOut.writeBytes("HTTP/1.0 " + BAD_REQ);

					//flush dataoutput stream
					dataOut.flush();
				}

			}

			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println(FILE_NOT_FOUND +": " + ioe.getMessage());
			}
			
		} catch (SocketTimeoutException e) {
			try {
				dataOut.write(("HTTP/1.0 " + REQ_TIMEOUT+"\r\n").getBytes());
				dataOut.flush();
			}
			catch (IOException ioe) {
				System.err.println(ioe);
			}
			System.err.println(REQ_TIMEOUT);
			System.out.println("Time started: "+timestart);
			System.out.println("Time ended: "+System.currentTimeMillis());
		} catch (IOException ioe) {
			System.err.println(INSERVERR + ": " + ioe);
		} finally {
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
		out.println("Server: HTTP1Server");
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