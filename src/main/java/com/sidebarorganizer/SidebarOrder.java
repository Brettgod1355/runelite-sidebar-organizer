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
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.ui.NavigationButton;

/** Immutable saved order. Missing plugins keep their slots until reset. */
final class SidebarOrder
{
    private static final int MAX_ENTRIES = 4096;
    private final List<String> ids;
    private final Map<String, Integer> ranks = new HashMap<>();

    SidebarOrder(Collection<String> values)
    {
        List<String> clean = new ArrayList<>();
        for (String id : values)
        {
            if (id != null && !id.isEmpty() && id.length() <= 4096 && !ranks.containsKey(id))
            {
                ranks.put(id, clean.size());
                clean.add(id);
                if (clean.size() == MAX_ENTRIES)
                {
                    break;
                }
            }
        }
        ids = Collections.unmodifiableList(clean);
    }

    static String id(NavigationButton button)
    {
        // RuneLite has no public stable navigation ID. Class + tooltip distinguishes
        // separate panels with the same display name and survives client restarts.
        return button.getPanel().getClass().getName() + "\n" + button.getTooltip();
    }

    static SidebarOrder decode(Gson gson, String json)
    {
        if (json == null || json.isEmpty())
        {
            return new SidebarOrder(Collections.emptyList());
        }
        if (json.length() > 1_000_000)
        {
            throw new IllegalArgumentException("Saved sidebar order is too large");
        }
        try
        {
            String[] values = gson.fromJson(json, String[].class);
            if (values == null)
            {
                throw new IllegalArgumentException("Saved sidebar order is null");
            }
            return new SidebarOrder(java.util.Arrays.asList(values));
        }
        catch (JsonParseException ex)
        {
            throw new IllegalArgumentException("Saved sidebar order is invalid", ex);
        }
    }

    String encode(Gson gson)
    {
        return gson.toJson(ids);
    }

    List<String> resolve(List<String> natural, SidebarOrganizerConfig.NewTabPlacement placement)
    {
        if (ids.isEmpty())
        {
            return new ArrayList<>(natural);
        }
        Set<String> active = new HashSet<>(natural);
        List<String> result = new ArrayList<>();
        ids.stream().filter(active::contains).forEach(result::add);
        for (int i = 0; i < natural.size(); i++)
        {
            String unknown = natural.get(i);
            if (result.contains(unknown))
            {
                continue;
            }
            int insertion = result.size();
            if (placement == SidebarOrganizerConfig.NewTabPlacement.NEAR_DEFAULT)
            {
                // Deterministic neighbor rule: nearest preceding natural tab,
                // otherwise nearest following tab. Never reorder saved tabs.
                boolean found = false;
                for (int j = i - 1; j >= 0; j--)
                {
                    int neighbor = result.indexOf(natural.get(j));
                    if (neighbor >= 0)
                    {
                        insertion = neighbor + 1;
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    for (int j = i + 1; j < natural.size(); j++)
                    {
                        int neighbor = result.indexOf(natural.get(j));
                        if (neighbor >= 0)
                        {
                            insertion = neighbor;
                            break;
                        }
                    }
                }
            }
            result.add(insertion, unknown);
        }
        return result;
    }

    SidebarOrder move(List<String> visible, String source, String target)
    {
        int from = visible.indexOf(source);
        int to = visible.indexOf(target);
        if (from < 0 || to < 0 || from == to)
        {
            return this;
        }
        List<String> desired = new ArrayList<>(visible);
        desired.remove(from);
        desired.add(to, source);
        // Substitute visible slots, leaving disabled plugins in their saved slots.
        Set<String> active = new HashSet<>(visible);
        List<String> result = new ArrayList<>();
        int cursor = 0;
        for (String id : ids)
        {
            result.add(active.contains(id) ? desired.get(cursor++) : id);
        }
        while (cursor < desired.size())
        {
            result.add(desired.get(cursor++));
        }
        return new SidebarOrder(result);
    }
}
