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
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteLAF;
import net.runelite.client.ui.laf.RuneLiteTabbedPaneUI;
import org.junit.Test;
import static com.sidebarorganizer.SidebarOrganizerConfig.NewTabPlacement.END;
import static org.junit.Assert.*;

/** Reflection is used ONLY in this test fixture to initialize the real ClientUI headlessly. */
public class SidebarBridgeTest
{
    @Test
    public void visualOrderChangesButNativeNavigationAndSelectionNeverMove() throws Exception
    {
        edt(() ->
        {
            Fixture f = new Fixture();
            NavigationButton a = f.add("A", 5);
            NavigationButton b = f.add("B", 5);
            NavigationButton c = f.add("C", 5);
            f.pane.setSelectedIndex(1);
            f.pane.doLayout();
            Rectangle firstSlot = f.pane.getBoundsAt(0);
            List<Integer> changes = new ArrayList<>();
            f.pane.addChangeListener(e -> changes.add(f.pane.getSelectedIndex()));
            SidebarBridge bridge = new SidebarBridge(a.getPanel().getWrappedPanel(), f.order(c, a, b), END);
            bridge.apply(f.order(c, a, b), END);
            assertSame(f.original, f.entries());
            f.assertAligned();
            assertSame(b.getPanel().getWrappedPanel(), f.pane.getSelectedComponent());
            assertEquals(firstSlot, f.pane.getBoundsAt(2));
            assertTrue(changes.isEmpty());
            Rectangle target = f.pane.getBoundsAt(2);
            int hit = f.pane.indexAtLocation(target.x + target.width / 2, target.y + target.height / 2);
            assertEquals(2, hit);
            f.pane.dispatchEvent(new MouseEvent(f.pane, MouseEvent.MOUSE_PRESSED, 1, 0,
                target.x + target.width / 2, target.y + target.height / 2, 1, false, MouseEvent.BUTTON1));
            assertSame(c.getPanel().getWrappedPanel(), f.pane.getSelectedComponent());
            assertEquals(Collections.singletonList(2), changes);
            bridge.restore();
            assertEquals(RuneLiteTabbedPaneUI.class, f.pane.getUI().getClass());
            assertSame(c.getPanel().getWrappedPanel(), f.pane.getSelectedComponent());
            assertSame(f.original, f.entries());
            f.assertAligned();
        });
    }

    @Test
    public void realClientAddRemoveAndReenableKeepNativeAndVisualOrdersCorrect() throws Exception
    {
        edt(() ->
        {
            Fixture f = new Fixture();
            NavigationButton a = f.add("A", 5);
            NavigationButton b = f.add("B", 5);
            SidebarOrder order = f.order(b, a);
            SidebarBridge bridge = new SidebarBridge(a.getPanel().getWrappedPanel(), order, END);
            bridge.apply(order, END);
            NavigationButton c = f.add("C", -100);
            bridge.apply(order, END);
            assertEquals(Arrays.asList("B", "A", "C"), titles(bridge));
            assertArrayEquals(new Object[]{c, a, b}, f.entries().toArray());
            f.assertAligned();
            f.remove(b);
            bridge.apply(order, END);
            assertEquals(Arrays.asList("A", "C"), titles(bridge));
            NavigationButton newB = f.add("B", 5);
            bridge.apply(order, END);
            assertEquals(Arrays.asList("B", "A", "C"), titles(bridge));
            assertArrayEquals(new Object[]{c, a, newB}, f.entries().toArray());
            f.assertAligned();
            bridge.restore();
            assertSame(f.original, f.entries());
            f.assertAligned();
        });
    }

