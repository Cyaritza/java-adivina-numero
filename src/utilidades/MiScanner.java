package utilidades;

import java.util.Scanner;

public class MiScanner {
    
    public static String miString(String mensaje){

        Scanner entrada = new Scanner(System.in);
        System.out.println(mensaje);
        return entrada.nextLine();
    }
    
}
