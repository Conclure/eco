package com.willfp.eco.proxy.v1_16_R3;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.willfp.eco.core.display.Display;
import com.willfp.eco.proxy.ChatComponentProxy;
import net.minecraft.server.v1_16_R3.ChatBaseComponent;
import net.minecraft.server.v1_16_R3.ChatHoverable;
import net.minecraft.server.v1_16_R3.ChatMessage;
import net.minecraft.server.v1_16_R3.ChatModifier;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.MojangsonParser;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class ChatComponent implements ChatComponentProxy {
    @Override
    public Object modifyComponent(@NotNull final Object obj,
                                  @NotNull final Player player) {
        if (!(obj instanceof IChatBaseComponent chatComponent)) {
            return obj;
        }

        for (IChatBaseComponent iChatBaseComponent : chatComponent) {
            if (iChatBaseComponent == null) {
                continue;
            }

            modifyBaseComponent(iChatBaseComponent, player);
        }

        return chatComponent;
    }

    private void modifyBaseComponent(@NotNull final IChatBaseComponent component,
                                     @NotNull final Player player) {
        for (IChatBaseComponent sibling : component.getSiblings()) {
            if (sibling == null) {
                continue;
            }

            modifyBaseComponent(sibling, player);
        }
        if (component instanceof ChatMessage) {
            Arrays.stream(((ChatMessage) component).getArgs())
                    .filter(o -> o instanceof IChatBaseComponent)
                    .map(o -> (IChatBaseComponent) o)
                    .forEach(o -> this.modifyBaseComponent(o, player));
        }

        ChatHoverable hoverable = component.getChatModifier().getHoverEvent();

        if (hoverable == null) {
            return;
        }

        JsonObject jsonObject = hoverable.b();
        JsonElement json = hoverable.b().get("contents");
        if (json.getAsJsonObject().get("id") == null) {
            return;
        }
        if (json.getAsJsonObject().get("tag") == null) {
            return;
        }
        String id = json.getAsJsonObject().get("id").toString();
        String tag = json.getAsJsonObject().get("tag").toString();
        ItemStack itemStack = getFromTag(tag, id);

        Display.displayAndFinalize(itemStack, player);

        json.getAsJsonObject().remove("tag");
        String newTag = toJson(itemStack);
        json.getAsJsonObject().add("tag", new JsonPrimitive(newTag));

        jsonObject.remove("contents");
        jsonObject.add("contents", json);
        ChatHoverable newHoverable = ChatHoverable.a(jsonObject);
        ChatModifier modifier = component.getChatModifier();
        modifier = modifier.setChatHoverable(newHoverable);

        ((ChatBaseComponent) component).setChatModifier(modifier);
    }

    private static ItemStack getFromTag(@NotNull final String jsonTag,
                                        @NotNull final String id) {
        String processedId = id;
        String processedJsonTag = jsonTag;
        processedId = processedId.replace("minecraft:", "");
        processedId = processedId.toUpperCase();
        processedId = processedId.replace("\"", "");
        processedJsonTag = processedJsonTag.substring(1, processedJsonTag.length() - 1);
        processedJsonTag = processedJsonTag.replace("id:", "\"id\":");
        processedJsonTag = processedJsonTag.replace("\\", "");
        Material material = Material.getMaterial(processedId);

        assert material != null;
        ItemStack itemStack = new ItemStack(material);
        net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

        try {
            nmsStack.setTag(MojangsonParser.parse(processedJsonTag));
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    private static String toJson(@NotNull final ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack).getOrCreateTag().toString();
    }
}