    @Test
    public void multipleColumnsKeepEveryIconHitTestMappedToItsOriginalPanel() throws Exception
    {
        edt(() ->
        {
            Fixture f = new Fixture();
            List<NavigationButton> buttons = new ArrayList<>();
            for (int i = 0; i < 15; i++)
            {
                buttons.add(f.add(String.format("Tab %02d", i), 5));
            }
            f.pane.setSize(300, 140);
            f.pane.doLayout();
            List<Rectangle> nativeSlots = new ArrayList<>();
            for (int i = 0; i < buttons.size(); i++)
            {
                nativeSlots.add(f.pane.getBoundsAt(i));
            }
            Collections.reverse(buttons);
            SidebarOrder order = f.order(buttons.toArray(new NavigationButton[0]));
            SidebarBridge bridge = new SidebarBridge(buttons.get(0).getPanel().getWrappedPanel(), order, END);
            bridge.apply(order, END);
            for (int visual = 0; visual < buttons.size(); visual++)
            {
                Component panel = buttons.get(visual).getPanel().getWrappedPanel();
                int logical = f.pane.indexOfComponent(panel);
                Rectangle bounds = f.pane.getBoundsAt(logical);
                assertEquals(nativeSlots.get(visual), bounds);
                assertEquals(logical, f.pane.indexAtLocation(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
            }
            // Painting exercises the actual RuneLite/FlatLaf delegate for wrapped runs.
            BufferedImage image = new BufferedImage(300, 140, BufferedImage.TYPE_INT_ARGB);
            f.pane.paint(image.createGraphics());
            f.assertAligned();
            bridge.restore();
        });
    }

    @Test
    public void resetPreservesClosedPanelAndRestoreDoesNotResurrectRemovedTabs() throws Exception
    {
        edt(() ->
        {
            Fixture f = new Fixture();
            NavigationButton a = f.add("A", 1);
            NavigationButton b = f.add("B", 1);
            SidebarBridge bridge = new SidebarBridge(a.getPanel().getWrappedPanel(), f.order(b, a), END);
            bridge.apply(f.order(b, a), END);
            assertEquals(-1, f.pane.getSelectedIndex());
            bridge.apply(new SidebarOrder(Collections.emptyList()), END);
            assertEquals(Arrays.asList("A", "B"), titles(bridge));
            f.remove(b);
            bridge.restore();
            assertEquals(1, f.pane.getTabCount());
            assertEquals(-1, f.pane.getSelectedIndex());
            f.assertAligned();
        });
    }

    @Test
    public void refusesToReplaceAnotherPluginsUiDelegate() throws Exception
    {
        edt(() ->
        {
            Fixture f = new Fixture();
            NavigationButton a = f.add("A", 1);
            javax.swing.plaf.basic.BasicTabbedPaneUI foreign = new javax.swing.plaf.basic.BasicTabbedPaneUI();
            f.pane.setUI(foreign);
            try
            {
                new SidebarBridge(a.getPanel().getWrappedPanel(), f.order(a), END);
                fail("Expected unsupported UI guard");
            }
            catch (IllegalStateException expected)
            {
                assertSame(foreign, f.pane.getUI());
                assertSame(f.original, f.entries());
            }
        });
    }

    @Test
    public void unfamiliarTabFallsBackToNativeGeometryBeforeDeferredValidation() throws Exception
    {
        edt(() ->
        {
            Fixture f = new Fixture();
            NavigationButton a = f.add("A", 1);
            NavigationButton b = f.add("B", 1);
            SidebarOrder order = f.order(b, a);
            SidebarBridge bridge = new SidebarBridge(a.getPanel().getWrappedPanel(), order, END);
            bridge.apply(order, END);
            f.pane.addTab("Unknown", new javax.swing.JPanel());
            // Swing can lay out immediately, before the container listener runs.
            f.pane.doLayout();
            Rectangle originalFirst = f.pane.getBoundsAt(0);
            try
            {
                bridge.apply(order, END);
                fail("Expected unfamiliar tab guard");
            }
            catch (IllegalStateException expected)
            {
                bridge.restore();
                assertEquals(originalFirst, f.pane.getBoundsAt(0));
                assertEquals(3, f.pane.getTabCount());
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsAccessOutsideEdt()
    {
        new SidebarBridge(null, new SidebarOrder(Collections.emptyList()), END);
    }

    private static List<String> titles(SidebarBridge bridge)
    {
        List<String> titles = new ArrayList<>();
        bridge.buttons().forEach(button -> titles.add(button.title));
        return titles;
    }

    private interface CheckedRunnable
    {
        void run() throws Exception;
    }

    private static void edt(CheckedRunnable task) throws Exception
    {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() ->
        {
            try
            {
                task.run();
            }
            catch (Throwable ex)
            {
                failure[0] = ex;
            }
        });
        if (failure[0] != null)
        {
            throw new AssertionError(failure[0]);
        }
    }

    private static class Fixture
    {
        final ClientUI ui;
        final JTabbedPane pane;
        final TreeSet<NavigationButton> original;

        Fixture() throws Exception
        {
            RuneLiteLAF.setup();
            pane = new JTabbedPane(JTabbedPane.RIGHT);
            pane.putClientProperty("FlatLaf.style", "tabInsets: 2,5,2,5; variableSize: true; deselectable: true; tabHeight: 26");
            pane.setSize(300, 600);
            Constructor<?> constructor = ClientUI.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object[] args = new Object[constructor.getParameterCount()];
            Class<?>[] types = constructor.getParameterTypes();
            for (int i = 0; i < types.length; i++)
            {
                if (types[i] == boolean.class)
                {
                    args[i] = false;
                }
                if (types[i] == String.class)
                {
                    args[i] = "Sidebar Organizer test";
                }
            }
            ui = (ClientUI) constructor.newInstance(args);
            Field field = ClientUI.class.getDeclaredField("sidebar");
            field.setAccessible(true);
            field.set(ui, pane);
            original = entries();
        }

        NavigationButton add(String name, int priority) throws Exception
        {
            NavigationButton button = NavigationButton.builder().tooltip(name).priority(priority)
                .panel(new TestPanel()).icon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)).build();
            call("addNavigation", button);
            return button;
        }

        void remove(NavigationButton button) throws Exception
        {
            call("removeNavigation", button);
        }

        private void call(String name, NavigationButton button) throws Exception
        {
            Method method = ClientUI.class.getDeclaredMethod(name, NavigationButton.class);
            method.setAccessible(true);
            method.invoke(ui, button);
        }

        @SuppressWarnings("unchecked")
        TreeSet<NavigationButton> entries() throws Exception
        {
            Field field = ClientUI.class.getDeclaredField("sidebarEntries");
            field.setAccessible(true);
            return (TreeSet<NavigationButton>) field.get(ui);
        }

        void assertAligned() throws Exception
        {
            assertEquals(entries().size(), pane.getTabCount());
            int i = 0;
            for (NavigationButton button : entries())
            {
                assertSame(button.getPanel().getWrappedPanel(), pane.getComponentAt(i++));
            }
        }

        SidebarOrder order(NavigationButton... buttons)
        {
            List<String> ids = new ArrayList<>();
            for (NavigationButton button : buttons)
            {
                ids.add(SidebarOrder.id(button));
            }
            return new SidebarOrder(ids);
        }
    }
    private static class TestPanel extends PluginPanel
    {
    }

}
