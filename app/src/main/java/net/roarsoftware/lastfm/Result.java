package net.roarsoftware.lastfm;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.roarsoftware.xml.DomElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The <code>Result</code> class contains the response sent by the server, i.e.
 * the status (either ok or failed), an error code and message if failed and the
 * xml response sent by the server.
 * 
 * @author Janni Kovacs
 */
public class Result {

	public enum Status {
		OK, FAILED
	}

	private final Status status;
	private String errorMessage = null;
	private int errorCode = -1;
	private int httpErrorCode = -1;

	private Document resultDocument;

	public Result(Document resultDocument) {
		this.status = Status.OK;
		this.resultDocument = resultDocument;
	}

	public Result(String errorMessage) {
		this.status = Status.FAILED;
		this.errorMessage = errorMessage;
	}

	static Result createOkResult(Document resultDocument) {
		return new Result(resultDocument);
	}

	static Result createHttpErrorResult(int httpErrorCode, String errorMessage) {
		Result r = new Result(errorMessage);
		r.httpErrorCode = httpErrorCode;
		return r;
	}

	static Result createRestErrorResult(int errorCode, String errorMessage) {
		Result r = new Result(errorMessage);
		r.errorCode = errorCode;
		return r;
	}

	/**
	 * Returns if the operation was successful. Same as
	 * <code>getStatus() == Status.OK</code>.
	 * 
	 * @return <code>true</code> if the operation was successful
	 */
	public boolean isSuccessful() {
		return status == Status.OK;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public int getHttpErrorCode() {
		return httpErrorCode;
	}

	public Status getStatus() {
		return status;
	}

	public Document getResultDocument() {
		return resultDocument;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public DomElement getContentElement() {
		if (!isSuccessful()) {
			System.out.println("LastFM = Not Successfull");
			return null;
		}

		/*
		 * try { TransformerFactory tf = TransformerFactory.newInstance();
		 * Transformer transformer = tf.newTransformer();
		 * transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
		 * "yes"); StringWriter writer = new StringWriter();
		 * transformer.transform(new DOMSource(resultDocument), new
		 * StreamResult(writer)); String output = writer.getBuffer().toString()
		 * .replaceAll("\n|\r", "");
		 * 
		 * System.out.println("LastFM Document: " + output); } catch (Exception
		 * e) { e.printStackTrace(); }
		 */

		// System.out.println("First Name : " +
		// resultDocument.getDocumentElement().getElementsByTagName("*").item(0).getTextContent());

		return new DomElement((Element) resultDocument.getDocumentElement()
				.getElementsByTagName("*").item(0));
	}
}
