import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.lang.*;
import java.text.*;
import java.time.*;


// Each Client Connection will be managed in a dedicated Thread
public class PartialHTTP1Server implements Runnable{
	
	static final File WEB_ROOT = new File(".");
	static final String FILE_NOT_FOUND = "404 Not Found";
	static final String HTTP_NOT_SUPPORTED = "505 HTTP Version Not Supported";
	static final String OK = "200 OK";
	static final String NOT_MOD = "304 Not Modified";
	static final String BAD_REQ = "400 Bad Request";
	static final String FORBIDDEN = "403 Forbidden";
	static final String REQ_TIMEOUT = "408 Request Timeout";
	static final String INSERVERR = "500 Internal Server Error";
	static final String NOTIMP = "501 Not Implemented";
	static final String UNAVAILSERV = "503 Service Unavailable";
	static final int TIMEOUT = 5000;
	long timestart = 0;
	static int runningThreads = 0;

	
	// verbose mode
	static final boolean verbose = true;
	
	// Client Connection via Socket Class
	private Socket connect;
	
	public PartialHTTP1Server(Socket c) {
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
				PartialHTTP1Server myServer = new PartialHTTP1Server(serverConnect.accept());
				
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
			connect.setSoTimeout(TIMEOUT);
			
			// we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// create outputstream to send data to client
			dataOut = new DataOutputStream(connect.getOutputStream());
			
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
	 			// we get file requested
				fileRequested = parse.nextToken().toLowerCase();
				//get HTTP version. Also check if the token exists
				String httpver = "";
				boolean checkModified = false;
				System.out.println("Num tokens: "+parse.countTokens()); // Remove later
				if(parse.hasMoreTokens())
					httpver = parse.nextToken();
				System.out.println("httpver: "+httpver); //Remove later
				// read the next line in client's request
				String anotherLine = in.readLine();
				if(anotherLine != null) {
					parse = new StringTokenizer(anotherLine);
					System.out.println("another line read.");
				}
				boolean cookiefound=false;
				String[] cookievals;
				//check for cookies
				if(parse.hasMoreTokens()&&parse.nextToken().equals("Cookie:")) {
					
					//check if Cookie is lasttime
					String cookie=parse.nextToken();
					if(cookie.contains("lasttime")) {
						cookiefound=true;
						
						
					}
					
				}else if(parse.hasMoreTokens() && parse.nextToken().equals("If-Modified-Since:")) {
					checkModified = true;
					System.out.println("checked modified set to true on line 103");
				}
				String checkModifiedDateStringVal = "";
				while(parse.hasMoreTokens()){
					checkModifiedDateStringVal = checkModifiedDateStringVal + parse.nextToken() + " ";
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

						
						if(fileRequested.equals("/")) {
							
							//convert html files into string
							Path filenamei=Path.of("index.html");
							String newuser=Files.readString(filenamei);
							Path filenameis=Path.of("index_seen.html");
							String olduser=Files.readString(filenameis);
							
							if(cookiefound==true) {
								
								//check if date is formatted properly
								
							}
						}
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

							if(needToModify) {
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
					} else {
						out.println("HTTP/1.0 "+FILE_NOT_FOUND);
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
		out.println("Server: PartialHTTP1Server");
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
			
