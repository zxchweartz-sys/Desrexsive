package platform;


import aethereal.core.Desrexsive;
import net.fabricmc.api.ClientModInitializer;

public class Initializer implements ClientModInitializer {


    public void onInitializeClient() {
        new Desrexsive();
    }
}
