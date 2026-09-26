package aethereal.module.player;

import aethereal.core.Category;
import aethereal.core.Desrexsive;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.setting.BindSetting;
import aethereal.util.ChatUtil;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

@ModuleRegister(name = "Click Action", description = "Выполняет действие, привязанное к выбранной клавише", category = Category.Player)
public class ClickAction extends Module {
    private final BindSetting b = new BindSetting("Эндер-жемчуг", -1).a(() -> {
        Desrexsive.getInstance().getModuleProcessor().v().getUseableHandler().a(Items.ENDER_PEARL.getDefaultStack());
    });
    private final BindSetting c = new BindSetting("Добавление друга", -1).a(() -> {
        EntityHitResult hit = mc.crosshairTarget instanceof EntityHitResult ehr ? ehr : null;
        if (hit != null) {
            if (hit.getEntity() instanceof AbstractClientPlayerEntity targetPlayer) {
                if (targetPlayer != mc.player) {
                    String name = targetPlayer.getName().getString();
                    if (Desrexsive.getInstance().getModuleProcessor().e().d(name)) {
                        Desrexsive.getInstance().getModuleProcessor().e().c(name);
                        Desrexsive.getInstance().getModuleProcessor().e().unSetup();
                        ChatUtil.sendMessage("Товарищ " + name + " был успешно удален из списка друзей.");
                    } else {
                        Desrexsive.getInstance().getModuleProcessor().e().b(name);
                        Desrexsive.getInstance().getModuleProcessor().e().unSetup();
                        ChatUtil.sendMessage("Товарищ " + name + " был успешно добавлен в список друзей.");
                    }
                }
            }
        }
    });

    public ClickAction() {
        a(this.b, this.c);
    }

    public BindSetting q() {
        return this.b;
    }

    public BindSetting r() {
        return this.c;
    }
}
