package src.Servidor;

import java.net.*;
import java.util.*;

public class AceitadorDeConexao {
    private ServerSocket pedido;
    private ArrayList<Parceiro> usuarios;

    AceitadorDeConexao(String porta, ArrayList<Parceiro> users) throws Exception {
        if (porta == null)
            throw new Exception("Porta ausente");

        try {
            this.pedido = new ServerSocket(Integer.parseInt(porta));
        } catch (Exception e) {
            throw new Exception("Porta invalida");
        }

        if (users == null)
            throw new Exception("Usuarios ausentes");

        this.usuarios = users;
    }

    public void run() {
        for (;;) {
            Socket conexao = null;

            try {
                conexao = this.pedido.accept();
            } catch (Exception e) {
                continue;
            }

            SupervisoraDeConexao supervisoraDeConexao = null;

            try {
                supervisoraDeConexao = new SupervisoraDeConexao(conexao, users);
            } catch (Exception e) {
            }

            supervisoraDeConexao.start();
        }
    }
}
