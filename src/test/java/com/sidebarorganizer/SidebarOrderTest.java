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

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import org.junit.Test;
import static com.sidebarorganizer.SidebarOrganizerConfig.NewTabPlacement.END;
import static com.sidebarorganizer.SidebarOrganizerConfig.NewTabPlacement.NEAR_DEFAULT;
import static org.junit.Assert.*;

public class SidebarOrderTest
{
    @Test
    public void defaultOrderAndBothInsertionPolicies()
    {
        List<String> natural = Arrays.asList("A", "B", "C", "D", "E");
        assertEquals(natural, new SidebarOrder(Collections.emptyList()).resolve(natural, END));
        SidebarOrder reverse = new SidebarOrder(Arrays.asList("D", "C", "B", "A"));
        assertEquals(Arrays.asList("D", "C", "B", "A", "E"), reverse.resolve(natural, END));
        assertEquals(Arrays.asList("D", "E", "C", "B", "A"), reverse.resolve(natural, NEAR_DEFAULT));
    }

    @Test
    public void disabledPluginsKeepTheirSavedSlotsAndNewTabsDoNotScrambleKnownOnes()
    {
        SidebarOrder order = new SidebarOrder(Arrays.asList("A", "disabled", "B", "C"));
        order = order.move(Arrays.asList("A", "B", "C", "new"), "C", "A");
        assertEquals(Arrays.asList("C", "disabled", "A", "B", "new"),
            order.resolve(Arrays.asList("A", "B", "C", "disabled", "new"), END));
        assertEquals(Arrays.asList("C", "A", "B", "new"),
            order.resolve(Arrays.asList("A", "B", "C", "new"), END));
    }

    @Test
    public void arbitraryRepeatedMovesRoundTripThroughJson()
    {
        Gson gson = new Gson();
        List<String> natural = Arrays.asList("A", "B", "C", "D");
        SidebarOrder order = new SidebarOrder(Collections.emptyList());
        order = order.move(natural, "D", "A");
        order = order.move(order.resolve(natural, END), "B", "D");
        order = SidebarOrder.decode(gson, order.encode(gson));
        assertEquals(Arrays.asList("B", "D", "A", "C"), order.resolve(natural, END));
        assertSame(order, order.move(natural, "missing", "A"));
        assertSame(order, order.move(natural, "A", "A"));
    }

    @Test
    public void savedIdsHandleQuotesNewlinesAndDuplicates()
    {
        Gson gson = new Gson();
        List<String> ids = Arrays.asList("Panel\nA,B", "Panel\nA\"B", "Panel\nA,B");
        SidebarOrder order = SidebarOrder.decode(gson, new SidebarOrder(ids).encode(gson));
        assertEquals(Arrays.asList("Panel\nA,B", "Panel\nA\"B"), order.resolve(ids.subList(0, 2), END));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedConfig()
    {
        SidebarOrder.decode(new Gson(), "{broken");
    }

    @Test
    public void samePanelClassWithDifferentTooltipsHasDistinctIds()
    {
        assertNotEquals(SidebarOrder.id(button("A", 5, new TestPanel())),
            SidebarOrder.id(button("B", 5, new TestPanel())));
        assertNotEquals(SidebarOrder.id(button("A", 5, new TestPanel())),
            SidebarOrder.id(button("A", 5, new OtherPanel())));
    }

    private static NavigationButton button(String tooltip, int priority, PluginPanel panel)
    {
        return NavigationButton.builder().tooltip(tooltip).priority(priority).panel(panel).build();
    }

    private static class OtherPanel extends PluginPanel
    {
    }
    private static class TestPanel extends PluginPanel
    {
    }

}
