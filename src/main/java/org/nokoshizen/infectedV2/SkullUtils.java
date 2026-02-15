package org.nokoshizen.infectedV2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.UUID;

public class SkullUtils {

    public static ItemStack getSkull(String uuid) {
        String json = getContent("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        if (json == null) return null;

        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String value = o.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        try {
            // Créer un GameProfile avec un nom non-null (obligatoire)
            GameProfile profile = new GameProfile(UUID.randomUUID(), "CustomHead");
            profile.getProperties().put("textures", new Property("textures", value));

            // Reflection pour créer un ResolvableProfile à partir du GameProfile
            Class<?> resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
            Constructor<?> constructor = resolvableProfileClass.getDeclaredConstructor(GameProfile.class);
            constructor.setAccessible(true);
            Object resolvableProfile = constructor.newInstance(profile);

            // Injecter dans le meta la ResolvableProfile
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, resolvableProfile);

            skull.setItemMeta(meta);
            return skull;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getContent(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}