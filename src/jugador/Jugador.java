package jugador;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

import javax.crypto.SecretKey;

import cifrado.GestorCifrado;
import utilidades.MiScanner;

public class Jugador {
    
    //clave AES que recibo del servidor
    static SecretKey claveAES;

    private static BufferedReader lector;
    private static PrintWriter escritor;

    public static void main(String[] args) {

        try {
            //conexión con el servidor
            Socket socketConexionS = new Socket("localhost", 51000);

            // flujos de inicializacion que reciben mensajes cifrados del servidor
            lector = new BufferedReader(new InputStreamReader(socketConexionS.getInputStream()));
            escritor = new PrintWriter(socketConexionS.getOutputStream(), true);

            // variable que recibe la clave AES enviada por el servidor. (servidor la envia en Base64)
            String mensajeIncial = lector.readLine();

            // si mensaje enviado por servidor es que esta lleno, muestra el mensaje y cierra eocket y termina el programa para ese cliente
            if (mensajeIncial.startsWith("Servidor Lleno")) {
                System.out.println(mensajeIncial);
                socketConexionS.close();
                return;
            }

            // convierto la claveAES enviada en Base64 a SecretKey
            claveAES = GestorCifrado.base64Texto(mensajeIncial);

            System.out.println("JUEGO ADIVINA NÚMERO SECRETO");

            // jugador puede intentar adivinar el numero las veces que quiera
            while (true) {
                
                String numero;

                // leo el numero del usuario y Si preciona Ctrl+C lanzaria esta excepción
                try {

                    numero = MiScanner.miString("Ingresa un número del 1 al 100: ");

                } catch (Exception e) {
                    System.out.println("Saliendo del juego...");
                    break;
                }

                // cifro el numero antes de enviarlo
                String numeroCifrado = GestorCifrado.cifrar(numero, claveAES);
                escritor.println(numeroCifrado);


                //recibimos respuesta cifrada del servidor
                String respuestaCifrada = lector.readLine();

                // al dar ctrl+c se vuelve null el readLine y entonces atrapamos ese error aqui
                if (respuestaCifrada == null) {
                    System.out.println("Conexión cerrada.");
                    break;
                }

                // deciframos la respuesta
                String respuesta = GestorCifrado.decifrar(respuestaCifrada, claveAES);
            
                System.out.println("SERVIDOR>> " + respuesta);

                //si se gana se termina el juego, se sale del bucle
                if (respuesta.startsWith("GANASTE") || respuesta.toLowerCase().startsWith("fin")) {
                    System.out.println("Juego teminado.");
                    break;
                }
            }

            // cierre de flujos y socket
            lector.close();
            escritor.close();
            socketConexionS.close();
 
        }catch (ConnectException ce){
            System.out.println("Servidor no disponible.");
        }catch(SocketException sk){
            System.out.println("No se pudo conectar al servidor");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}
