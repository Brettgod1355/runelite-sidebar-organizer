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
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Sidebar Organizer", description = "Arrange sidebar tabs in a saved order",
    tags = {"sidebar", "organizer", "reorder", "tabs"})
public class SidebarOrganizerPlugin extends Plugin
{
    private static final Logger LOG = LoggerFactory.getLogger(SidebarOrganizerPlugin.class);
    @Inject private ClientToolbar toolbar;
    @Inject private ConfigManager configManager;
    @Inject private SidebarOrganizerConfig config;
    @Inject private Gson gson;

    private SidebarPanel panel;
    private NavigationButton navigation;
    private SidebarBridge bridge;
    private SidebarDragHandler drag;
    private ContainerAdapter listener;
    private SidebarOrder order;
    private boolean refreshQueued;
    private boolean active;
    private int generation;

    @Provides
    SidebarOrganizerConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(SidebarOrganizerConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        onEdt(() ->
        {
            active = true;
            int session = ++generation;
            panel = new SidebarPanel(this::move, this::reset,
                locked -> configManager.setConfiguration(SidebarOrganizerConfig.GROUP, "locked", locked));
            navigation = NavigationButton.builder().tooltip("Sidebar Organizer").priority(10)
                .icon(icon()).panel(panel).build();
            toolbar.addNavigation(navigation);
            // addNavigation is asynchronous even on the EDT.
            SwingUtilities.invokeLater(() ->
            {
                if (active && generation == session)
                {
                    connect();
                }
            });
        });
    }

    private void connect()
    {
        try
        {
            order = SidebarOrder.decode(gson, config.savedOrder());
            bridge = new SidebarBridge(panel.getWrappedPanel(), order, config.newTabs());
            bridge.apply(order, config.newTabs());
            drag = new SidebarDragHandler(bridge.pane(), config::locked, this::moveComponents);
            drag.install();
            listener = new ContainerAdapter()
            {
                @Override
                public void componentAdded(ContainerEvent event)
                {
                    scheduleRefresh();
                }

                @Override
                public void componentRemoved(ContainerEvent event)
                {
                    scheduleRefresh();
                }
            };
            bridge.pane().addContainerListener(listener);
            refresh();
        }
        catch (RuntimeException ex)
        {
            fail(ex);
        }
    }

    private void scheduleRefresh()
    {
        if (!active || bridge == null || bridge.isChanging() || refreshQueued)
        {
            return;
        }
        refreshQueued = true;
        int session = generation;
        SwingUtilities.invokeLater(() ->
        {
            if (session != generation)
            {
                return;
            }
            refreshQueued = false;
            if (active && bridge != null)
            {
                refresh();
            }
        });
    }

    private void refresh()
    {
        try
        {
            bridge.apply(order, config.newTabs());
            panel.update(bridge.buttons(), config.locked());
        }
        catch (RuntimeException ex)
        {
            fail(ex);
        }
    }

    private void moveComponents(Component source, Component target)
    {
        if (bridge == null || !active || config.locked())
        {
            return;
        }
        try
        {
            String sourceId = null;
            String targetId = null;
            for (SidebarTab button : bridge.buttons())
            {
                if (button.component == source)
                {
                    sourceId = button.id;
                }
                if (button.component == target)
                {
                    targetId = button.id;
                }
            }
            if (sourceId != null && targetId != null)
            {
                move(sourceId, targetId);
            }
        }
        catch (RuntimeException ex)
        {
            fail(ex);
        }
    }

    private void move(String source, String target)
    {
        if (bridge == null || !active)
        {
            return;
        }
        try
        {
            List<String> visible = new ArrayList<>();
            bridge.buttons().forEach(button -> visible.add(button.id));
            SidebarOrder next = order.move(visible, source, target);
            if (next == order)
            {
                return;
            }
            bridge.apply(next, config.newTabs());
            order = next;
            configManager.setConfiguration(SidebarOrganizerConfig.GROUP, SidebarOrganizerConfig.ORDER, order.encode(gson));
            panel.update(bridge.buttons(), config.locked());
        }
        catch (RuntimeException ex)
        {
            fail(ex);
        }
    }

    private void reset()
    {
        configManager.unsetConfiguration(SidebarOrganizerConfig.GROUP, SidebarOrganizerConfig.ORDER);
        order = new SidebarOrder(Collections.emptyList());
        if (bridge == null && active)
        {
            connect();
        }
        else if (bridge != null)
        {
            refresh();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!SidebarOrganizerConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }
        SwingUtilities.invokeLater(() ->
        {
            if (!active || bridge == null)
            {
                return;
            }
            try
            {
                order = SidebarOrder.decode(gson, config.savedOrder());
                if (drag != null && config.locked())
                {
                    drag.cancel();
                }
                refresh();
            }
            catch (RuntimeException ex)
            {
                fail(ex);
            }
        });
    }

    private void fail(Exception ex)
    {
        LOG.warn("Sidebar Organizer stopped applying custom order", ex);
        disconnect();
        if (panel != null)
        {
            panel.failure("This sidebar layout could not be reordered. Reset order to retry; see the client log for details.");
        }
    }

    private void disconnect()
    {
        if (drag != null)
        {
            drag.uninstall();
            drag = null;
        }
        if (bridge != null)
        {
            if (listener != null)
            {
                bridge.pane().removeContainerListener(listener);
                listener = null;
            }
            try
            {
                bridge.restore();
            }
            catch (RuntimeException ex)
            {
                LOG.warn("Could not restore sidebar order; restart RuneLite to restore its default UI", ex);
            }
            bridge = null;
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        onEdt(() ->
        {
            active = false;
            generation++;
            refreshQueued = false;
            disconnect();
            if (navigation != null)
            {
                toolbar.removeNavigation(navigation);
                navigation = null;
            }
            panel = null;
            order = null;
        });
    }

    private static void onEdt(Runnable action) throws Exception
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            action.run();
        }
        else
        {
            SwingUtilities.invokeAndWait(action);
        }
    }

    private static BufferedImage icon()
    {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(245, 190, 72));
        for (int y = 4; y <= 18; y += 7)
        {
            g.fillRoundRect(3, y, 3, 3, 1, 1);
            g.fillRoundRect(9, y, 12, 3, 2, 2);
        }
        g.dispose();
        return image;
    }
}
