package photoassociation;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.*;
import javax.servlet.http.*;
import com.qizx.api.CompilationException;
import com.qizx.api.Configuration;
import com.qizx.api.Expression;
import com.qizx.api.Item;
import com.qizx.api.ItemSequence;
import com.qizx.api.Library;
import com.qizx.api.LibraryManager;
import com.qizx.api.LibraryManagerFactory;
import com.qizx.api.LibraryMember;
import com.qizx.api.Message;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.api.XQuerySession;
import com.qizx.api.XQuerySessionManager;
import com.qizx.api.util.DefaultModuleResolver;
import com.qizx.api.util.XMLSerializer;

public class XQueryServlet extends HttpServlet {

	private static final long serialVersionUID = -9186875057311859285L;

	{
	 	System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
	 	"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
	 	System.setProperty("javax.xml.parsers.SAXParserFactory",
	 	"org.apache.xerces.jaxp.SAXParserFactoryImpl");
	}
	
	public void init(ServletConfig servletconfig) throws ServletException {
		super.init(servletconfig);
		
	}

	public void doGet(HttpServletRequest httpservletrequest,HttpServletResponse httpservletresponse) throws IOException {
		doPost(httpservletrequest, httpservletresponse);
	}

	public void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws IOException {
		httpservletresponse.setContentType("text/xml; UTF-8");
		PrintStream out = new PrintStream(httpservletresponse.getOutputStream());
		XQuerySessionManager manager = null;
		XQuerySession session = null;
		String date = httpservletrequest.getParameter("date");
		String text = httpservletrequest.getParameter("text");
		try {
			Configuration.set(Configuration.ALLOWED_CLASSES, 
					"java.util.HashMap," +
					"java.lang.String," +
	 		 		"photoassociation.qizx.UtilityFunctions,"+
	 		 		"java.lang.Math");
			manager = Configuration.createSessionManager("localhost:8888/");			
			manager.setModuleResolver(new MyModuleResolver());
			session = manager.createSession();			
		    BufferedReader input = new BufferedReader(new InputStreamReader(this.getServletContext().getResourceAsStream("WEB-INF/src/xquery/xquery2compile.xq")));
			String line = null;
			StringBuffer sBuffer = new StringBuffer();
		    while ( (line = input.readLine()) != null ) { sBuffer.append(line); sBuffer.append("\n"); }
		    String xqy = sBuffer.toString();		    

			QName[] varNames = { session.getQName("text") , session.getQName("date") };
			String[] varValues = { text , date };
		    Expression expr = session.compileExpression(xqy);
			for (int i = 0; varNames != null && i < varNames.length; ++i) {
				expr.bindVariable(varNames[i], varValues[i], null);
			}
			ItemSequence results = expr.evaluate();
			XMLSerializer serializer = new XMLSerializer(out, "UTF-8");
			serializer.setIndent(2);
			serializer.setOmitXMLDeclaration(true);
			out.println("<?xml version='1.0' encoding='UTF-8'?>");
			out.println("<results>");
			while (results.moveToNextItem()) try {
				Item result = results.getCurrentItem();
				if (!result.isNode()) { out.println( "<error>" + result.getString() + "</error>" ); }
				Node node = result.getNode();
				serializer.reset();
				String xmlForm = serializer.serializeToString(node);
				out.println(xmlForm);
				out.flush();
			} catch ( Exception ex ) {
				out.println("<error>");
				if (session != null && manager != null && manager instanceof LibraryManager) try { 
					shutdown( (Library)session, (LibraryManager)manager);
				} catch (Exception e2) { }
				ex.printStackTrace(out);
				out.println("</error>");
				out.flush();
				return;
			}
			out.println("</results>");
			out.flush();
			if (session != null && manager != null && manager instanceof LibraryManager) {
				shutdown( (Library)session, (LibraryManager)manager);
			}
		} catch (Exception ex) {
			out.println("<results>\n <error>");
			if (session != null && manager != null && manager instanceof LibraryManager) try { 
				shutdown( (Library)session, (LibraryManager)manager);
			} catch (Exception e2) { }
			ex.printStackTrace(out);
			out.println("</error>\n </results>");
			out.flush();
		}		
	}
	
	private static void shutdown(Library lib, LibraryManager libManager) throws QizxException {
		lib.close();
		libManager.closeAllLibraries(10000);
	}

    class MyModuleResolver extends DefaultModuleResolver {
	 	
    	public MyModuleResolver ( ) throws MalformedURLException { 
                      super ( new URL( "http://localhost:8888/" ) ); 
	 	};
	 	
	 	public URL[] resolve (String moduleNamespaceURI, String[] locationHints) throws MalformedURLException {
	 		if (moduleNamespaceURI.equals("http://www.flickr.com/services/api/")) 
	 		 	return new URL[]{ new URL("http://localhost:8888/flickr.xqy") };
	 		else if (moduleNamespaceURI.equals("http://developer.yahoo.com/geo/")) 
	 		 	return new URL[]{ new URL("http://localhost:8888/geoplanet.xqy") };
	 		else if (moduleNamespaceURI.equals("http://web.tagus.ist.utl.pt/~rui.candeias/")) 
	 		 	return new URL[]{ new URL("http://localhost:8888/photoAssociation.xqy") };
	 		else return super.resolve(moduleNamespaceURI,locationHints);
	 	}
	}

}