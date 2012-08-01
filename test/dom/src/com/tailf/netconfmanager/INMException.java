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

package com.tailf.netconfmanager;

/**
 * Exception class for errors produced by this library.
 * <p>
 * INMException uses an errorCode field to indicate what went wrong. Depending
 * on errorCode an opaqueData field may point to contextual information
 * describing the error. The toString method uses both of these fields to print
 * an appropriate error string describing the error.
 */
public class INMException extends Exception {
    public INMException() {
        this(NOT_SET, null);
    }

    public INMException(int errorCode) {
        this(errorCode, null);
    }

    public INMException(int errorCode, Object opaqueData) {
        this.errorCode = errorCode;
        this.opaqueData = opaqueData;
        if (errorCode == RPC_REPLY_ERROR) {
            // set rpcErrors array to the returned errors
            Element t = (Element) opaqueData;
            try {
                NodeSet errors = t.get("self::rpc-reply/rpc-error");
                if (errors != null) {
                    rpcErrors = new RpcError[errors.size()];
                    for (int i = 0; i < errors.size(); i++) {
                        rpcErrors[i] = new RpcError((Element) errors.get(i));
                    }
                }
            } catch (Exception e) {
                System.err.println("rpc-error can't be parsed: "
                        + t.toXMLString());
                e.printStackTrace();
            }
        }
    }

    /**
     * An error code indicating what went wrong. This field may take any of
     * these values:
     * <ul>
     * <li> {@link #NOT_SET}
     * <li> {@link #AUTH_FAILED}
     * <li> {@link #NODE_ERROR}
     * <li> {@link #PATH_ERROR}
     * <li> {@link #PATH_CREATE_ERROR}
     * <li> {@link #PARSER_ERROR}
     * <li> {@link #RPC_REPLY_ERROR}
     * <li> {@link #SESSION_ERROR}
     * <li> {@link #ELEMENT_ALREADY_IN_USE}
     * <li> {@link #ELEMENT_MISSING}
     * <li> {@link #NOTIFICATION_ERROR}
     * <li> {@link #TIMEOUT_ERROR}
     * <li> {@link #REVISION_ERROR}
     * </ul>
     * <p>
     * Depending on the value the opaqueData field may be set accordingly. If so
     * this is described below for each possible value.
     */
    public int errorCode = NOT_SET;

    /**
     * Contextual information describing the error. The meaning of this field as
     * is described for each possible errorCode value.
     */
    public Object opaqueData = null;

    /**
     * If errorCode is RPC_REPLY_ERROR the rpc-error is parsed and the rpcErrors
     * array will contain the returned error information.
     */
    public RpcError[] rpcErrors;

    public static final int NOT_SET = 0;
    public static final int AUTH_FAILED = -1;
    public static final int NODE_ERROR = -2;
    public static final int PATH_ERROR = -3;
    public static final int PATH_CREATE_ERROR = -4;
    public static final int PARSER_ERROR = -5;

    /**
     * An <code>rpc-reply</code> with one or more <code>reply-error</code>
     * elements was returned from the device. The opaqueData field contains the
     * error (in XML format) returned from the device.
     */
    public static final int RPC_REPLY_ERROR = -6;

    /**
     * A session error occured. The opaqueData field is a string describing the
     * error.
     */
    public static final int SESSION_ERROR = -7;

    /**
     * This element has already been used. The opaqueData field is the offending
     * Element object.
     */
    public static final int ELEMENT_ALREADY_IN_USE = -8;

    /**
     * This element does not exist. The opaqueData field is a path pointing to
     * the missing element.
     */
    public static final int ELEMENT_MISSING = -9;

    /**
     * Notification error. The received message was not a notification.
     */
    public static final int NOTIFICATION_ERROR = -10;

    /**
     * Timeout error. The timeout specified when creating the SSHSession object
     * expired.
     */
    public static final int TIMEOUT_ERROR = -11;

    /**
     * Revision error. When we tried to encode a configuration tree in order to
     * send it to a device - A revision error was encountered. This only applies
     * to ConfM code. A revision error occurs when we try to encode something
     * the receiving node doesn't understand, such as an enumeration/bit value
     * the node cannot understand
     */
    public static final int REVISION_ERROR = -12;

    /**
     * Message-id mismatch. The message-id attribute in the rpc-reply we
     * received from the Netconf server didn't match the message-id in the rpc
     * we sent.
     */
    public static final int MESSAGE_ID_MISMATCH = -13;

    /**
     * The toString method uses both of errorCode and opaqueData fields to
     * generate an appropriate error string.
     */

    public String toString() {
        switch (errorCode) {
        case NOT_SET:
            return "Error reason not specified";
        case AUTH_FAILED:
            return "Authentication failed";
        case NODE_ERROR:
            return "Unknown error";
        case PATH_ERROR:
            return "Error in path: " + opaqueData;
        case PATH_CREATE_ERROR:
            return "Error in create path: " + opaqueData;
        case PARSER_ERROR:
            return "Parse error: " + opaqueData;
        case RPC_REPLY_ERROR:
            if (opaqueData != null)
                return "rpc-reply error: "
                        + ((Element) opaqueData).toXMLString();
            else
                return "rpc-reply error";
        case SESSION_ERROR:
            return "Session error: " + opaqueData;
        case ELEMENT_ALREADY_IN_USE:
            return "Element has already been used: "
                    + ((Element) opaqueData).name;
        case ELEMENT_MISSING:
            return "Element does not exists: " + opaqueData;
        case NOTIFICATION_ERROR:
            return "Notification error: " + opaqueData;
        case TIMEOUT_ERROR:
            return "Timeout error: " + opaqueData;
        case REVISION_ERROR:
            return "Revision error: " + opaqueData;
        case MESSAGE_ID_MISMATCH:
            return "Message ID mismatch: " + opaqueData;
        default:
            return "Internal error: " + errorCode;
        }
    }
}