/**
 *  Copyright 2007 Tail-F Systems AB. All rights reserved.
 *
 *  This software is the confidential and proprietary
 *  information of Tail-F Systems AB.
 *
 *  $Id$
 *
 */

package com.tailf.netconfmanager;

import java.util.*;

/**
 * This is an iterator class that is used for iterating over all leaf-list
 * children with a specified name in a NodeSet. An object of this iterator class
 * is obtained from the {@link Element#iterator} method.
 * <p>
 * Example usage:
 * 
 * <pre>
 * ElementLeafListValueIterator domainIter = config.iterator(&quot;domain&quot;);
 * while (domainIter.hasNext()) {
 *     String domain = (String) domainIter.next();
 *     System.out.println(&quot;Domain: &quot; + host);
 * }
 * </pre>
 * 
 */
public class ElementLeafListValueIterator implements Iterator<Object> {
    private Iterator<Element> childrenIterator;
    private Element nextChild;
    private boolean hasNextChild = false;
    private String name;

    /**
     * Constructor to create a new children iterator for leaf-list children of a
     * specific name.
     */
    public ElementLeafListValueIterator(NodeSet children, String name) {
        if (children != null)
            childrenIterator = children.iterator();
        else
            childrenIterator = null;
        this.name = name;
    }

    /**
     * Return true if there are more children, false otherwise.
     * 
     */
    public boolean hasNext() {
        if (hasNextChild)
            return true;
        if (childrenIterator == null)
            return false;
        while (childrenIterator.hasNext()) {
            if (name == null)
                return true;
            Element child = (Element) childrenIterator.next();
            if (child.name.equals(name)) {
                hasNextChild = true;
                nextChild = child;
                return true;
            }
        }
        hasNextChild = false;
        return false;
    }

    /**
     * Return next child or null.
     * 
     */
    public Object nextElement() {
        if (hasNextChild) {
            hasNextChild = false;
            return nextChild.value;
        }
        hasNextChild = false;
        while (childrenIterator.hasNext()) {
            Element child = (Element) childrenIterator.next();
            if (name == null)
                return child.value;
            else if (child.name.equals(name))
                return child.value;
        }
        return null;
    }

    /**
     * Return next child or null.
     */
    public Object next() {
        return nextElement();
    }

    /**
     * Remove is not supported.
     * 
     */
    public void remove() {
    }
}
