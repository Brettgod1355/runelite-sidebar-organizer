/*
 * Copyright (c) 2026, Brettgod1355 (https://github.com/Brettgod1355)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sidebarorganizer;

import java.awt.Component;
import java.awt.Container;
import javax.swing.Icon;
import javax.swing.JTabbedPane;
import net.runelite.client.ui.PluginPanel;

/** A view of an existing Swing tab, read entirely through public APIs. */
final class SidebarTab
{
    final String id;
    final String title;
    final Icon icon;
    final Component component;

    SidebarTab(JTabbedPane pane, int index)
    {
        component = pane.getComponentAt(index);
        title = pane.getToolTipTextAt(index);
        icon = pane.getIconAt(index);
        PluginPanel panel = findPanel(component);
        if (panel == null || title == null)
        {
            throw new IllegalStateException("Unrecognized sidebar tab");
        }
        id = panel.getClass().getName() + "\n" + title;
    }

    private static PluginPanel findPanel(Component component)
    {
        if (component instanceof PluginPanel)
        {
            return (PluginPanel) component;
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                PluginPanel found = findPanel(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }
}
