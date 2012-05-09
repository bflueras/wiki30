/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.gwt.wysiwyg.client.plugin.rt.dom.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Property;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.dom.client.Text;
import org.xwiki.gwt.dom.client.TextFragment;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.ToggleInlineStyleExecutable;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;

import fr.loria.score.jupiter.tree.operation.TreeOperation;
import fr.loria.score.jupiter.tree.operation.TreeStyle;

/**
 * Applies {@link TreeStyle} on a DOM tree.
 *
 * @version $Id$
 */
public class DomStyle extends AbstractDomOperation
{
    private static final Logger log = Logger.getLogger(DomStyle.class.getName());

    /**
     * The real executable which applies the style or creates a link to the DOM document
     */
    private DomStyleExecutable realDomExecutable;

    /**
     * {@code true} if the {@code TreeOperation} is received from server, and {@code false} if it's generated locally
     * This is the simplest solution in order to fix a bug. Otherwise I would have to modify a lot of code
     */
    private final boolean isRemote;

    /**
     * Creates a new DOM operation equivalent to the given Tree operation.
     *
     * @param operation a Tree operation
     * @param isRemote true if tree operation was remotely generated (received from server)
     */
    public DomStyle(TreeOperation operation, boolean isRemote)
    {
        super(operation);
        this.isRemote = isRemote;
    }

    public boolean isRemote()
    {
        return isRemote;
    }

    @Override
    public Range execute(Document document) {
        TreeStyle treeStyleOp = getOperation();
        String stylePropertyName = treeStyleOp.param;
        String stylePropertyValue = treeStyleOp.value;

        Property property = null;
        if (stylePropertyName.equalsIgnoreCase("font-weight")) {
            property = Style.FONT_WEIGHT;
        } else if (stylePropertyName.equalsIgnoreCase("font-style")) {
             property = Style.FONT_STYLE;
        } else if (stylePropertyName.equalsIgnoreCase("text-decoration")) {
             property = Style.TEXT_DECORATION;
        }

        // Create range object from the op context
        Range styleRange = document.createRange();
        // Target node is the same for start and end because the style op is applied sequentially on every sub-range (text node) within the original selection range
        Node targetNode = getTargetNode(document);
        styleRange.setStart(targetNode, treeStyleOp.start);
        styleRange.setEnd(targetNode, treeStyleOp.end);
        log.info("Style range: " + styleRange.toString());

        Range localRange = document.getSelection().getRangeAt(0);
        log.info("Local range: " + localRange.toString());

        // Such a dirty hack because Link is modeled as a Style with a different tag name
        if (property != null) {
            //Apply style
            realDomExecutable = new DomStyleExecutable(document, property, stylePropertyValue);
        } else {
            //Apply Link
            realDomExecutable = new DomLinkExecutable(document, property, stylePropertyValue);
        }
        return realDomExecutable.execute(styleRange, stylePropertyValue);
    }


    /**
     * If there is no selection, the insertion point will set the given style for subsequently typed characters. If there is a
     * selection and all of the characters are already styled, the style will be removed. Otherwise, all selected characters
     * will become styled.
     * <p/>
     * It would be easier to inherit directly from BoldExecutable and override just one method,
     * but this class is located into xwiki-platform-wysiwyg-client module and thus we were introducing a circular dependence.
     *
     * @version $Id: dd0a6a0520f2764164a0b938aaa5a52815febff6 $
     */
    class DomStyleExecutable extends ToggleInlineStyleExecutable {
        /**
         * The tag name, which is empty string since we use CSS styling properties
         */
        private static final String TAG_NAME = "";

        protected static final String SPAN = "span";

        /**
         * The document target
         */
        private Document document;

        /**
         * The actual range on which the style is applied
         */
        private Range styleRange;

        //todo: commit changes in gwt-user to have access to it
        protected String propertyValue;

        /**
         * Creates a new executable of this type.
         *
         * @param document the document target
         * @param propertyName the style property name
         * @param propertyValue the style property value
         */
        public DomStyleExecutable(Document document, Property propertyName, String propertyValue) {
            // We don't use the RTA but the document, and override all methods that use the RTA
            super(new RichTextArea(), propertyName, propertyValue, TAG_NAME);
            this.document = document;
            this.propertyValue = propertyValue;

        }

        @Override
        public boolean execute(String parameter) {
            Selection selection = document.getSelection();
            List<Range> ranges = new ArrayList<Range>();
            for (int i = 0; i < selection.getRangeCount(); i++) {
                ranges.add(execute(selection.getRangeAt(i), parameter));
            }
            selection.removeAllRanges();
            for (Range range : ranges) {
                selection.addRange(range);
            }
            return true;
        }

        @Override
        public Range execute(Range range, String parameter) {
            this.styleRange = range;
            return super.execute(range, parameter);
        }

        @Override
        public String getParameter() { // todo: investigate where it is used
            Selection selection = document.getSelection();
            String selectionParameter = null;
            for (int i = 0; i < selection.getRangeCount(); i++) {
                String rangeParameter = getParameter(selection.getRangeAt(i));
                if (rangeParameter == null || (selectionParameter != null && !selectionParameter.equals(rangeParameter))) {
                    return null;
                }
                selectionParameter = rangeParameter;
            }
            return selectionParameter;
        }

