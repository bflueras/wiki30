package org.xwiki.gwt.wysiwyg.client.plugin.rt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

/**
 * Utility class used for controlling the behaviour of the realtime wysiwyg editor.
 *
 * @author Bogdan.Flueras@inria.fr
 */
public final class EditorUtils
{
    public static final String NON__EMPTY = "NON_EMPTY";

    public static final String EMPTY = "EMPTY";

    /**
     * The caret has to be deterministically positioned (within a paragraph), it is always located as follows:
     * <ul>
     *     <li>at the last position of the previous non-empty text-node within the same paragraph if such a previous text-node exists.</li>
     *     <li>if not, at the first position of the next non-empty text-node relative to the locator within the same paragraph.</li>
     *     <li>if there are no non-empty text nodes (all text nodes are empty) then the caret moves to the first empty text node either at left or at right
     * </ul>
     * @param oldCaretPosition the old caret position
     * @return the new caret position, as described above
     */
    public static Range normalizeCaretPosition(Range oldCaretPosition)
    {
        Range newCaretPos = oldCaretPosition.cloneRange();
        if (oldCaretPosition.isCollapsed()) {
            // if I'm in a non-empty text node, return the oldCaretPosition
            if (isNonEmptyTextNode(oldCaretPosition.getStartContainer()) && oldCaretPosition.getStartOffset() > 0) {
                return oldCaretPosition;
            } else {
                // go left whenever possible, if not go right
                Map<String, List<Text>> leftTextNodesInSameP = getLeftTextNodesInSameP(oldCaretPosition);
                List<Text> leftNonEmpty = leftTextNodesInSameP.get(NON__EMPTY);
                int leftSize = leftNonEmpty.size();
                if (leftSize > 0) {
                    Text firstLeftTextNode = leftNonEmpty.get(leftSize - 1);
                    newCaretPos.setStart(firstLeftTextNode, firstLeftTextNode.getLength());
                } else {
                    Map<String, List<Text>> rightTextNodesInSameP = getRightTextNodesInSameP(oldCaretPosition);
                    List<Text> rightNonEmpty = rightTextNodesInSameP.get(NON__EMPTY);
                    if (rightNonEmpty.size() > 0) {
                        Text firstRightTextNode = rightNonEmpty.get(0);
                        newCaretPos.setStart(firstRightTextNode, 0);
                    } else { //no left or right non-empty text nodes, so go to first text child (which is empty)
                        List<Text> emptyTextNodes = leftTextNodesInSameP.get(EMPTY);
                        Text emptyText = null;
                        if (emptyTextNodes.size() > 0) {
                            emptyText = emptyTextNodes.get(0);
                        } else {
                            emptyTextNodes = rightTextNodesInSameP.get(EMPTY);
                            if (emptyTextNodes.size() > 0) {
                                emptyText = emptyTextNodes.get(0);
                            }
                        }
                        if (emptyText != null) {
                            newCaretPos.setStart(emptyText, 0);
                        }
                    }
                }
            }
            newCaretPos.collapse(true);
        }
        return newCaretPos;
    }

    /**
     * Converts a DOM range to an list of operation targets.   TODO: remove it and Operation target too
     *
     * @param range a DOM range
     * @return the corresponding list of operation targets
     */
    public static List<OperationTarget> getIntermediaryTargets(Range range) {
        // Iterate through all the text nodes within the given range and extract the operation target
        List<OperationTarget> operationTargets = new ArrayList<OperationTarget>();
        // Create the intermediary targets backwards because if we preserve the normal order when we modify the tree,
        // the following targets will no longer reflect that
        List<Text> textNodes = getTextNodes(range).get(NON__EMPTY);
        for (int i = 0; i < textNodes.size(); i++) {
            Text text = textNodes.get(i);
            int startIndex = 0;
            if (text == range.getStartContainer()) {
                startIndex = range.getStartOffset();
            }
            int endIndex = text.getLength();
            if (text == range.getEndContainer()) {
                endIndex = range.getEndOffset();
            }
            operationTargets.add(0, new OperationTarget(getLocator(text), startIndex, endIndex, text.getLength()));
        }
        return operationTargets;
    }

    /**
     * Converts a DOM range to a list of simple ranges.
     * A simple range spans across a single text node
     *
     * @param range a DOM range
     * @return the corresponding list of simple ranges
     */
    public static List<Range> getIntermediaryRanges(Range range) {
        // Iterate through all the text nodes within the given range and extract the simple ranges
        List<Range> simpleRanges = new ArrayList<Range>();
        // Create the simple ranges backwards because if we preserve the normal order when we modify the tree,
        // the following targets will no longer reflect that
        List<Text> textNodes = getTextNodes(range).get(NON__EMPTY);
        for (int i = 0; i < textNodes.size(); i++) {
            Text text = textNodes.get(i);
            int startIndex = 0;
            if (text == range.getStartContainer()) {
                startIndex = range.getStartOffset();
            }
            int endIndex = text.getLength();
            if (text == range.getEndContainer()) {
                endIndex = range.getEndOffset();
            }

            Range simpleRange = range.cloneRange();
            simpleRange.collapse(true);
            simpleRange.setStart(text, startIndex);
            simpleRange.setEnd(text, endIndex);

            simpleRanges.add(0, simpleRange);
        }
        return simpleRanges;
    }

