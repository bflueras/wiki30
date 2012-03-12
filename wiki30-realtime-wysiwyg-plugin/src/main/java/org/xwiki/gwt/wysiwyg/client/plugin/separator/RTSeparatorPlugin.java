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
package org.xwiki.gwt.wysiwyg.client.plugin.separator;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.CompositeUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import fr.loria.score.jupiter.tree.operation.TreeOperation;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.wysiwyg.client.plugin.rt.BaseRealTimePlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.rt.EditorUtils;

/**
 * Does not inherit the standard SeparatorPlugin because it is so simple code.
 * Allows the user to insert an horizontal line in place of the current selection.
 *
 */
public class RTSeparatorPlugin extends BaseRealTimePlugin implements ClickHandler
{
    /**
     * The tool bar button used for inserting a new horizontal rule.
     */
    private PushButton hr;

    /**
     * Tool bar extension that includes the horizontal rule button.
     */
    private final FocusWidgetUIExtension toolBarFocusWidgets = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        //getTextArea().getCommandManager().registerCommand(Command.INSERT_HORIZONTAL_RULE,
        //    new InsertHRExecutable(textArea));

        //if (getTextArea().getCommandManager().isSupported(Command.INSERT_HORIZONTAL_RULE)) {
        hr = new PushButton(new Image(Images.INSTANCE.hr()));
        saveRegistration(hr.addClickHandler(this));
        hr.setTitle(Strings.INSTANCE.hr());
        toolBarFocusWidgets.addFeature("hr", hr);
        //}
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (hr != null) {
            hr.removeFromParent();
            hr = null;
        }

        toolBarFocusWidgets.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == hr && hr.isEnabled()) {
            getTextArea().setFocus(true);
 
            Range r = getTextArea().getDocument().getSelection().getRangeAt(0);
            if (r != null) {
               r = EditorUtils.normalizeCaretPosition(r);               
               TreeOperation op = treeOperationFactory.createHeadingOrParagraphOperation(clientJupiter.getSiteId(), r, "hr");
               clientJupiter.generate(op);
            }

        }
    }
}
