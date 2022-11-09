import config.clientimpl.ClientConfigManager;
import config.serverimpl.ServerConfigManager;
import proxy.clientimpl.ClientEeProxy;
import proxy.serverimpl.ServerEeProxy;
import utils.Log;

import java.io.IOException;

public class EeProxyApplication {
    public static void main(String[] args) {
        if (args.length != 1||(!args[0].equals("CLIENT")&&!args[0].equals("SERVER"))) {
            Log.error("Usage: <CLIENT|SERVER>");
            return;
        }

        try {
            switch (args[0]) {
                case "CLIENT" -> {
                    ClientConfigManager.load();
                    new ClientEeProxy().start(ClientConfigManager.getPort());
                }
                case "SERVER" -> {
                    ServerConfigManager.load();
                    new ServerEeProxy().start(ServerConfigManager.getPort());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
