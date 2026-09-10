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

import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.ui.laf.RuneLiteTabbedPaneUI;

/**
 * Changes only tab geometry using protected Swing look-and-feel extension points.
 * Painting, hit testing, tooltips, and RuneLite's context menus all use those
 * rectangles. Logical indexes and the original navigation set never change.
 */
final class OrderedSidebarUI extends RuneLiteTabbedPaneUI
{
    private SidebarOrder order;
    private SidebarOrganizerConfig.NewTabPlacement placement;
    private int[] visualIndexes;

    OrderedSidebarUI(SidebarOrder order, SidebarOrganizerConfig.NewTabPlacement placement)
    {
        this.order = order;
        this.placement = placement;
    }

    void setOrder(SidebarOrder order, SidebarOrganizerConfig.NewTabPlacement placement)
    {
        this.order = order;
        this.placement = placement;
    }

    @Override
    protected LayoutManager createLayoutManager()
    {
        return new OrderedLayout();
    }

    @Override
    protected boolean shouldRotateTabRuns(int tabPlacement)
    {
        return false;
    }

    @Override
    protected int getRunForTab(int tabCount, int tabIndex)
    {
        int visual = visualIndexes != null && tabIndex >= 0 && tabIndex < visualIndexes.length
            ? visualIndexes[tabIndex] : tabIndex;
        return super.getRunForTab(tabCount, visual);
    }

    private final class OrderedLayout extends RuneLiteTabbedPaneLayout
    {
        @Override
        protected void calculateTabRects(int tabPlacement, int tabCount)
        {
            visualIndexes = null;
            super.calculateTabRects(tabPlacement, tabCount);
            if (tabCount < 2 || order == null)
            {
                return;
            }
            List<String> natural = new ArrayList<>();
            Map<String, Integer> logicalIndexes = new HashMap<>();
            Rectangle[] slots = new Rectangle[tabCount];
            for (int i = 0; i < tabCount; i++)
            {
                SidebarTab tab;
                try
                {
                    tab = new SidebarTab(tabPane, i);
                }
                catch (IllegalStateException ex)
                {
                    // A container change may trigger layout before the bridge's
                    // deferred validation. Keep the native geometry until then.
                    return;
                }
                natural.add(tab.id);
                if (logicalIndexes.put(tab.id, i) != null)
                {
                    // Ambiguous identities: leave the native layout untouched.
                    return;
                }
                slots[i] = new Rectangle(rects[i]);
            }
            List<String> visual = order.resolve(natural, placement);
            int[] mapping = new int[tabCount];
            for (int position = 0; position < tabCount; position++)
            {
                int logical = logicalIndexes.get(visual.get(position));
                rects[logical].setBounds(slots[position]);
                mapping[logical] = position;
            }
            visualIndexes = mapping;
        }
    }
}
