package org.tvrenamer.controller.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

public class XPathUtilities {
    private static Logger logger = Logger.getLogger(XPathUtilities.class.getName());

    public static NodeList nodeListValue(String name, Document doc, XPath xpath)
        throws XPathExpressionException
    {
        XPathExpression expr = xpath.compile(name);
        return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }

    public static String nodeTextValue(String name, Node eNode, XPath xpath)
        throws XPathExpressionException
    {
        XPathExpression expr = xpath.compile(name);
        Node node = (Node) expr.evaluate(eNode, XPathConstants.NODE);
        if (node == null) {
            return null;
        }
        return node.getTextContent();
    }
}
