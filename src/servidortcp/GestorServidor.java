package servidortcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import cifrado.GestorCifrado;


public class GestorServidor extends Thread{

    //Guardamos el socket de conexion con el cliente y todos los flujos de E/S
    private Socket socketComunicaciones;
    private BufferedReader lector;
    private PrintWriter escritor;

    //Constructor que recibe el socket del cliente para usarlo en el hilo
    public GestorServidor(Socket socket){
        this.socketComunicaciones = socket;
    }

    // sobreecribimos run que atiende a cada cliente. se ejecuta en Servidor (cliente.start()) al 
    // llamar a atenderCliente() si algo falla muestra mensaje y en el finally cierra el socket si o si
    @Override
    public void run() {

        try {

            atenderCliente();

        } catch (Exception e) {

            System.out.println("SERVIDOR >> Cliente desconectado inesperadamente.");

        }finally{
            try {
                socketComunicaciones.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // comunicacion con el jugador
    public void atenderCliente() throws Exception{

        System.out.println("SERVIDOR>> Sesión del cliente iniciada.");

        //Lector que recibira mensajes del cliente
        lector = new BufferedReader(new InputStreamReader(socketComunicaciones.getInputStream()));

        //escritor que enviara mensajes al cliente
        escritor = new PrintWriter(socketComunicaciones.getOutputStream(), true);
        
        // servidor envia la clave AES al cliente, esto permite que cliente decifre los mensajes
        escritor.println(Servidor.claveBase64);

        // mientras ningun jugador acierte, cada jugador sigue enviando numeros
        while (!Servidor.acertado){

            // lee el número cifrado del cliente 
            String numeroIngresado = lector.readLine();

            //si el cliente actual se desconecta lo quitamos de la listaJugadores y salimos
            if (numeroIngresado == null) {
                System.out.println("Cliente desconectado.");

                synchronized (Servidor.lock){
                    Servidor.listaJugadores.remove(this);
                }

                return;
            }

            // se decifra el numero recibido
            String numeroDescifrado = GestorCifrado.decifrar(numeroIngresado, Servidor.claveAES);

            try {
                int numero = Integer.parseInt(numeroDescifrado);

                //sincronizamos con el objeto lock de la clase Servidor
                synchronized(Servidor.lock){

                    String respuestaCifrada;

                    if (numero < Servidor.numSecreto) {
                        respuestaCifrada = GestorCifrado.cifrar("El número secreto es MAYOR", Servidor.claveAES);
                        escritor.println(respuestaCifrada);
                    }
                    else if (numero > Servidor.numSecreto) {
                        respuestaCifrada = GestorCifrado.cifrar("El número secreto es MENOR", Servidor.claveAES);
                        escritor.println(respuestaCifrada);
                    }
                    else {
                        
                        //cambiamos variable a true, el jugador acerto 
                        Servidor.acertado = true;

                        // aviso a todos los jugadores que se termina el juego
                        Servidor.broadcast("Fin del juego, ya hay un ganador!!");

                        //mnesaje individual al ganador
                        String mensajeGanador = "HAS GANADO!! El número secreto era " + Servidor.numSecreto;
                        respuestaCifrada = GestorCifrado.cifrar(mensajeGanador, Servidor.claveAES);
                        escritor.println(respuestaCifrada);
                     
                        System.out.println("______________________________");
                        System.out.println("SERVIDOR>> Juego terminado.");
                    }
            }

            // si el cliente no envia un numero valido lo atrapamos en esta exception
            } catch (NumberFormatException nfe) {
                escritor.println(GestorCifrado.cifrar("SERVIDOR>> ERROR, debes introducir un numero", Servidor.claveAES));
            }
        
        }
        
        // cierre de flujos E/S y socket finaliza la partida
        lector.close();
        escritor.close();
        socketComunicaciones.close();
        System.out.println("SERVIDOR>> Sesion del cliente finalizada.");
    }

    // enviar mensaje cifrado al cliente
    public void enviarMensaje(String mensaje){

        if (escritor != null) {
            escritor.println(mensaje);
        }else{
            System.out.println("SERVIDOR >> escritor no inicializado aún.");
        }
        
    }

}
