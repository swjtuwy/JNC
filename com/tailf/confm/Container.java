/*    -*- Java -*-
 *
 *  Copyright 2007 Tail-F Systems AB. All rights reserved.
 *
 *  This software is the confidential and proprietary
 *  information of Tail-F Systems AB.
 *
 *  $Id$
 *
 */

package com.tailf.confm;
import java.util.*;
import com.tailf.inm.*;
import java.lang.reflect.*;
import java.lang.Class;

/**
 * The Container is a configuration sub-tree like the
 * {@link com.tailf.inm.Element Element}.
 * It is an extension of the Element class to make
 * the configuration sub-tree data model aware.
 * Classes generated from the ConfM compiler are
 * either Containers, Leafs, or derived data types.
 * <p>
 * Thus the Container which is an abstract class is never used
 * directly.
 * <p>
 * Fundamental methods for comparing, syncing, and inspecting
 * sets of configuration data, is provided by this class.
 * The following two methods highlights the fundmental purpose
 * of this library:
 * <ul>
 * <li>{@link #checkSync checkSync} - checks if two configurations are
 * are equal, or if a sync is needed. This is a common typical operation
 * a manager would like to do when one configuration is stored on
 * the device and the other is stored in a database.
 * <li>{@link #sync sync} - will calculate the difference between
 * two configuration sub trees and build up a resulting subtree
 * which is basically a tree with NETCONF operations
 * needed to turn the source tree into the target tree.
 * The purpose of this is to find a minimal set of operations
 * to sync two configurations so that they become equal.
 * </ul>
 *
 */
public abstract class Container extends Element {
    private static String reservedWords[] =
    {"abstract", "boolean", "break", "byte", "case", "catch", "char",
     "class", "const", "continue", "default", "do", "double", "else",
     "extends", "final", "finally", "float", "for", "goto", "if",
     "implements", "import", "instanceof", "int", "interface", "long",
     "native", "new", "package", "private", "protected", "public",
     "return","short","static","super", "switch", "synchronized", "this",
     "throw", "throws", "transient", "try", "void", "volatile", "while"};

    /**
     * Structure information.
     * An array of the children names.
     */
    abstract protected String[] childrenNames();

    /**
     * Structure information.
     * An array of the names of the key children.
     */
    abstract protected String[] keyNames();

    /**
     * Constructor for the container
     */
    public Container(String ns, String name) {
        super(ns, name);
    }

    /**
     * Static createInstance
     * this is a data model "aware" method to create
     * a container instance below a parent node.
     *
     * It is made package private to not allow
     * creation outside package.
     *
     * @return The created element or null if no container was created.
     *
     */
    static Element createInstance(ConfHandler parser, Element parent,
                                  String ns, String name)
        throws ConfMException {

        String pkg = hasPackage(ns);
        if (parent==null) {
            // ROOT
            if (pkg==null) return new Element(ns,name); // not aware

            try {
                String className =
                    pkg +"."+normalize(name);
                Class rootClass = Class.forName(className);
                return (Element)rootClass.newInstance();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new ConfMException(
                    ConfMException.ELEMENT_MISSING,
                    parent.getPath(name)+": Unexpected element");
            }
            catch (Exception e2) {
                e2.printStackTrace();
                throw new ConfMException(
                    ConfMException.ELEMENT_MISSING,
                    parent.getPath(name)+": Unexpected element");
            }
        } else {
            // CHILD
            if (pkg==null) {
                // not aware
                Element child= new Element(ns,name);
                parent.addChild(child);
                return child;
            }
            if (parent instanceof Container) {
                // aware

                try { // A Container
                    //System.out.println(parent.name+"."+methodName+"()");
                    String methodName = "add"+normalize(name);
                    Class[] types = new Class[] {};
                    //System.out.println(parent.name+"."+methodName+"()");
                    Method addContainer =
                        parent.getClass().getMethod(methodName, types);
                    Object[] args = {};
                    Element child = (Element)addContainer.invoke(parent, args);
                    return child;

                } catch (NoSuchMethodException e) {
                    Container c = (Container)parent;
                    if (c.isChild(name)) {
                        // known existing leaf
                        // will be handled by endElement code
                        return null;
                    }
                    // It's an unknown container or child
                    // FIXME - check capas
                    if (!RevisionInfo.newerRevisionSupportEnabled) {
                        throw new ConfMException(
                            ConfMException.ELEMENT_MISSING,
                            parent.getPath(name)+": Unexpected element");
                    }
                    parser.unknownLevel = 1;
                    return null;

                } catch (Exception e2) {
                    throw new ConfMException(
                        ConfMException.ELEMENT_MISSING,
                        parent.getPath(name)+": Unexpected element");
                }
            }
            else { // Container is aware but parent is not
                   // This is the case where we stop parsing
                   // the NETCONF rpc data and start to create
                   // ConfM objects instead
                try {
                    String className =
                        pkg +"."+normalize(name);
                    Class rootClass = Class.forName(className);
                    Element child = (Element)rootClass.newInstance();
                    parent.addChild(child);
                    return child;
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new ConfMException(
                        ConfMException.ELEMENT_MISSING,
                        parent.getPath(name)+": Unexpected element");
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                    throw new ConfMException(
                        ConfMException.ELEMENT_MISSING,
                        parent.getPath(name)+": Unexpected element");
                }

            }

        }
    }