     /**
     * @param range a DOM range
     * @return the list of empty and non empty text nodes that are completely or partially (at least one character) included in
     *         the given range
     */
    public static Map<String, List<Text>> getTextNodes(Range range) {
        Map<String, List<Text>> textNodesMap = new HashMap<String, List<Text>>();

        Node leaf = DOMUtils.getInstance().getFirstLeaf(range);
        Node lastLeaf = DOMUtils.getInstance().getLastLeaf(range);
        List<Text> nonEmptyTextNodes = new ArrayList<Text>();
        List<Text> emptyTextNodes = new ArrayList<Text>();

        // If the range starts at the end of a text node we have to ignore that node.
        if (isNonEmptyTextNode(leaf) &&
            (leaf != range.getStartContainer() || range.getStartOffset() < leaf.getNodeValue().length())) {
            nonEmptyTextNodes.add((Text) leaf);
        }
        if (isEmptyTextNode(leaf)) {
            emptyTextNodes.add((Text) leaf);
        }
        while (leaf != lastLeaf) {
            leaf = DOMUtils.getInstance().getNextLeaf(leaf);
            if (isNonEmptyTextNode(leaf)) {
                nonEmptyTextNodes.add((Text) leaf);
            } else if (isEmptyTextNode(leaf)) {
                emptyTextNodes.add((Text) leaf);
            }
        }
        // If the range ends at the start of a text node then we have to ignore that node.
        int lastIndex = nonEmptyTextNodes.size() - 1;
        if (lastIndex >= 0 && range.getEndOffset() == 0 && nonEmptyTextNodes.get(lastIndex) == range.getEndContainer()) {
            nonEmptyTextNodes.remove(lastIndex);
        }
        textNodesMap.put(NON__EMPTY, nonEmptyTextNodes);

        textNodesMap.put(EMPTY, emptyTextNodes);

        return textNodesMap;
    }

    /**
     * @param range the selection range, usually a collapsed range which is the caret position, placed inside a paragraph
     * @return all the non-empty text nodes from the left of the caret within the same paragraph
     */
    private static Map<String, List<Text>> getLeftTextNodesInSameP(Range range)
    {
        Range leftRange = range.cloneRange();
        leftRange.setEnd(range.getStartContainer(), range.getStartOffset());
        Node parentContainer = DOMUtils.getInstance().getNearestBlockContainer(range.getStartContainer());
        leftRange.setStart(parentContainer, 0);

        return getTextNodes(leftRange);
    }

    /**
     * @param range the selection range, usually a collapsed range which is the caret position, placed inside a paragraph
     * @return all the non-empty text nodes from the right of the caret within the same paragraph
     */
    private static Map<String, List<Text>> getRightTextNodesInSameP(Range range)
    {
        Range rightRange = range.cloneRange();
        Node parentContainer = DOMUtils.getInstance().getNearestBlockContainer(range.getEndContainer());
        rightRange.setEnd(parentContainer, parentContainer.getChildCount());

        return getTextNodes(rightRange);
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is of type {@link Node#TEXT_NODE} and it's not empty, {@code false}
     *         otherwise
     */
    public static boolean isNonEmptyTextNode(Node node) {
        return isTextNode(node) && node.getNodeValue().length() > 0;
    }

    public static boolean isTextNode(Node node) {
        return node != null && node.getNodeType() == Node.TEXT_NODE;
    }

    public static boolean isEmptyTextNode(Node node) {
        return isTextNode(node) && node.getNodeValue().length() == 0;
    }

    /**
     * @param root the node where the locator is relative to
     * @param path the location/path
     * @return the child node at the locator value starting from the root node
     */
    public static Node getChildNodeFromLocator(Node root, int[] path)
    {
        Node targetNode = root;
        for (int i = 0; i < path.length; i++) {
            targetNode = targetNode.getChildNodes().getItem(path[i]);
        }
        return targetNode;
    }

    /**
     * @param node a DOM node
     * @return the path from the BODY element of the document that owns the given node to the node; a path item is the
     *         index of the corresponding note among its siblings.
     */
    public static List<Integer> getLocator(Node node)
    {
        List<Integer> locator = new ArrayList<Integer>();
        Node ancestor = node;
        while (ancestor != null && ancestor != node.getOwnerDocument().getBody()) {
            locator.add(0, DOMUtils.getInstance().getNodeIndex(ancestor));
            ancestor = ancestor.getParentNode();
        }
        return locator;
    }

    /**
     * Converts a list of {@link Integer} objects to an array of integer numbers.
     *
     * @param list the list to be converted
     * @return an array of integer numbers
     */
    public static int[] toIntArray(List<Integer> list)
    {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /**
     * @param range a DOM range
     * @return the node in the same paragraph that precedes the start point of the given range, in a depth-first pre-order search
     */
    public Node getPreviousNodeInSameParagraph(Range range)
    {
        Node node = range.getStartContainer();
        if (node.getNodeName().equalsIgnoreCase("p")) {
            return null;
        }

        if (node.hasChildNodes() && range.getStartOffset() > 0) {
            return node.getChildNodes().getItem(range.getStartOffset() - 1);
        }
        while (node != null && node.getPreviousSibling() == null) {
            node = node.getParentNode();
        }
        return node;
    }
}
