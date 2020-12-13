import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;
import java.net.http.*;

public class HttpRequestParser{

	public String _requestline;
	public Hashtable<String, String> _requestHeaders;
	public StringBuffer _messageBody;

	public HttpRequestParser(){

		_requestHeaders=new Hashtable<String, String>();
		_messageBody=new StringBuffer();

	}

	//parse an HTTP request
	public void parseRequest(String request) throws Exception, IOException{

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

		return _requestline;
	}
	
	public void setRequestLine(String requestLine) throws Exception{

		if(requestLine==null || requestLine.length()==0){

			throw new Exception("Invalid Request-Line: " + requestLine);
		}
		_requestline=requestLine;
	}

	public void appendHeaderParameter(String header) throws Exception{

		int idx=header.indexOf(":");

		if(idx==-1){

			throw new Exception("Invalid header parameter: " + header);
		}

		_requestHeaders.put(header.substring(0, idx), header.substring(idx+1, header.length()));
	}

	public String getMessageBody(){

		return _messageBody.toString();
	}

	public void appendMessageBody(String bodyLine){

		_messageBody.append(bodyLine).append("\r\n");
	}

//search through _requestHeaders for a specific header if found return headerName else it will return null
	public String getHeaderParam(String headerName){

		return _requestHeaders.get(headerName);

	}
}