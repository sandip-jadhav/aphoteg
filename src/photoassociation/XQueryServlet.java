package photoassociation;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.qizx.api.CompilationException;
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
import com.qizx.api.util.XMLSerializer;

public class XQueryServlet extends HttpServlet {

	{
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
				"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		System.setProperty("javax.xml.parsers.SAXParserFactory",
				"org.apache.xerces.jaxp.SAXParserFactoryImpl");
	}

	public void init(ServletConfig servletconfig) throws ServletException {
		super.init(servletconfig);
	}

	public void doGet(HttpServletRequest httpservletrequest,
			HttpServletResponse httpservletresponse) throws IOException {
		doPost(httpservletrequest, httpservletresponse);
	}

	public void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws IOException {
		httpservletresponse.setContentType("text/xml");
		PrintStream out = new PrintStream(httpservletresponse.getOutputStream());
		String point = httpservletrequest.getParameter("point");
		String script = "";
		String libName = "";
		String queryRootPath = "";
		File storageDir = new File("");
		try {
			LibraryManagerFactory factory = LibraryManagerFactory.getInstance();
			LibraryManager libManager = factory.openLibraryGroup(storageDir);
			Library lib = libManager.openLibrary(libName);
			LibraryMember queryRoot = lib.getMember(queryRootPath);
			QName[] varNames = null;
			String[] varValues = null;
			Expression expr = compileExpression(lib, script, queryRoot, varNames, varValues);
			evaluateExpression(expr, 0, 10000);
			shutdown(lib, libManager);
		} catch (Exception ex) {
		}
	}

	private static Expression compileExpression(Library lib, String script, LibraryMember queryRoot, QName[] varNames, String[] varValues) throws IOException, QizxException {
		Expression expr;
		try {
			expr = lib.compileExpression(script);
		} catch (CompilationException e) {
			Message[] messages = e.getMessages();
			for (int i = 0; i < messages.length; ++i) {
				System.out.println(messages[i].toString());
			}
			throw e;
		}
		if (queryRoot != null) expr.bindImplicitCollection(queryRoot);
		if (varNames != null) {
			for (int i = 0; i < varNames.length; ++i) {
				expr.bindVariable(varNames[i], varValues[i], /* type */null);
			}
		}
		return expr;
	}

	private static void evaluateExpression(Expression expr, int from, int limit) throws QizxException {
		ItemSequence results = expr.evaluate();
		if (from > 0) {
			results.skip(from);
		}
		XMLSerializer serializer = new XMLSerializer();
		serializer.setIndent(2);
		int count = 0;
		while (results.moveToNextItem()) {
			Item result = results.getCurrentItem();
			System.out.print("[" + (from + 1 + count) + "] ");
			showResult(serializer, result);
			System.out.println();
			++count;
			if (count >= limit)
				break;
		}
		System.out.flush();
	}

	private static void showResult(XMLSerializer serializer, Item result) throws QizxException {
		if (!result.isNode()) {
			System.out.println(result.getString());
			return;
		}
		Node node = result.getNode();
		serializer.reset();
		String xmlForm = serializer.serializeToString(node);
		System.out.println(xmlForm);
	}

	private static void shutdown(Library lib, LibraryManager libManager) throws QizxException {
		lib.close();
		libManager.closeAllLibraries(10000);
	}

}