package aethereal.discord;

import aethereal.config.BaseProcessor;
import aethereal.util.UidUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/**
 * Discord Rich Presence клиента Desrexsive.
 *
 * Статус в Discord:
 *  - верхняя строка: название приложения из панели разработчика
 *    (там укажите «Desrexsive | Rework 1.21.4»)
 *  - state: «Uid - &lt;номер&gt;»
 *  - на сервере: details = айпи сервера
 *  - в главном меню: details = «В главном меню»
 *  - 2 кнопки всегда (и в меню, и на сервере): Desrexsive DLC (telegram) и Discord
 *
 * ⚠️ Замечание: самую верхнюю строку («Playing <название>») Discord
 * берёт из названия приложения в панели разработчика, клиент её не задаёт.
 *
 * НАСТРОЙКА:
 *  1. Откройте https://discord.com/developers/applications → New Application
 *  2. Скопируйте 19-значный Application ID и вставьте его в APPLICATION_ID ниже
 *  3. Большая картинка активности — это внешний URL (LARGE_IMAGE_KEY).
 *     Сейчас там прямой линк на анимированный GIF лого из GitHub-репозитория:
 *     https://raw.githubusercontent.com/zxchweartz-sys/Desrexsive/main/pictures/Desrexsive_Logo_3D_Glow.gif
 *     (Discord принимает http(s)-URL в поле assets.large_image;
 *      также можно вернуть ключ ассета из разделав Rich Presence → Art Assets)
 *  Кнопки видны только в личном статусе (профиль по клику на ник/аватарку),
 *  в списке участников сервера их нет.
 */
public class DiscordProcessor extends BaseProcessor {

    // ======================================================================
    // Discord Application ID (19 цифр)
    // ======================================================================
    private static final long APPLICATION_ID = 1552687471597199510L;

    private static final String LARGE_IMAGE_KEY = "https://raw.githubusercontent.com/zxchweartz-sys/Desrexsive/main/pictures/Desrexsive_Logo_3D_Glow.gif";
    private static final String LARGE_IMAGE_TEXT = "Desrexsive";

    private static final String BTN_A_LABEL = "Desrexsive DLC";
    private static final String BTN_A_URL = "https://t.me/DesrexsiveDlc";
    private static final String BTN_B_LABEL = "Discord";
    private static final String BTN_B_URL = "https://discord.gg/8Qnac7YqBc";

    private static final long CONNECT_RETRY_MS = 5000L;
    private static final long ACTIVITY_UPDATE_MS = 5000L;

    private DiscordIPC ipc;
    private long lastConnectAttempt;
    private long lastActivitySend;
    private long activityStart = System.currentTimeMillis() / 1000L;
    private String cachedUid;
    private boolean configuredWarned;
    private volatile boolean activityLogged;

    @Override
    public void setup() {
    }

    @Override
    public void unSetup() {
        shutdown();
    }

    /** Вызывается каждый игровой тик с рендер-потока (см. MinecraftClientMixin.onGlobalTick). */
    public void onTick() {
        if (APPLICATION_ID <= 0L) {
            if (!this.configuredWarned) {
                this.configuredWarned = true;
                System.out.println("[Desrexsive] Discord RPC неактивен: укажите APPLICATION_ID в aethereal/discord/DiscordProcessor.java");
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (this.ipc == null || !this.ipc.d()) {
            if (now - this.lastConnectAttempt < CONNECT_RETRY_MS) {
                return;
            }
            this.lastConnectAttempt = now;
            connectAsync();
            return;
        }
        if (now - this.lastActivitySend < ACTIVITY_UPDATE_MS) {
            return;
        }
        this.lastActivitySend = now;
        updateActivity();
    }

    private void connectAsync() {
        try {
            if (this.ipc == null) {
                this.ipc = DiscordIPC.a(APPLICATION_ID);
            }
            this.ipc.b().whenComplete((result, ex) -> {
                if (ex == null) {
                    updateActivity();
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private void updateActivity() {
        DiscordIPC currentIpc = this.ipc;
        if (currentIpc == null || !currentIpc.d()) {
            return;
        }
        try {
            if (this.cachedUid == null) {
                this.cachedUid = UidUtil.getUid();
            }
            Activity.a builder = new Activity.a()
                    .type(ActivityType.PLAYING)
                    .state("Uid - " + this.cachedUid)
                    .startAt(this.activityStart)
                    .largeImage(LARGE_IMAGE_KEY, LARGE_IMAGE_TEXT)
                    .c(BTN_A_LABEL, BTN_A_URL)
                    .c(BTN_B_LABEL, BTN_B_URL);
            String server = currentServer();
            if (server != null) {
                builder.b(server);
            } else {
                builder.b("В главном меню");
            }
            currentIpc.b(builder.build()).whenComplete((json, ex) -> {
                if (ex != null) {
                    System.out.println("[Desrexsive] Discord RPC: SET_ACTIVITY failed: " + ex);
                } else if (!this.activityLogged) {
                    this.activityLogged = true;
                    System.out.println("[Desrexsive] Discord RPC: статус принят Discord: " + json);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private String currentServer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return null;
        }
        try {
            if (mc.getNetworkHandler() != null) {
                ServerInfo info = mc.getNetworkHandler().getServerInfo();
                if (info != null && info.address != null && !info.address.isBlank()) {
                    return info.address;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public void shutdown() {
        if (this.ipc != null) {
            try {
                this.ipc.close();
            } catch (Throwable ignored) {
            }
            this.ipc = null;
        }
    }

    /** Доступ к текущему IPC-клиенту (совместимость со старым API). */
    public DiscordIPC a() {
        return this.ipc;
    }
}