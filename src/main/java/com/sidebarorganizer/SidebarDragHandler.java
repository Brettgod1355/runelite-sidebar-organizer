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
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/** Listens alongside RuneLite's normal click handlers; never replaces them. */
final class SidebarDragHandler extends MouseAdapter
{
    private final JTabbedPane pane;
    private final BooleanSupplier locked;
    private final BiConsumer<Component, Component> move;
    private Component source;
    private Point start;
    private Cursor cursor;
    private boolean dragging;

    SidebarDragHandler(JTabbedPane pane, BooleanSupplier locked, BiConsumer<Component, Component> move)
    {
        this.pane = pane;
        this.locked = locked;
        this.move = move;
    }

    void install()
    {
        pane.addMouseListener(this);
        pane.addMouseMotionListener(this);
    }

    void uninstall()
    {
        cancel();
        pane.removeMouseListener(this);
        pane.removeMouseMotionListener(this);
    }

    void cancel()
    {
        if (dragging)
        {
            pane.setCursor(cursor);
        }
        source = null;
        dragging = false;
    }

    @Override
    public void mousePressed(MouseEvent event)
    {
        cancel();
        int index = pane.indexAtLocation(event.getX(), event.getY());
        if (!locked.getAsBoolean() && SwingUtilities.isLeftMouseButton(event) && index >= 0)
        {
            source = pane.getComponentAt(index);
            start = event.getPoint();
            cursor = pane.getCursor();
        }
    }

    @Override
    public void mouseDragged(MouseEvent event)
    {
        if (source != null && !locked.getAsBoolean() && start.distance(event.getPoint()) >= 5)
        {
            dragging = true;
            pane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent event)
    {
        Component moving = source;
        int target = pane.indexAtLocation(event.getX(), event.getY());
        boolean shouldMove = dragging && SwingUtilities.isLeftMouseButton(event)
            && !locked.getAsBoolean() && target >= 0
            && pane.indexOfComponent(moving) >= 0;
        Component destination = shouldMove ? pane.getComponentAt(target) : null;
        cancel();
        if (shouldMove && moving != destination)
        {
            // Let RuneLite finish processing this mouse event before changing tabs.
            SwingUtilities.invokeLater(() -> move.accept(moving, destination));
        }
    }
}
