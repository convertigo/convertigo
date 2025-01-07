/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import org.apache.xerces.dom.DOMMessageFormatter;
import org.apache.xerces.dom.NodeImpl;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

public class TwsTreeWalker  implements TreeWalker {
    
    //
    // Data
    //
    
    /** When TRUE, the children of entites references are returned in the iterator. */
    private boolean fEntityReferenceExpansion = false;
    /** The whatToShow mask. */
    private int fWhatToShow = NodeFilter.SHOW_ALL;
    /** The NodeFilter reference. */
    private NodeFilter fNodeFilter;
    /** The current Node. */
    private Node fCurrentNode;
    /** The root Node. */
    private Node fRoot;
    /** Use Node.isSameNode() to check if one node is the same as another. */
    private boolean fUseIsSameNode;
    
    //
    // Implementation Note: No state is kept except the data above
    // (fWhatToShow, fNodeFilter, fCurrentNode, fRoot) such that 
    // setters could be created for these data values and the 
    // implementation will still work.
    
    
    // 
    // Constructor
    //
    
    /** Public constructor */
    public TwsTreeWalker(Node root, 
                          int whatToShow, 
                          NodeFilter nodeFilter,
                          boolean entityReferenceExpansion) {
        fCurrentNode = root;
        fRoot = root;
        fUseIsSameNode = useIsSameNode(root);
        fWhatToShow = whatToShow;
        fNodeFilter = nodeFilter;
        fEntityReferenceExpansion = entityReferenceExpansion;
    }
    
    public Node getRoot() {
	return fRoot;
    }

    /** Return the whatToShow value */
    public int                getWhatToShow() {
        return fWhatToShow;
    }

    public void setWhatShow(int whatToShow){
        fWhatToShow = whatToShow;
    }
    /** Return the NodeFilter */
    public NodeFilter         getFilter() {
        return fNodeFilter;
    }
    
    /** Return whether children entity references are included in the iterator. */
    public boolean            getExpandEntityReferences() {
        return fEntityReferenceExpansion;
    }
            
    /** Return the current Node. */
    public Node               getCurrentNode() {
        return fCurrentNode;
    }
    /** Return the current Node. */
    public void               setCurrentNode(Node node) {
        if (node == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
              throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
        }

        fCurrentNode = node;
    }
    
    /** Return the parent Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               parentNode() {

        if (fCurrentNode == null) return null;
                
        Node node = getParentNode(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
        
    }

    /** Return the first child Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               firstChild() {
        
        if (fCurrentNode == null) return null;
                
        Node node = getFirstChild(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    /** Return the last child Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               lastChild() {

        if (fCurrentNode == null) return null;
                
        Node node = getLastChild(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    
    /** Return the previous sibling Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               previousSibling() {

        if (fCurrentNode == null) return null;
                
        Node node = getPreviousSibling(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    
    /** Return the next sibling Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               nextSibling(){
        if (fCurrentNode == null) return null;
                
        Node node = getNextSibling(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    
    /** Return the previous Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    @SuppressWarnings("unused")
	public Node               previousNode() {
        Node result;
        
        if (fCurrentNode == null) return null;
        
        // get sibling
        result = getPreviousSibling(fCurrentNode);
        if (result == null) {
            result = getParentNode(fCurrentNode);
            if (result != null) {
                fCurrentNode = result;
                return fCurrentNode;
            } 
            return null;
        }
        
        // get the lastChild of result.
        Node lastChild  = getLastChild(result);
        
        Node prev = lastChild ;
        while (lastChild != null) {
          prev = lastChild ;
          lastChild = getLastChild(prev) ;
        }

        lastChild = prev ;
        
        // if there is a lastChild which passes filters return it.
        if (lastChild != null) {
            fCurrentNode = lastChild;
            return fCurrentNode;
        } 
        
        // otherwise return the previous sibling.
        if (result != null) {
            fCurrentNode = result;
            return fCurrentNode;
        }
        
        // otherwise return null.
        return null;
    }
    
    /** Return the next Node from the current node, 
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               nextNode() {
        
        if (fCurrentNode == null) return null;
        
        Node result = getFirstChild(fCurrentNode);
        
        if (result != null) {
            fCurrentNode = result;
            return result;
        }
        
        result = getNextSibling(fCurrentNode);
        
        if (result != null) {
            fCurrentNode = result;
            return result;
        }
                
        // return parent's 1st sibling.
        Node parent = getParentNode(fCurrentNode);
        while (parent != null) {
            result = getNextSibling(parent);
            if (result != null) {
                fCurrentNode = result;
                return result;
            } else {
                parent = getParentNode(parent);
            }
        }
        
        // end , return null
        return null;
    }
    
    /** Internal function.
     *  Return the parent Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    private Node getParentNode(Node node) {
        
        if (node == null || isSameNode(node, fRoot)) return null;
        
        Node newNode = node.getParentNode();
        if (newNode == null)  return null; 
                        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        //if (accept == NodeFilter.SKIP_NODE) // and REJECT too.
        {
            return getParentNode(newNode);
        }
        
        
    }
    
    /** Internal function.
     *  Return the nextSibling Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    private Node getNextSibling(Node node) {
		return getNextSibling(node, fRoot);
	}

    /** Internal function.
     *  Return the nextSibling Node, from the input node
     *  after applying filter, whatToshow.
     *  NEVER TRAVERSES ABOVE THE SPECIFIED ROOT NODE. 
     *  The current node is not consulted or set.
     */
    private Node getNextSibling(Node node, Node root) {
        for (;;) {
	        if (node == null || isSameNode(node, root)) return null;
	        
	        Node newNode = node.getNextSibling();
        	for (;;) {
		        if (newNode == null) {
		                
		            newNode = node.getParentNode();
		                
		            if (newNode == null || isSameNode(newNode, root)) return null; 
		                
		            int parentAccept = acceptNode(newNode);
		                
		            if (parentAccept==NodeFilter.FILTER_SKIP) {
		            	break;
		                // return getNextSibling(newNode, root);
		            }
		                
		            return null;
		        }
		        
		        int accept = acceptNode(newNode);
		        
		        if (accept == NodeFilter.FILTER_ACCEPT)
		            return newNode;
		        else 
		        if (accept == NodeFilter.FILTER_SKIP) {
		            Node fChild = getFirstChild(newNode);
		            if (fChild == null) {
		            	break;
		                //return getNextSibling(newNode, root);
		            }
		            return fChild;
		        }
		        else 
		        //if (accept == NodeFilter.REJECT_NODE) 
		        {
		        	break;
		            //return getNextSibling(newNode, root);
		        }
        	}
        	node = newNode;
        }
    } // getNextSibling(Node node) {
    
