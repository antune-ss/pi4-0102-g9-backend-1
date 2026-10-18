package src.Cliente;

import java.net.*;
import java.io.*;

public class Cliente {
    public static final String HOST_PADRAO = "localhost";
    public static final int PORTA_PADRAO = 3000;

    public static void main(String[] args) {
        if (args.length > 2) {
            System.err.println("Erro: muitas informações passadas para [HOST[PORTA]]");
            return;
        }

        Socket conexao = null;

        try {
            String host = Cliente.HOST_PADRAO;
            int porta = Cliente.PORTA_PADRAO;

            // foram passadas informações sobre host
            if (args.length > 0)
                host = args[0];

            if (args.length == 2)
                porta = Integer.parseInt(args[1]);

            conexao = new Socket(host, porta);

        } catch (Exception e) {
            System.err.println("Erro ao conectar com o servidor, verifique o host e porta passados");
            return;
        }

        ObjectOutputStream output = null;

        try {
            output = new ObjectOutputStream(conexao.getOutputStream());
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o servidor, verifique o host e porta passados");
            return;
        }

        ObjectInputStream input = null;

        try {
            input = new ObjectInputStream(conexao.getInputStream());
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o servidor, verifique o host e porta passados");
            return;
        }

        Parceiro servidor = null;

        try {
            servidor = new Parceiro(conexao, input, output);
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o servidor, verifique o host e porta passados");
            return;
        }

        DesligamentoHandler desligamento = null;

        try {
            desligamento = new DesligamentoHandler(servidor);
        } catch (Exception e) {
        }

        // esta parte ainda será construida de acordo com o que será necessário
        do {
            /*
             * Existem duas opções:
             * 
             * Opção A — Recomendada: navegador → Servidor.java
             * O JavaScript abre um WebSocket diretamente com o servidor. O Cliente.java
             * continua existindo como cliente Java para a comunicação exigida pelo
             * professor, mas não precisa intermediar as mensagens do navegador.
             * Mais simples para um dashboard web e atualizações em tempo real.
             * 
             * 
             * Opção B — Navegador → Cliente.java → Servidor.java
             * O Cliente.java precisaria aceitar conexões WebSocket do navegador e,
             * separadamente, encaminhar as mensagens ao Servidor.java pelo socket Java. Ele
             * funcionaria como uma ponte entre os dois protocolos.
             * Possível, mas adiciona complexidade e exige que o cliente Java esteja
             * executando e acessível pelo navegador.
             * 
             * 
             * Recomendação do chat: é a Opção A, a menos que o professor tenha exigido
             * explicitamente que toda comunicação do frontend passe pelo Cliente.java.
             */
        } while (true);
    }
}
