package servidortcp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import cifrado.GestorCifrado;

public class Servidor {

    // maximos jugadores a participar
    private static final int MAX_JUGADORES = 12;
    
    // numero a adivinar 
    protected static int numSecreto;

    // atributo que cambia si un jugador acerta el numero secreto
    protected static boolean acertado = false;

    //socket que acepta conexiones de jugadores y se comunica con ellos
    private ServerSocket socketServidor;

    // lista con los jugadores conectados
    public static List<GestorServidor> listaJugadores = new ArrayList<>();

    //objeto de bloqueo para sincronizar acceso concurrente a listaJugadores
    public static Object lock = new Object();

    // clave AES para cifrar comunicacion
    public static SecretKey claveAES;
    
    // Clave AES codificada para enviar a lo clientes
    public static String claveBase64;


    public Servidor(int puerto) throws Exception{

        // generar clave AES que compartiran todos los clientes
        claveAES = GestorCifrado.generarClave();
        claveBase64 = GestorCifrado.claveBase64String(claveAES);

        // creacion se socket servidor que escucha en el puerto que se le pasa por parametros
        socketServidor = new ServerSocket(puerto);

        // numero generado del 1 al 100 y muestro el numero en el servidor para pruebas
        numSecreto  = (int)(Math.random()*100)+1 ;
        System.out.println("numero Secreto: " + numSecreto); 
        
        // bucle infinito que espera conexion de jugadores 
        while (true){

            // aceptamos a jugadores si los hay y se establece la conexion
            Socket socketComunicacion = socketServidor.accept();

            // bloque sincronizado 
            synchronized(lock){

                // si no hay 12 jugadores
                if (listaJugadores.size() < MAX_JUGADORES) {

                    // creamos hilo que gestione a cada nuevo jugador
                    GestorServidor jugador = new GestorServidor(socketComunicacion);

                    System.out.println("SERVIDOR>> Cliente conectado desde: " + socketComunicacion.getInetAddress().getHostName());

                    // añado jugador a la lista
                    listaJugadores.add(jugador);
                    
                    // lanzamos el hilo que atendera al jugador
                    jugador.start();

                // si el servidor esta lleno, avisa y ademas cierra conexion
                } else{
                    PrintWriter escritor = new PrintWriter(socketComunicacion.getOutputStream(), true);
                    escritor.println("Servidor Lleno. Máximo 12 jugadores.");
                    socketComunicacion.close();
                }
            }

        }
    }

    // buenas practicas crear funcion que cierre el socker del servidor
    public void terminar() throws IOException{
        socketServidor.close();
        System.out.println("SERVIDOR >>> Conexiones cerradas");
    }


    // Envia un mensaje cifrado a todos los jugadores conectados.
    // synchronized para evitar que varios hilos envien mensajes simultaneamente y mezclen datos
    // 
    public static synchronized void broadcast(String mensaje) throws Exception{

        // ciframos el mensaje con la clave AES compartida
        String mensajeCifrado = GestorCifrado.cifrar(mensaje, claveAES);

        // para cada jugador de la lista enviamos mensaje
        for (GestorServidor jugador : listaJugadores) {
            try {
                jugador.enviarMensaje(mensajeCifrado);
 
            }catch (Exception e) {
                System.out.println("SERVIDOR >> Error jugador desconectado.");
            }
        }
    }

    // clase principal 
    public static void main(String[] args) {

        try {
            
            System.out.println("SERVIDOR >>> Iniciando servidor en el puerto 51000");
            System.out.println("SERVIDOR>> Esperando jugadores...");

            new Servidor(51000);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
