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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.TabbedPaneUI;
import net.runelite.client.ui.laf.RuneLiteTabbedPaneUI;

/** Owns a reversible, public Swing UI-delegate installation. No private field access. */
final class SidebarBridge
{
    private final JTabbedPane pane;
    private final TabbedPaneUI originalUI;
    private final OrderedSidebarUI orderedUI;
    private SidebarOrder order;
    private SidebarOrganizerConfig.NewTabPlacement placement;
    private boolean changing;

    SidebarBridge(Component ownPanel, SidebarOrder order, SidebarOrganizerConfig.NewTabPlacement placement)
    {
        requireEdt();
        pane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, ownPanel);
        if (pane == null || pane.getUI().getClass() != RuneLiteTabbedPaneUI.class
            || pane.getTabPlacement() != JTabbedPane.RIGHT
            || pane.getTabLayoutPolicy() != JTabbedPane.WRAP_TAB_LAYOUT)
        {
            throw new IllegalStateException("Unsupported sidebar layout or another sidebar UI is installed");
        }
        originalUI = pane.getUI();
        this.order = order;
        this.placement = placement;
        naturalTabs();
        orderedUI = new OrderedSidebarUI(order, placement);
        changing = true;
        try
        {
            pane.setUI(orderedUI);
        }
        catch (RuntimeException ex)
        {
            pane.setUI(originalUI);
            throw ex;
        }
        finally
        {
            changing = false;
        }
    }

    JTabbedPane pane()
    {
        return pane;
    }

    boolean isChanging()
    {
        return changing;
    }

    List<SidebarTab> buttons()
    {
        requireEdt();
        validateOwnership();
        Map<String, SidebarTab> byId = new HashMap<>();
        List<String> naturalIds = new ArrayList<>();
        for (SidebarTab tab : naturalTabs())
        {
            byId.put(tab.id, tab);
            naturalIds.add(tab.id);
        }
        List<SidebarTab> result = new ArrayList<>();
        order.resolve(naturalIds, placement).forEach(id -> result.add(byId.get(id)));
        return result;
    }

    void apply(SidebarOrder order, SidebarOrganizerConfig.NewTabPlacement placement)
    {
        requireEdt();
        validateOwnership();
        naturalTabs();
        this.order = order;
        this.placement = placement;
        orderedUI.setOrder(order, placement);
        pane.revalidate();
        pane.doLayout();
        pane.repaint();
    }

    void restore()
    {
        requireEdt();
        validateOwnership();
        changing = true;
        try
        {
            pane.setUI(originalUI);
            pane.revalidate();
            pane.doLayout();
            pane.repaint();
        }
        finally
        {
            changing = false;
        }
    }

    private void validateOwnership()
    {
        if (pane.getUI() != orderedUI)
        {
            throw new IllegalStateException("Another component changed the sidebar UI");
        }
    }

    private List<SidebarTab> naturalTabs()
    {
        List<SidebarTab> tabs = new ArrayList<>();
        Map<String, SidebarTab> ids = new HashMap<>();
        for (int i = 0; i < pane.getTabCount(); i++)
        {
            SidebarTab tab = new SidebarTab(pane, i);
            if (ids.put(tab.id, tab) != null)
            {
                throw new IllegalStateException("Two sidebar tabs have the same persistent identity");
            }
            tabs.add(tab);
        }
        return tabs;
    }

    static void requireEdt()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            throw new IllegalStateException("Sidebar access must be on the Swing EDT");
        }
    }
}
