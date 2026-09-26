package platform;


import aethereal.core.Desrexsive;
import aethereal.render.LogoGif;
import net.fabricmc.api.ClientModInitializer;

public class Initializer implements ClientModInitializer {


    public void onInitializeClient() {
        LogoGif.request();
        new Desrexsive();
    }
}
