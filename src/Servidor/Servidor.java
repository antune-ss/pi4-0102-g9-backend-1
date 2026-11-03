package src.Servidor;

public class Servidor {
    public static String PORTA_PADRAO = "3000";

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Erro na passagem de valores. Uso esperado é java Servidor [PORTA] - Servidor.java:8");
            return;
        }

        String porta = Servidor.PORTA_PADRAO;

        if (args.length == 1)
            porta = args[0];

        ArrayList<Parceiro> usuarios = new ArrayList<Parceiro>();

        AceitadorDeConexao aceitador = null;
        try {
            aceitador = new AceitadorDeConexao(porta, usuarios);
        aceitador.start();
            System.out.println("Servidor executado na porta - Servidor.java:23" + porta + "...");
        } catch (Exception e) {
            System.err.println("Erro ao iniciar o aceitador: - Servidor.java:25" + e.getMessage());
            return;
        }

    }
}