    /** Internal function.
     *  Return the previous sibling Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    private Node getPreviousSibling(Node node) {
		return getPreviousSibling(node, fRoot);
	}

    /** Internal function.
     *  Return the previousSibling Node, from the input node
     *  after applying filter, whatToshow.
	 *  NEVER TRAVERSES ABOVE THE SPECIFIED ROOT NODE. 
     *  The current node is not consulted or set.
     */
    private Node getPreviousSibling(Node node, Node root) {
        
        if (node == null || isSameNode(node, root)) return null;
        
        Node newNode = node.getPreviousSibling();
        if (newNode == null) {
                
            newNode = node.getParentNode();
            if (newNode == null || isSameNode(newNode, root)) return null; 
                
            int parentAccept = acceptNode(newNode);
                
            if (parentAccept==NodeFilter.FILTER_SKIP) {
                return getPreviousSibling(newNode, root);
            }
            
            return null;
        }
        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        if (accept == NodeFilter.FILTER_SKIP) {
            Node fChild =  getLastChild(newNode);
            if (fChild == null) {
                return getPreviousSibling(newNode, root);
            }
            return fChild;
        }
        else 
        //if (accept == NodeFilter.REJECT_NODE) 
        {
            return getPreviousSibling(newNode, root);
        }
        
    } // getPreviousSibling(Node node) {
    
    /** Internal function.
     *  Return the first child Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    private Node getFirstChild(Node node) {
        if (node == null) return null;
        
        if ( !fEntityReferenceExpansion
             && node.getNodeType() == Node.ENTITY_REFERENCE_NODE)
            return null;
        Node newNode = node.getFirstChild();
        if (newNode == null)  return null;
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        if (accept == NodeFilter.FILTER_SKIP
            && newNode.hasChildNodes()) 
        {
            Node fChild = getFirstChild(newNode);
            
            if (fChild == null) {
                return getNextSibling(newNode, node);
            }
            return fChild;
        }
        else 
        //if (accept == NodeFilter.REJECT_NODE) 
        {
            return getNextSibling(newNode, node);
        }
        
        
    }
   
    /** Internal function.
     *  Return the last child Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    private Node getLastChild(Node node) {
        
        if (node == null) return null;
        
        if ( !fEntityReferenceExpansion
             && node.getNodeType() == Node.ENTITY_REFERENCE_NODE)
            return null;
            
        Node newNode = node.getLastChild();
        if (newNode == null)  return null; 
        
        int accept = acceptNode(newNode);
        
        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else 
        if (accept == NodeFilter.FILTER_SKIP
            && newNode.hasChildNodes()) 
        {
            Node lChild = getLastChild(newNode);
            if (lChild == null) {
                return getPreviousSibling(newNode, node);
            }
            return lChild;
        }
        else 
        //if (accept == NodeFilter.REJECT_NODE) 
        {
            return getPreviousSibling(newNode, node);
        }
        
        
    }
    
    /** Internal function. 
     *  The node whatToShow and the filter are combined into one result. */
    private short acceptNode(Node node) {
        /***
         7.1.2.4. Filters and whatToShow flags 

         Iterator and TreeWalker apply whatToShow flags before applying Filters. If a node is rejected by the
         active whatToShow flags, a Filter will not be called to evaluate that node. When a node is rejected by
         the active whatToShow flags, children of that node will still be considered, and Filters may be called to
         evaluate them.
         ***/
                
        if (fNodeFilter == null) {
            if ( ( fWhatToShow & (1 << node.getNodeType()-1)) != 0) {
                return NodeFilter.FILTER_ACCEPT;
            } else {
                return NodeFilter.FILTER_SKIP;
            }
        } else {
            if ((fWhatToShow & (1 << node.getNodeType()-1)) != 0 ) {
                return fNodeFilter.acceptNode(node);
            } else {
                // What to show has failed. See above excerpt from spec.
                // Equivalent to FILTER_SKIP.
                return NodeFilter.FILTER_SKIP;
            }
        }
    }
    
    /**
     * Use isSameNode() for testing node identity if the DOM implementation
     * supports DOM Level 3 core and it isn't the Xerces implementation.
     */
    private boolean useIsSameNode(Node node) {
        if (node instanceof NodeImpl) {
            return false;
        }
        Document doc = node.getNodeType() == Node.DOCUMENT_NODE 
            ? (Document) node : node.getOwnerDocument();
        return (doc != null && doc.getImplementation().hasFeature("Core", "3.0"));
    }
    
    /**
     * Returns true if <code>m</code> is the same node <code>n</code>.
     */
    private boolean isSameNode(Node m, Node n) {
        return (fUseIsSameNode) ? m.isSameNode(n) : m == n;
    }
}

