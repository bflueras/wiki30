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
package org.xwiki.gwt.wysiwyg.client.plugin.symbol;

import java.util.logging.Logger;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.Console;
import org.xwiki.gwt.user.client.ui.CompositeDialogBox;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.rt.BaseRealTimePlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.rt.EditorUtils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;

import fr.loria.score.jupiter.tree.operation.TreeOperation;

/**
 * Could not inherit the standard SymbolPlugin because of a circular dependence!
 * Allows the user to insert a special symbol chosen with a symbol picker in place of the current selection.
 *
 */
public class RealTimeSymbolPlugin extends BaseRealTimePlugin implements ClickHandler, CloseHandler<CompositeDialogBox>
{
    /**
     * The insert button to be placed on the tool bar.
     */
    private PushButton insert;

    /**
     * The symbol picker used for choosing the symbol to insert.
     */
    private RealTimeSymbolPicker picker;

    /**
     * Tool bar extension.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    private static Logger log = Logger.getLogger(RealTimeSymbolPlugin.class.getName());

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        Console.getInstance().log("Loading RT Symbol Plugin...");

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_HTML)) {
            insert = new PushButton(new Image(Images.INSTANCE.charmap()));
            saveRegistration(insert.addClickHandler(this));
            insert.setTitle(Strings.INSTANCE.charmap());

            toolBarExtension.addFeature("symbol", insert);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getUIExtensionList().add(toolBarExtension);
        }
        Console.getInstance().log("RT Symbol Plugin loaded ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (insert != null) {
            insert.removeFromParent();
            insert = null;

            if (picker != null) {
                picker.hide();
                picker.removeFromParent();
                picker = null;
            }
        }

        toolBarExtension.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == insert) {
            onSymbols(true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see CloseHandler#onClose(CloseEvent)
     */
    public void onClose(CloseEvent<CompositeDialogBox> event)
    {
        if (event.getSource() == getSymbolPicker() && !event.isAutoClosed()) {
            onSymbols(false);
        }
    }

    /**
     * Either shows the symbol picker dialog or inserts the chosen symbol depending in the given flag.
     * 
     * @param show whether to show the symbol picker or insert the chosen symbol.
     */
    public void onSymbols(boolean show)
    {
        if (show) {
            if (insert.isEnabled()) {
                getSymbolPicker().center();
            }
        } else { // insert it
            getTextArea().setFocus(true);
            String character = getSymbolPicker().getSymbol();

            if (character != null) {
                Range r = getTextArea().getDocument().getSelection().getRangeAt(0);
                if (r != null) {
                    r = EditorUtils.normalizeCaretPosition(r);
                    TreeOperation op = treeOperationFactory.createTreeInsertText(clientJupiter.getSiteId(), r,
                        character.charAt(0));
                    clientJupiter.generate(op);
                    log.severe("FOK");
                }
            }
        }
    }

    /**
     * We use lazy loading in case of the symbol picker to optimize editor loading time because the symbol palette is an
     * HTML table with many cells and takes a bit of time to be created. In the future we should consider using
     * innerHTML for creating the palette and widget binding.
     * 
     * @return the symbol picker to be used for selecting the symbol.
     */
    private RealTimeSymbolPicker getSymbolPicker()
    {
        if (picker == null) {
            picker = new RealTimeSymbolPicker();
            saveRegistration(picker.addCloseHandler(this));
        }
        return picker;
    }
}
