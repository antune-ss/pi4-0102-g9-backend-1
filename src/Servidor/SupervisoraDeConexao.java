package src.Servidor;

import java.io.*;
import java.net.*;
import java.util.*;

public class SupervisoraDeConexao extends Thread {
    private Socket conexao;
    private ArrayList<Parceiro> usuarios;
    private Parceiro usuario;

    public SupervisoraDeConexao(Socket conexao, ArrayList<Parceiro> usuarios) throws Exception {
        if (conexao == null)
            throw new Exception("Conexão ausente");
        if (usuarios == null)
            throw new Exception("Usuários ausentes");

        this.conexao = conexao;
        this.usuarios = usuarios;
    }

    @Override
    public void run() {
        ObjectOutputStream transmissor = null;
        try {
            transmissor = new ObjectOutputStream(this.conexao.getOutputStream());
        } catch (Exception erro) {
            return;
        }

        ObjectInputStream receptor = null;
        try {
            receptor = new ObjectInputStream(this.conexao.getInputStream());
        } catch (Exception erro) {
            try {
                transmissor.close();
            } catch (Exception ignored) {}
            return;
        }

        try {
            this.usuario = new Parceiro(this.conexao, receptor, transmissor);
        } catch (Exception erro) {
            return;
        }

        try {
            // Adiciona o parceiro à lista sincronizada
            synchronized (this.usuarios) {
                this.usuarios.add(this.usuario);
            }

            // Loop principal de comunicação com o cliente
            for (;;) {
                Comunicado comunicado = this.usuario.envie();
                if (comunicado == null)
                    return;

                // Processar comunicados / leituras recebidas do cliente aqui...
            }
        } catch (Exception erro) {
            // Cliente desconectou ou ocorreu erro de leitura
        } finally {
            // Remove o cliente da lista e encerra a conexão
            synchronized (this.usuarios) {
                this.usuarios.remove(this.usuario);
            }
            try {
                this.usuario.adeus();
            } catch (Exception ignored) {}
        }
    }
}