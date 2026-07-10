package cifrado;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class GestorCifrado {
    
        //generar una clave AES de 128 bits
        public static SecretKey generarClave() throws Exception{

            KeyGenerator claveGenerada = KeyGenerator.getInstance("AES");
            claveGenerada.init(128);

            return claveGenerada.generateKey();
        }

        //convertir clave a texto Base64 para poder enviarla por el socket
        public static String claveBase64String(SecretKey clave){

            return Base64.getEncoder().encodeToString(clave.getEncoded());
        
        }

        //reconstruye la clave de texto apartir de la Base64 recibida
        public static SecretKey base64Texto(String base64){

            byte[] bytes = Base64.getDecoder().decode(base64);

            return new SecretKeySpec(bytes, "AES");
        }

        //cifra un texto con AES y lo devuelve en Base64
        public static String cifrar(String texto, SecretKey clave) throws Exception {
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // generar IV de 12 bytes rellenandolo con el el numero aleatio se hace de manera segura para que 
            // el cifrado no sea predecibl
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // configura AES-GCM con IV y tamaño del tag
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, clave, spec);

            // cifrado del texto recibido
            byte[] cifrado = cipher.doFinal(texto.getBytes());

            // creamos el paquete que guarda IV + el cifrado
            byte[] paquete = new byte[iv.length + cifrado.length];

            // copiamos el IV al paquete 
            System.arraycopy(iv, 0, paquete, 0, iv.length);

            // se copia luego ek cifrado
            System.arraycopy(cifrado, 0, paquete, iv.length, cifrado.length);

            // Devolver todo en Base64
            return Base64.getEncoder().encodeToString(paquete);
            
        }

        //hace lo contrario a cifrar, recibe Base64 decifra y devuelve el texto original
        public static String decifrar(String textoCifrado, SecretKey clave) throws Exception {

            // decodifico el texto y recupero los bytes originales
            byte[] paquete = Base64.getDecoder().decode(textoCifrado);

            // paquetes que conteine IV y el texto cifrado
            byte[] iv = new byte[12];
            byte[] cifrado = new byte[paquete.length - 12];

            // copio lo que queda del paquete desde la pos 0 hasta la final y hago mismo desde el 12 hasta fin
            System.arraycopy(paquete, 0, iv, 0, 12);
            System.arraycopy(paquete, 12, cifrado, 0, cifrado.length);

            // objeto Cipher que cifra usaando modo GCM
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");

            // aqui le paso al GCM los parametros con el IV y el tamaño del bloque 
            // inicializamos cipher y deciframos usando clave y IV
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cifrador.init(Cipher.DECRYPT_MODE, clave, spec);

            byte[] decifrado = cifrador.doFinal(cifrado);

            return new String(decifrado);

        }


}
