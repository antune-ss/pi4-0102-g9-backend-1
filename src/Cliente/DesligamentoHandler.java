package src.Cliente;

public class DesligamentoHandler {
    private Parceiro servidor;

    public DesligamentoHandler(Parceiro servidor) throws Exception {
        if (servidor == null)
            throw new Exception("Porta invalida");

        this.servidor = servidor;
    }

    public void run() {
        for (;;) {
            try {
                if (this.servidor.espie() instanceof ComunicadoDesligamento) {
                    System.out.println("\nO servidor vai ser desligado agora;");
                    System.err.println("volte mais tarde!\n");
                    System.exit(0);
                }
            } catch (Exception erro) {
            }
        }
    }
}