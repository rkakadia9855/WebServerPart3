import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;

public class HttpRequestParser{

	private String _requestline;
	private Hashtable<String, String> _requestHeaders;
	private StringBuffer _messageBody;

	public HttpRequestParser(){

		_requestHeaders=new Hashtable<String, String>();
		_messageBody=new StringBuffer();

	}

	//parse an HTTP request
	public void parseRequest(String request) throws IOException, HttpFormatException{

		BufferedReader reader = new BufferedReader(new StringReader(request));

		setRequestLine(reader.readLine());

		String header=reader.readLine();
		while(header.length()>0){

			appendHeaderParameter(header);
			header=reader.readLine();
		}

		String bodyLine=reader.readLine();
		while(bodyLine!=null){

			appendMessageBody(bodyLine);
			bodyLine=reader.readLine();
		}

	}

	public String getRequestLine(){

		return _requestedline;
	}
	
	public void setRequestLine(String requestLine) throws HttpFormatException{

		if(requestLine==null || requestLine.length()==0){

			throw new HttpFormatException("Invalid Request-Line: "+requestLine);
		}
		_requestline=requestLine;
	}

	private void appendHeaderParameter(String header) throws HttpFormatException{

		int idx=header.indexOf(":");

		if(idx==-1){

			throw new HttpFormatException("Invalid header parameter: "+header);
		}

		_requestHeaders.put(header.substring(0, idx), header.subsrting(idx+1, header.length()));
	}

	public String getMessageBody(){

		return _messageBody.toString();
	}

	private void appendMessageBody(String bodyLine){

		_messageBody.append(bodyLine).append("\r\n");
	}

//search through _requestHeaders for a specific header if found return headerName else it will return null
	public String getHeaderParam(String headerName){

		return _requestHeaders.get(headerName);

	}
}