    /**
     * setLeafValue is a data model aware method to
     * set the leaf value of the specified leaf of
     * this container.
     *
     */
    public void setLeafValue(String ns, String name, String value)
        throws ConfMException, INMException {

        // Aware
        String methodName = "set"+normalize(name)+"Value";
        Class[] types = new Class[] {
            String.class
        };
        try {
            Method setLeafValue =
                getClass().getMethod(methodName,types);
            Object[] args = {value};
            setLeafValue.invoke(this,args);
        }
        catch (NoSuchMethodException e) {
            if (!RevisionInfo.newerRevisionSupportEnabled) {
                //e.printStackTrace();
                throw new ConfMException(
                    ConfMException.ELEMENT_MISSING,
                    getPath(name)+": Unexpected element");
            }
            NodeSet nodes = get(name);
            if (nodes.size() == 0) {
                Element leaf = new Element(ns, name);
                leaf.setValue(value);
                insertLast(leaf);
            }
            else {
                Element leaf = (Leaf)nodes.first();
                leaf.setValue(value);
            }
        }
        catch (java.lang.reflect.InvocationTargetException cm) {
            // case with added enumerations,
            if (!RevisionInfo.newerRevisionSupportEnabled) {
                throw new ConfMException(
                    ConfMException.BAD_VALUE,
                    getPath(name)+
                    ": "+cm.getCause().toString());
            }

            NodeSet nodes = get(name);
            if (nodes.size() == 0) {
                Element leaf = new Element(ns, name);
                leaf.setValue(value);
                insertLast(leaf);
            }
            else {
                Element leaf = (Leaf)nodes.first();
                leaf.setValue(value);
            }
        }


        catch (Exception invErr) {
            // type error
            throw new ConfMException(ConfMException.BAD_VALUE,
                                     getPath(name)+
                                     ": "+invErr.getCause().toString());
        }
    }


    static class Package {
        String pkg;
        String ns;
        Package(String ns,String pkg) {
            this.ns= ns;
            this.pkg= pkg;
        }
    }


    /**
     * Static list of packages.
     *
     */
    static ArrayList packages =  new ArrayList();


    /**
     * Locate package from Namespace.
     *
     * @return Package name, if namespace is data model aware
     */
    public static String hasPackage(String ns) {
        if (packages==null) return null;
        for (int i=0;i<packages.size();i++) {
            Package p = (Package) packages.get(i);
            if (p.ns.equals(ns)) return p.pkg;
        }
        return null;
    }

    /**
     * Assiciate a JAVA package with a namespace.
     */
    public static void setPackage(String ns, String pkg) {
        if (packages==null) packages = new ArrayList();
        removePackage(ns);
        packages.add( new Package(ns,pkg) );
    }