        @Override
        public boolean isExecuted() {
           return isExecuted(styleRange);
        }

        @Override
        protected TextFragment execute(Text text, int startIndex, int endIndex, String parameter) {
            boolean addStyle = isExecuted();
            //if operation is remote, then add style if necessary, but don't remove it
            if (DomStyle.this.isRemote()) {
                return addStyle ? new TextFragment(text, startIndex, endIndex) : addStyle(text, startIndex, endIndex);
            } else {
                // operation is local, add or remove style
                return addStyle ? removeStyle(text, startIndex, endIndex) : addStyle(text, startIndex, endIndex);
            }
        }

        /**
         * Adds the underlying style to the given text node.
         *
         * @param text           the target text node
         * @param firstCharIndex the first character on which we apply the style
         * @param lastCharIndex  the last character on which we apply the style
         * @return a text fragment indicating what has been formatted
         */
        protected TextFragment addStyle(Text text, int firstCharIndex, int lastCharIndex) {
            if (matchesStyle(text)) {
                // Already styled. Skip.
                return new TextFragment(text, firstCharIndex, lastCharIndex);
            }

            // Make sure we apply the style only to the selected text.
            text.crop(firstCharIndex, lastCharIndex);
            Element element = (Element) text.getParentElement();

            return handleTag(text, element);
        }

        //handles the SPAN tag
        protected TextFragment handleTag(Text text, Element element)
        {
            if (SPAN.equalsIgnoreCase(element.getNodeName())) {
                if (element.getChildCount() != 1) {
                    splitParentNode(text, element);
                }
                element.getStyle().setProperty(getProperty().getJSName(), propertyValue);
            } else {
                SpanElement spanElement = Document.get().createSpanElement();
                spanElement.getStyle().setProperty(getProperty().getJSName(), propertyValue);

                element.replaceChild(spanElement, text);
                spanElement.appendChild(text);
            }
            return new TextFragment(text, 0, text.getLength());
        }

        /**
         * Override because of a bug
         */
        @Override
        protected boolean matchesInheritedStyle(Element element) {
            final com.google.gwt.dom.client.Style style = element.getStyle();
            String computedValue;
            if (style != null && style.getProperty(getProperty().getJSName()).length() > 0) {
                computedValue = style.getProperty(getProperty().getJSName());
            } else {
                computedValue = element.getComputedStyleProperty(getProperty().getJSName());
            }
            if (getProperty().isMultipleValue()) {
                return computedValue != null && computedValue.toLowerCase().contains(propertyValue);
            } else {
                return propertyValue.equalsIgnoreCase(computedValue);
            }
        }

        /**
         * Splits the parent node of the text, in order to preserve styling
         * Useful when a new style is applied on a sub-selection of an already styled element.
         * We have to split the parent node to preserve initial styling
         *
         * @param text the text node, whose parent is to be split
         * @param parent the parent element to be split
         */
        private void splitParentNode(Text text, Element parent)
        {
            Node prevSibling = text.getPreviousSibling();
            if (prevSibling != null) {
                Element leftParent = Element.as(parent.cloneNode(false)); // apply the old styling
                addNodeSiblingsToNewParent(text.getPreviousSibling(), leftParent, -1);
                parent.getParentNode().insertBefore(leftParent, parent);
            }

            Node nextSibling = text.getNextSibling();
            if (nextSibling != null) {
                Element rightParent = Element.as(parent.cloneNode(false)); // apply the old styling
                addNodeSiblingsToNewParent(text.getNextSibling(), rightParent, 1);
                parent.getParentNode().insertAfter(rightParent, parent);
            }
        }

        /**
         * Remove sibling nodes from actual parent and adds them as children into a new parent node, keeping their original order
         * @param node the node whose siblings (left or right) would be removed from their parent
         * @param newParent the new node parent
         * @param dir removal direction to follow: -1 is left, +1 is right
         */
        private void addNodeSiblingsToNewParent(Node node, Node newParent, int dir) {
            if (node != null) {
                if (dir == -1) {
                    addNodeSiblingsToNewParent(node.getPreviousSibling(), newParent, dir);
                } else if (dir == 1) {
                    addNodeSiblingsToNewParent(node.getNextSibling(), newParent, dir);
                }
            }
            if (node != null) {
                if (dir == -1) {
                    newParent.appendChild(node);
                } else if (dir == 1) {
                    newParent.insertFirst(node);
                }
            }
        }
    }

    class DomLinkExecutable extends DomStyleExecutable
    {
        protected static final String A = "a";

        DomLinkExecutable(Document document, Property propertyName, String propertyValue)
        {
            super(document, propertyName, propertyValue);
        }

        //handles the A tag
        @Override protected TextFragment handleTag(Text text, Element element)
        {
            if (A.equalsIgnoreCase(element.getNodeName())) {
                // todo: unlink ?
            } else {
                AnchorElement anchorElement = Document.get().createAnchorElement();
                anchorElement.setHref(propertyValue);

                element.replaceChild(anchorElement, text);
                anchorElement.appendChild(text);
            }
            return new TextFragment(text, 0, text.getLength());
        }

        @Override protected boolean matchesStyle(Element inputElement)
        {
            return A.equalsIgnoreCase(inputElement.getNodeName());
        }
    }
}
