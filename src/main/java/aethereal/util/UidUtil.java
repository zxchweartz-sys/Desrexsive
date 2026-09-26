package aethereal.util;

import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Локальная система UID + HWID клиента Desrexsive.
 *
 * UID — порядковый номер пользователя (1, 2, 3, ...). При первом запуске
 * присваивается 1 и сохраняется в конфиг; при необходимости номер можно
 * выдать следующим пользователям, изменив значение в конфиге вручную.
 * HWID — контрольная сумма (SHA-256) стабильных характеристик машины
 * (MAC-адреса сетевых интерфейсов + ОС + архитектура + имя пользователя + CPU),
 * используется для привязки лицензии к конкретному ПК.
 *
 * Конфиг хранится в: <runDirectory>/configs/general/uid.json
 */
public final class UidUtil {

    private static final Gson GSON = new Gson();

    private UidUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Порядковый номер пользователя (устойчив между запусками). */
    public static String getUid() {
        return load().uid;
    }

    /** Аппаратный идентификатор машины (не меняется между запусками). */
    public static String getHwid() {
        return load().hwid;
    }

    private static File file() {
        return new File(new File(new File(MinecraftClient.getInstance().runDirectory, "configs"), "general"), "uid.json");
    }

    private static Data load() {
        try {
            File f = file();
            if (f.exists()) {
                byte[] bytes = Files.readAllBytes(f.toPath());
                if (bytes.length > 0) {
                    Data data = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), Data.class);
                    if (data != null && data.isValid()) {
                        return data;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Data fresh = Data.create();
        save(fresh);
        return fresh;
    }

    private static void save(Data data) {
        try {
            File f = file();
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            Files.write(f.toPath(), GSON.toJson(data).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static final class Data {
        String uid;
        String hwid;

        boolean isValid() {
            return uid != null && !uid.isBlank() && hwid != null && !hwid.isBlank();
        }

        static Data create() {
            Data data = new Data();
            data.uid = "1";
            data.hwid = computeHwid();
            return data;
        }
    }

    private static String computeHwid() {
        StringBuilder sb = new StringBuilder();
        try {
            List<String> macs = new ArrayList<>();
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback() || iface.isVirtual() || !iface.isUp()) {
                    continue;
                }
                byte[] hardware = iface.getHardwareAddress();
                if (hardware != null && hardware.length > 0) {
                    StringBuilder mac = new StringBuilder();
                    for (byte b : hardware) {
                        mac.append(String.format("%02X", b));
                    }
                    macs.add(mac.toString());
                }
            }
            Collections.sort(macs);
            sb.append(String.join("|", macs));
        } catch (Exception ignored) {
        }
        sb.append('|').append(System.getProperty("os.name", ""));
        sb.append('|').append(System.getProperty("os.arch", ""));
        sb.append('|').append(System.getProperty("user.name", ""));
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpu != null && !cpu.isBlank()) {
            sb.append('|').append(cpu.trim());
        }
        return sha256(sb.toString());
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

}