package com.willfp.eco.proxy.v1_16_R3;

import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.Validate;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class FastItemStackUtils {
    private static final Field FIELD;

    public static net.minecraft.server.v1_16_R3.ItemStack getNMSStack(@NotNull final ItemStack itemStack) {
        if (!(itemStack instanceof CraftItemStack)) {
            return CraftItemStack.asNMSCopy(itemStack);
        } else {
            try {
                net.minecraft.server.v1_16_R3.ItemStack nms = (net.minecraft.server.v1_16_R3.ItemStack) FIELD.get(itemStack);
                return nms == null ? CraftItemStack.asNMSCopy(itemStack) : nms;
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
                return net.minecraft.server.v1_16_R3.ItemStack.b;
            }
        }
    }

    static {
        Field temp = null;

        try {
            Field handleField = CraftItemStack.class.getDeclaredField("handle");
            handleField.setAccessible(true);
            temp = handleField;
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        assert temp != null;
        Validate.notNull(temp, "Error occurred in initialization!");

        FIELD = temp;
    }
}