    /**
     * Remove a package from the list of Packages
     */
    public static void removePackage(String ns) {
        if (packages==null) return;
        for (int i=0;i<packages.size();i++) {
            Package p = (Package) packages.get(i);
            if (p.ns.equals(ns)) {
                packages.remove(i);
                return;
            }
        }
    }


    /**
     */
    private static String normalize(String s) {
        if (isReserved(s))
            s = "j"+s;

        int pos;

        while ((pos = s.indexOf('-')) != -1)
            s = s.substring(0, pos)+capitalize(s.substring(pos+1));
        return capitalize(s);
    }

    /**
     */
    private static boolean isReserved(String s) {
        for (int i = 0; i < reservedWords.length; i++)
            if (reservedWords[i].equals(s))
                return true;

        return false;
    }

    /**
     */
    private static String capitalize(String s) {
        if (s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase()+s.substring(1);
    }



    // ------------------------------------------------------------



    protected void setLeafValue(String ns, String path, Object value,
                                String[] childrenNames)
        throws INMException {
        NodeSet nodes = get(path);

        if (nodes.size() == 0) {
            Leaf leaf = new Leaf(ns, path);
            leaf.setValue(value);
            insertChild(leaf, childrenNames);
        } else {
            Leaf leaf = (Leaf)nodes.first();
            leaf.setValue(value);
        }
    }

    protected void setLeafListValue(String ns, String path, Object value,
                                    String[] childrenNames)
        throws INMException {
        NodeSet nodes = get(path);
        Leaf leaf = new Leaf(ns, path);
        leaf.setValue(value);
        insertChild(leaf, childrenNames);
    }

    protected boolean isLeafDefault(String path)
        throws INMException {

        NodeSet nodes = get(path);

        if (nodes.size() == 0)
            return true;
        else
            return false;
    }

    protected void markLeafReplace(String path) throws INMException {
        NodeSet nodes = get(path);

        if (nodes.size() == 0)
            throw new ConfMException(ConfMException.ELEMENT_MISSING,
                                     getPath(path));
        else
            nodes.first().markReplace();
    }

    protected void markLeafMerge(String path) throws INMException {
        NodeSet nodes = get(path);
        if (nodes.size() == 0)
            throw new ConfMException(ConfMException.ELEMENT_MISSING,
                                     getPath(path));
        else
            nodes.first().markMerge();
    }

    protected void markLeafCreate(String path) throws INMException {
        NodeSet nodes = get(path);

        if (nodes.size() == 0)
            throw new ConfMException(ConfMException.ELEMENT_MISSING,
                                     getPath(path));
        else
            nodes.first().markCreate();
    }

    protected void markLeafDelete(String path) throws INMException {
        NodeSet nodes = get(path);

        if (nodes.size() == 0)
            throw new ConfMException(ConfMException.ELEMENT_MISSING,
                                     getPath(path));
        else
            nodes.first().markDelete();
    }


    /**
     *
     */
    protected Container getListContainer(String path) throws INMException {
        NodeSet nodes = get(path);
        if (nodes.size() == 0)
            throw new ConfMException(ConfMException.ELEMENT_MISSING,
                                     getPath(path));
        else
            return (Container)nodes.first();
    }



    /**
     * Given two (YANG) list entries - compare
     * the keys and return true if both YANG objects are the same
     */

    public boolean keyCompare(Container b) {

        // check that namespace and name and value are the same.
        if (! equals(b))
            return false;
        String[] keys = keyNames();
        if (keys == null)
            // not a list entry
            return false;
        for (int i=0;i<keys.length;i++) {
            Element x = (Element) getChild(keys[i]);
            Element bx = (Element)b.getChild(keys[i]);
            if ( ! x.equals( bx ))
                return false;
        }
        return true;
    }



    /**
     * Compare the contents of this container
     * toward another container.
     * Compares children values.
     * Returns:
     * <ul>
     * <li> 0 - if the two containers children are equal.
     * <li> -1 - if the two containers keys are not equal,
     *  which means that they are completely different.
     * <li> 1 - the two containers are the same except
     * the non-key children differs.
     * </ul>
     *
     * @param b Container to compare against.
     */
    public int compare(Container b) {

        // check that namespace and name and value are the same.
        if (! equals(b))
            return -1;

        String[] keys = keyNames();

        int i=0;
        if (keys!=null)
            for (i=0;i<keys.length;i++) {
                Element x = (Element) getChild(keys[i]);
                Element bx = (Element)b.getChild(keys[i]);
                if ( ! x.equals( bx ))
                    return -1;
            }
        String[] names = childrenNames();
        /* continue from 'i', since we believe that the
         * keys are the first in the childrenNames list.
         */

        for (;i<names.length;i++) {
            NodeSet nsA = getChildren(names[i]);
            NodeSet nsB = b.getChildren(names[i]);

            int hits = 0;
            for (int ii=0; ii<nsA.size(); ii++) {
                Element cA = (Element)nsA.get(ii);
                // Now does this elem exist in nsB
                for (int j=0; j<nsB.size(); j++) {
                    Element cB = (Element)nsB.get(j);
                    if (cA.equals(cB)) {
                        hits++;
                        break;
                    }
                }
            }
            if ((nsA.size() != nsB.size()) ||
                nsA.size() != hits)
                return 1;
        }
        return 0;
    }



    /**
     * Compare the contents of this container
     * toward another container.
     * Compares children values.
     * Returns:
     * <ul>
     * <li> 0 - if the two containers children are equal.
     * <li> -1 - if the two containers keys are not equal,
     *  which means that they are completely different.
     * <li> 1 - the two containers are the same except
     * the non-key children differs.
     * </ul>
     *
     * @param b Container to compare against.
     */
    public int compare(Element b) {
        if (b instanceof Container)
            return compare( (Container)b);
        return super.compare(b);
    }


    /**
     * Return the 'diff' between two trees.
     * Three empty NodeSets should be provided and
     * nodes that differ in the compare method
     * are put in one of the three resulting nodesets.
     * <ul>
     * <li>
     * Elements that are unique to node tree A will be
     * put in the uniqueA nodeset.
     * <li>
     * Elements that are unique to node tree B will be
     * put in the uniqueB NodeSet.
     * <li>
     * Dynamic elements that differ, but have the same
     * keys are put in the third nodeset 'changed'.
     * </ul>
     * if the two trees are identical the three returned
     * nodesets will be empty.
     * <p>
     * Attributes are not included in the inspection.
     * Only container structures and leaf-values
     * are checked.
     * <p>
     * Note that both subtrees must have a common starting point
     * Container in order to compare them.
     * <p>
     * @param a Subtree A (Container)
     * @param b Subtree B (Container)
     * @param uniqueA Resulting nodeset for elements that are unique to A.
     * @param uniqueB Resulting nodeset for elements that are unique to B.
     * @param changedA Resulting nodeset for changed elements.
     * A version in this nodeset.
     * @param changedB Resulting nodeset for changed elements.
     * B version in this nodeset.
     * @param the result nodeset that we wish to merge
     */

    public static void inspect(Container a, Container b,
                               NodeSet uniqueA, NodeSet uniqueB,
                               NodeSet changedA, NodeSet changedB) {
        int res= a.compare(b);
        if (res>=0) {
            // Containers are equal, go through the children.
            // Algoritm: for each child in 'a' children
            // compare against each child in b children.

            // 1.prepare: put the b children in bList.
            NodeSet bList = new NodeSet();
            if (b.children != null)
                for (int i=0;i<b.children.size();i++)
                    bList.add( b.children.getElement(i));

            // 2. for each child in a check against child in b.
            if (a.children !=null)
                for(int i=0;i<a.children.size();i++) {
                    Element aChild =  a.children.getElement(i);
                    // find it in b
                    int j=0;
                    boolean bFound= false;
                    int bRes= 0;
                    Element bChild = null;
                    while( j < bList.size() && bFound==false) {
                        bChild =  bList.getElement(j);
                        bRes = aChild.compare( bChild );
                        if (bRes >= 0)
                            bFound= true;
                        else
                            j++;
                    }
                    // remove it, if found
                    if (bFound) {
                        bList.remove(j);
                        if (bRes == 1) { // different content
                            changedA.add( aChild );
                            changedB.add( bChild );
                        } else { // bRes == 0 , they are equal
                            if (aChild instanceof Container)
                                // inspect recursively
                                Container.inspect((Container)aChild,
                                                  (Container)bChild,
                                                  uniqueA,uniqueB,
                                                  changedA, changedB);
                            else {
                                // skip if Leafs. They are equal
                            }
                        }
                    } else { // not found
                        uniqueA.add( aChild );
                    }
                }
            // 3. For the remaining in bList add them to uniqueB
            for (int i=0;i<bList.size();i++) {
                Element bChild = bList.getElement(i);
                uniqueB.add( bChild );
            }
        } else if (res==-1) {
            // A and B are completely different
            uniqueA.add(a);
            uniqueB.add(b);
        } else if (res==1) {
            // A and B is same but data is updated
            changedA.add(a);
            changedB.add(b);
        }
    }









    /**
     * Checks if two configurations are equal,
     * or if a sync is needed.
     * @return 'true' if both trees are equal. 'false' otherwise.
     */
    public boolean checkSync(Container b)  {
        return checkSync(this,b);
    }


    /**
     * Checks if two configurations are equal,
     * or if a sync is needed.
     * @return 'true' if both trees are equal. 'false' otherwise.
     */
    public static boolean checkSync(NodeSet a, NodeSet b)  {
        DummyContainer aDummy = new DummyContainer("DUMMY", "dummy");
        DummyContainer bDummy = new DummyContainer("DUMMY", "dummy");
        int i;
        for (i=0; i<a.size(); i++)
            aDummy.addChild((Element)a.get(i));
        for (i=0; i<b.size(); i++)
            bDummy.addChild((Element)b.get(i));

        return Container.checkSync(aDummy, bDummy);
    }


    /**
     * Checks if two configurations are equal,
     * or if a sync is needed.
     * @return 'true' if both trees are equal. 'false' otherwise.
     */
    public static boolean checkSync(Container a, Container b) {

        int res= a.compare(b);
        if (res==0) {
            // Containers are equal, go through the children.
            // Algoritm: for each child in 'a' children
            // compare against each child in b children.

            // 1.prepare: put the b children in bList.
            NodeSet bList = new NodeSet();
            if (b.children != null)
                for (int i=0;i<b.children.size();i++)
                    bList.add( b.children.getElement(i));

            // 2. for each child in a check against child in b.
            if (a.children !=null)
                for(int i=0;i<a.children.size();i++) {
                    Element aChild =  a.children.getElement(i);
                    // find it in b
                    int j=0;
                    boolean bFound= false;
                    int bRes= 0;
                    Element bChild = null;
                    while( j < bList.size() && bFound==false) {
                        bChild =  bList.getElement(j);
                        bRes = aChild.compare( bChild );
                        if (bRes >= 0)
                            bFound= true;
                        else
                            j++;
                    }
                    // remove it, if found
                    if (bFound) {
                        bList.remove(j);
                        if (bRes == 1) { // different content
                            return false;
                        } else { // res == 0 , they are equal
                            if (aChild instanceof Container) {
                                // inspect recursively
                                if (!checkSync((Container)aChild,
                                               (Container)bChild))
                                    return false;
                            } else {
                                // skip if Leafs. They are equal
                            }
                        }
                    } else { // not found
                        return false;
                    }
                }
            // 3. Any remaining in bList ?
            if (bList.size()>0)
                return false;

        } else if (res==-1) {
            // A and B are completely different
            return false;
        } else if (res==1) {
            // A and B is same but data is updated
            return false;
        }
        return true;
    }


    /**
     * Will return a subtree for syncing a subtree A with all the
     * necessary operations to make it look like the target tree B.
     *
     * @return Return subtree with operations to transmute subtree A
     * into subtree B.
     */
    public Container sync(Container b) throws INMException {
        return Container.sync(this, b);
    }

    /**
     * Will return a subtree for syncing a subtree A with all the
     * necessary operations to make it look like the target tree B.
     * This verion of sync will produce a NETCONF tree with
     * NETCONF replace operations where all list entries that
     * differ are replaced with a new list entry
     * using "nc:operation="replace". An alternative  (and better)
     * method is "merge". The method {@link #syncMerge} produces
     * a NETCONF tree with the merge operation instead.
     * @return Return subtree with operations to transmute subtree A
     * into subtree B.
     */

    public static Container sync(Container a, Container b) throws INMException {

        NodeSet uniqueA = new NodeSet();
        NodeSet uniqueB = new NodeSet();
        NodeSet changedA = new NodeSet();
        NodeSet changedB = new NodeSet();

        Container.inspect(a,b,uniqueA,uniqueB,changedA,changedB);


        Element result = null;
        for(int i=0;i<uniqueA.size();i++) {
            Element x= uniqueA.getElement(i);
            result = x.merge(result, OP_DELETE);
        }

        for (int i=0;i<uniqueB.size();i++) {
            Element x= uniqueB.getElement(i);
            result= x.merge(result, OP_CREATE);
        }

        for (int i=0;i<changedB.size();i++) {
            Element x= changedB.getElement(i);
            result = x.merge(result, OP_REPLACE);
        }


        return (Container) result;
    }



    /**
     * Will return a list of subtrees for syncing a subtree A with all the
     * necessary operations to make it look like the target tree B.
     * This variant uses the NETCONF merge operation
     * @return Return subtrees with operations to transmute subtree A
     * into subtree B.
     */

    public static NodeSet syncMerge(NodeSet a, NodeSet b) throws INMException {
        DummyContainer aDummy = new DummyContainer("DUMMY", "dummy");
        DummyContainer bDummy = new DummyContainer("DUMMY", "dummy");
        int i;
        for (i=0; i<a.size(); i++)
            aDummy.addChild((Element)a.get(i));
        for (i=0; i<b.size(); i++)
            bDummy.addChild((Element)b.get(i));

        Container result = Container.syncMerge(aDummy, bDummy);
        return result.getChildren();
    }



    /**
     * Will return a subtree for syncing a subtree A with all the
     * necessary operations to make it look like the target tree B.
     * This version of sync will produce a NETCONF tree with
     * NETCONF merge operations.
     * @return Return subtree with operations to transmute subtree A
     * into subtree B.
     */


    public static Container syncMerge(Container a, Container b) {
        Container copy = (Container)b.clone();
        NodeSet toDel = new NodeSet();
        Container.csync2((Container)a.clone(), copy, false, toDel);
        for (int i=0; i<toDel.size(); i++) {
            Element e = (Element)toDel.get(i);
            e.getParent().deleteChild(e);
        }
        return copy;
    }


    // Which NETCONF do we need to produce in order to go
    // from a to b
    // returns number of difss
    private static int csync2(Container a, Container b,
                                    boolean listEntry,
                                    NodeSet toDel) {
        NodeSet bchildren = b.getChildren();
        NodeSet achildren = a.getChildren();
        int i;
        int diffs = 0;
        int d;
        for (i =0; bchildren != null && i<bchildren.size(); i++) {
            Element bChild = (Element)bchildren.get(i);
            if (listEntry) {
                // inside list entries we ignore keys
                if ((bChild instanceof Leaf)) {
                    Leaf leaf = (Leaf)bChild;
                    if (leaf.isKey())
                        continue;
                }
            }

            Element aChild = null;

            if (achildren != null)
                aChild = Container.findDeleteChild(bChild, achildren);

            if (aChild == null) {
                // It's a new child that needs to be merged
                diffs++;
                continue;
            }

            // It was found - and it was also deleted from a

            if ((aChild instanceof Container) &&
                (((Container)aChild).keyNames()  == null)) {
                // regular containers
                d = Container.csync2((Container)aChild, (Container)bChild,
                                     false, toDel);
                diffs += d;
                if (d == 0) {


                    // both children are identical - remove
                    // from b as well
                    toDel.add(bChild);
                }
                continue;
            }
            if (aChild instanceof Container) {
                // It's a list entry - and the keys
                // in aChild and bChild are equal
                d = Container.csync2((Container)aChild, (Container)bChild,
                                true, toDel);
                diffs += d;
                if (d == 0) {
                    // both children are identical - remove
                    // from b as well
                    toDel.add(bChild);
                }

                continue;
            }


            if (aChild instanceof Leaf) {
                if (aChild.equals(bChild)) {
                    // identical leaves remove from b
                    // no need to send it
                    toDel.add(bChild);
                }
                else {
                    diffs++;
                }
            }
        }
        // Now all remaining elements in a - need to be
        // marked as delete, and also subsequently moved
        // to b


        for (i =0; achildren != null && i<achildren.size(); i++) {
            Element x = (Element)achildren.get(i);
            if (x instanceof Leaf) {
                Leaf leaf = (Leaf)x;
                if (leaf.isKey()) {
                    // ignore key leafs - they're handled elsewhere
                    continue;
                }
                diffs++;
                b.addChild(x);
                x.markDelete();
            }

            // If it's a list entry - remove all children
            // but the keys
            // If it's a container, remove all children
            else if (x instanceof Container) {
                diffs++;
                Container c = (Container)x;
                String[] keys = c.keyNames();
                 Container n = (Container)c.cloneShallow();
                 b.addChild(n);
                 n.markDelete();
            }
        }
        return diffs;
    }

    private static Element findDeleteChild(Element e, NodeSet s) {
        if (s == null)
            return null;
        if (e instanceof Leaf)
            return findDeleteChildLeaf((Leaf) e, s);
        else if (e instanceof Container)
            return findDeleteChildContainer((Container)e, s);
        else {
            // It's the case where we receive Elements
            // instead of Container/Leaf because the device
            // has a newer revision  than us. This code can
            // never be made to work perfect.
            return findDeleteChildElement(e, s);
            /*
            // this should never occur - throw error
            String str = e.toXMLString();
            throw new ConfMException(ConfMException.NOT_CONFM_OBJECT,str);
            */
        }
    }

    private static Element findDeleteChildElement(Element e, NodeSet s) {
        for (int i =0; i<s.size(); i++) {
            Element x = (Element)s.get(i);
            if (x.compare(e) >= 0 ) {
                s.remove(i);
                return x;
            }
        }
        return null;
    }


    private static Element findDeleteChildContainer(Container e, NodeSet s) {
        String [] keys = e.keyNames();
        for (int i =0; i<s.size(); i++) {
            Element x = (Element)s.get(i);
            if (x instanceof Leaf)
                continue;
            Container c = (Container) x;
            if (keys == null) {
                // plain container
                if (e.equals(x)) {
                    s.remove(i);
                    return c;
                }
            }
            else if (e.keyCompare(c)) {
                s.remove(i);
                return c;
            }
        }
        return null;
    }

    private static Element findDeleteChildLeaf(Leaf e, NodeSet s) {
        for (int i =0; i<s.size(); i++) {
            Element x = (Element)s.get(i);
            if (x instanceof Leaf) {
                if (x.compare(e) >= 0) {
                    s.remove(i);
                    return x;
                }
            }
        }
        return null;
    }








    /**
     * Will return a list of subtrees for syncing a subtree A with all the
     * necessary operations to make it look like the target tree B.
     *
     * @return Return subtrees with operations to transmute subtree A
     * into subtree B.
     */

    public static NodeSet sync(NodeSet a, NodeSet b) throws INMException {
        DummyContainer aDummy = new DummyContainer("DUMMY", "dummy");
        DummyContainer bDummy = new DummyContainer("DUMMY", "dummy");
        int i;
        for (i=0; i<a.size(); i++)
            aDummy.addChild((Element)a.get(i));
        for (i=0; i<b.size(); i++)
            bDummy.addChild((Element)b.get(i));

        Container result = Container.sync(aDummy, bDummy);
        return result.getChildren();
    }



    /**
     * Clones a container.
     * Only key children are cloned, the other children
     * are skipped.
     * Attributes and values are cloned.
     *
     */
    protected abstract Element cloneShallow();


    /**
     * Clones the contents of this container into a target copy.
     * All content is copied except children (shallow).
     * <p>
     * Note: Used by the generated ConfM classes
     * The key children are already cloned.
     *
     * @param copy The target copy to clone the contents to
     */
    protected Container cloneShallowContent(Container copy) {
        cloneAttrs(copy);
        return copy;
    }


    /**
     * Clones the content of this container into a target copy.
     * Key children are not copied.
     * <p>
     * Note: Used by the generated ConfM classes
     * The key children are already cloned.
     *
     * @param copy The target copy to clone the contents into
     */
    protected Container cloneContent(Container copy) {
        // copy children, except keys which are already copied
        if (children!= null) {
            String[] childrenNames= childrenNames();
            String[] keyNames = keyNames();
            if (keyNames!=null) {
                int i = keyNames.length;
                // this is better. even correct.
                for (;i<children.size();i++){
                    Element child = children.getElement(i);
                    Element child_copy = (Element) child.clone();
                    copy.addChild( child_copy );
                }
                /*
                  for (;i< childrenNames.length;i++) {
                  String childName = childrenNames[i];
                  Element child = getChild(childName);
                  if (child!=null) {
                  Element child_copy= (Element) child.clone();
                  copy.addChild( child_copy );
                  }
                  }
                */
            } else {
                // copy all instead.
                if (children!=null)
                    for (int i=0;i<children.size();i++){
                        Element child = children.getElement(i);
                        Element child_copy = (Element) child.clone();
                        copy.addChild( child_copy );
                    }
            }
        }

        // clone attrs and value
        cloneAttrs(copy);
        cloneValue(copy);
        return copy;
    }

    /**
     * Read file with XML text and parse it into
     * a configuration tree.
     *
     * @param filename File name.
     * @see #writeFile(String)
     */
    public static Element readFile(String filename)
        throws INMException {
        XMLParser p = new com.tailf.confm.XMLParser();
        return p.readFile(filename);
    }



    // cache the Tagpath and the CsNode
    private Tagpath tp  = null;
    private CsNode n = null;
    protected void encode(Transport out, boolean newline_at_end,
                          Capabilities capas) throws INMException  {
        if (RevisionInfo.olderRevisionSupportEnabled && capas != null) {
            if (tp == null)
                tp = tagpath();
            String actualNamespace = getRootElement().namespace;
            if (n == null)
                n = CsTree.lookup(actualNamespace, tp);
            String rev = capas.getRevision(actualNamespace);
            if (n.revInfo != null) {
                for (int i = 0; i<n.revInfo.length; i++) {
                    RevisionInfo r = n.revInfo[i];
                    if (r.introduced.compareTo(rev) > 0) {
                        // This node was somehow modified
                        //System.out.println("REVINFO " + r.type);
                        switch(r.type) {
                        case RevisionInfo.R_MAX_ELEM_RAISED:
                            int max = r.idata;
                            if (getChildren().size() > max) {
                                throw new INMException(
                                    INMException.REVISION_ERROR,
                                    tp + "too many children for old node " +
                                    "with rev( " + rev + ")");
                            }
                            break;
                        case RevisionInfo.R_MIN_ELEM_LOWERED:
                            int min = r.idata;
                            // naah we can't do anything here
                            break;
                        case RevisionInfo.R_NODE_ADDED:
                            //System.out.println("Dropping "+this.toXMLString());
                            return;
                        default:
                            break;
                        }
                    }
                }
            }
        }
        super.encode(out, newline_at_end, capas);
    }

    public boolean isChild(String childName) {
        String[] children = childrenNames();
        for (int i=0; i<children.length; i++) {
            if (childName.equals(children[i]))
                return true;
        }
        return false;
    }

    // Debug method to check that there are no Elements
    // inside a Container
    static public boolean isNotElem(Container c) {
        NodeSet s = c.getChildren();
        if (s == null) return true;
        for (int i=0; i<s.size(); i++) {
            Element e = (Element)s.get(i);
            if (e instanceof Leaf)
                return true;
            else if (e instanceof Container) {
                Container cc = (Container)e;
                if (!Container.isNotElem(cc)) {
                    System.out.println("Element " + e.toXMLString());
                    return false;
                }
            }
            else {
                System.out.println("Element " + e.toXMLString());
                return false;
            }
        }
        return true;
    }


}