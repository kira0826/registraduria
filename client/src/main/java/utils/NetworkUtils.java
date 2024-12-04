package utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class NetworkUtils {

    // Definir el prefijo de la IP
    private static final String IP_PREFIX = "10.147.19";

    /**
     * Obtiene la primera dirección IPv4 no local que comienza con el prefijo definido.
     *
     * @return Dirección IP como String.
     * @throws SocketException Si ocurre un error al acceder a las interfaces de red.
     */
    public static String getLocalIPAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            // Ignorar interfaces de loopback, virtuales o que no estén activas
            if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                // Solo considerar direcciones IPv4 no loopback
                if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                    String ip = addr.getHostAddress();
                    if (ip.startsWith(IP_PREFIX)) {
                        return ip;
                    }
                }
            }
        }
        // Fallback a localhost si no se encuentra otra IP
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new SocketException("No se pudo determinar la dirección IP local.");
        }
    }

    /**
     * Obtiene el prefijo de la IP.
     *
     * @return Prefijo de la IP como String.
     */
    public static String getIPPrefix() {
        return IP_PREFIX;
    }
}