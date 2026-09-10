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
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarDragHandlerTest
{
    @Test
    public void dragMovesOriginalComponentsAndUninstallRemovesListeners() throws Exception
    {
        List<Component> moved = new ArrayList<>();
        List<Component> expected = new ArrayList<>();
        SwingUtilities.invokeAndWait(() ->
        {
            JTabbedPane pane = pane();
            int listeners = pane.getMouseListeners().length;
            Component source = pane.getComponentAt(0);
            Component target = pane.getComponentAt(2);
            SidebarDragHandler handler = new SidebarDragHandler(pane, () -> false, (a, b) ->
            {
                moved.add(a);
                moved.add(b);
            });
            handler.install();
            handler.mousePressed(event(pane, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1));
            handler.mouseDragged(event(pane, MouseEvent.MOUSE_DRAGGED, 2, MouseEvent.NOBUTTON));
            assertEquals(Cursor.MOVE_CURSOR, pane.getCursor().getType());
            handler.mouseReleased(event(pane, MouseEvent.MOUSE_RELEASED, 2, MouseEvent.BUTTON1));
            assertNotEquals(Cursor.MOVE_CURSOR, pane.getCursor().getType());
            handler.uninstall();
            assertEquals(listeners, pane.getMouseListeners().length);
            // Result is deferred until the original mouse event has finished.
            assertTrue(moved.isEmpty());
            expected.add(source);
            expected.add(target);
        });
        SwingUtilities.invokeAndWait(() -> assertEquals(expected, moved));
    }

    @Test
    public void clickLockRightReleaseAndDisappearingSourceDoNotReorder() throws Exception
    {
        List<Component> moved = new ArrayList<>();
        SwingUtilities.invokeAndWait(() ->
        {
            JTabbedPane pane = pane();
            AtomicBoolean locked = new AtomicBoolean(false);
            SidebarDragHandler handler = new SidebarDragHandler(pane, locked::get, (a, b) -> moved.add(a));
            handler.mousePressed(event(pane, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1));
            handler.mouseReleased(event(pane, MouseEvent.MOUSE_RELEASED, 0, MouseEvent.BUTTON1));
            locked.set(true);
            drag(handler, pane, MouseEvent.BUTTON1);
            locked.set(false);
            drag(handler, pane, MouseEvent.BUTTON3);
            handler.mousePressed(event(pane, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1));
            handler.mouseDragged(event(pane, MouseEvent.MOUSE_DRAGGED, 2, MouseEvent.NOBUTTON));
            pane.removeTabAt(0);
            handler.mouseReleased(event(pane, MouseEvent.MOUSE_RELEASED, 1, MouseEvent.BUTTON1));
        });
        SwingUtilities.invokeAndWait(() -> assertTrue(moved.isEmpty()));
    }

    private static void drag(SidebarDragHandler handler, JTabbedPane pane, int releaseButton)
    {
        handler.mousePressed(event(pane, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1));
        handler.mouseDragged(event(pane, MouseEvent.MOUSE_DRAGGED, 2, MouseEvent.NOBUTTON));
        handler.mouseReleased(event(pane, MouseEvent.MOUSE_RELEASED, 2, releaseButton));
    }

    private static JTabbedPane pane()
    {
        JTabbedPane pane = new JTabbedPane(JTabbedPane.RIGHT);
        pane.addTab("A", new JLabel("A"));
        pane.addTab("B", new JLabel("B"));
        pane.addTab("C", new JLabel("C"));
        pane.setSize(250, 400);
        pane.doLayout();
        return pane;
    }

    private static MouseEvent event(JTabbedPane pane, int type, int tab, int button)
    {
        Rectangle bounds = pane.getBoundsAt(tab);
        return new MouseEvent(pane, type, 1, 0, bounds.x + bounds.width / 2,
            bounds.y + bounds.height / 2, 1, false, button);
    }
}
