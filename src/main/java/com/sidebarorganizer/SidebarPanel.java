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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

final class SidebarPanel extends PluginPanel
{
    private final DefaultListModel<SidebarTab> model = new DefaultListModel<>();
    private final JList<SidebarTab> list = new JList<>(model);
    private final JLabel status = new JLabel("Connecting to sidebar...");
    private final JButton up = new JButton("Up");
    private final JButton down = new JButton("Down");
    private final JButton reset = new JButton("Reset order");
    private final JCheckBox lock = new JCheckBox("Lock dragging");

    SidebarPanel(BiConsumer<String, String> move, Runnable onReset, java.util.function.Consumer<Boolean> onLock)
    {
        super(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.setOpaque(false);
        JLabel title = new JLabel("Sidebar Organizer");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 17f));
        heading.add(title, BorderLayout.NORTH);
        heading.add(new JLabel("<html>Drag icons on the sidebar or reorder<br>the list below. Changes save automatically.</html>"), BorderLayout.CENTER);
        status.setFont(status.getFont().deriveFont(11f));
        heading.add(status, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(32);
        list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        list.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> owner, Object value, int index, boolean selected, boolean focus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(owner, value, index, selected, focus);
                SidebarTab button = (SidebarTab) value;
                label.putClientProperty("html.disable", true);
                label.setText(button.title);
                label.setIcon(button.icon);
                label.setIconTextGap(10);
                label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return label;
            }
        });
        list.addListSelectionListener(event -> updateButtons());
        MouseAdapter drag = new MouseAdapter()
        {
            private String source;
            private java.awt.Point start;
            private boolean moved;

            @Override
            public void mousePressed(MouseEvent event)
            {
                source = null;
                moved = false;
                start = event.getPoint();
                int index = list.locationToIndex(start);
                if (javax.swing.SwingUtilities.isLeftMouseButton(event) && !lock.isSelected()
                    && index >= 0 && list.getCellBounds(index, index).contains(start))
                {
                    source = model.get(index).id;
                }
            }

            @Override
            public void mouseDragged(MouseEvent event)
            {
                moved = source != null && start.distance(event.getPoint()) >= 5;
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                String moving = source;
                source = null;
                int index = list.locationToIndex(event.getPoint());
                if (moving != null && moved && javax.swing.SwingUtilities.isLeftMouseButton(event)
                    && list.isEnabled() && !lock.isSelected() && index >= 0
                    && list.getCellBounds(index, index).contains(event.getPoint()))
                {
                    move.accept(moving, model.get(index).id);
                }
                moved = false;
            }
        };
        list.addMouseListener(drag);
        list.addMouseMotionListener(drag);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(205, 350));
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridLayout(0, 1, 0, 6));
        footer.setOpaque(false);
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        actions.setOpaque(false);
        actions.add(up);
        actions.add(down);
        up.addActionListener(event -> moveSelected(-1, move));
        down.addActionListener(event -> moveSelected(1, move));
        reset.addActionListener(event -> onReset.run());
        lock.setOpaque(false);
        lock.addActionListener(event -> onLock.accept(lock.isSelected()));
        footer.add(actions);
        footer.add(reset);
        footer.add(lock);
        add(footer, BorderLayout.SOUTH);
        updateButtons();
    }

    void update(List<SidebarTab> buttons, boolean locked)
    {
        SidebarTab selected = list.getSelectedValue();
        model.clear();
        buttons.forEach(model::addElement);
        if (selected != null)
        {
            for (int i = 0; i < model.size(); i++)
            {
                if (model.get(i).id.equals(selected.id))
                {
                    list.setSelectedIndex(i);
                    break;
                }
            }
        }
        lock.setSelected(locked);
        list.setEnabled(true);
        reset.setEnabled(true);
        status.setText(buttons.size() + " tabs · " + (locked ? "Dragging locked" : "Ready to arrange"));
        status.setToolTipText(null);
        updateButtons();
    }

    void failure(String message)
    {
        status.setText("Sidebar ordering unavailable");
        status.setToolTipText(message);
        list.setEnabled(false);
        up.setEnabled(false);
        down.setEnabled(false);
        // Reset remains available to recover from malformed saved configuration.
        reset.setEnabled(true);
    }

    private void moveSelected(int delta, BiConsumer<String, String> move)
    {
        int index = list.getSelectedIndex();
        int target = index + delta;
        if (index >= 0 && target >= 0 && target < model.size())
        {
            move.accept(model.get(index).id, model.get(target).id);
        }
    }

    private void updateButtons()
    {
        int index = list.getSelectedIndex();
        up.setEnabled(list.isEnabled() && index > 0);
        down.setEnabled(list.isEnabled() && index >= 0 && index < model.size() - 1);
    }
}
