package src.Servidor;

public class Servidor {
    public static String PORTA_PADRAO = "3000";

    public static void main(String[] agrs) {
        if (args.length > 1) {
            System.err.println("Erro na passagem de valores. Uso esperado é java Servidor [PORTA]");
            return;
        }

        String porta = Servidor.PORTA_PADRAO;

        if (args.length == 1)
            porta = args[0];

        ArrayList<Parceiro> usuarios = new ArrayList<Parceiro>();

        AceitadorDeConexao aceitador = null;

    }
}
