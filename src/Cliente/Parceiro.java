package src.Cliente;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Parceiro {
    // essenciais da conexão
    private Socket conexao;
    private ObjectInputStream receptor;
    private ObjectOutputStream transmissor;

    private Comunicado proximoComunicado = null;

    private Semaphore mutEx = new Semaphore(1, true);

    Parceiro(Socket conexao, ObjectInputStream rec, ObjectOutputStream trans) throws Exception {
        if (conexao == null)
            throw new Exception("Conexao ausente");

        if (rec == null)
            throw new Exception("Receptor ausente");

        if (trans == null)
            throw new Exception("Transmissor ausente");

        this.conexao = conexao;
        this.receptor = rec;
        this.transmissor = trans;
    }

    // a forma como vai operar pode ser mudada, portanto vou mexer depois nisso de
    // Comunicado ser usado
    public void receba(Comunicado x) {
        try {
            this.transmissor.writeObject(x);
            this.transmissor.flush();
        } catch (IOException erro) {
            throw new Exception("Erro de transmissao");
        }
    }

    public Comunicado envie() throws Exception {
        try {
            if (this.proximoComunicado == null)
                this.proximoComunicado = (Comunicado) this.receptor.readObject();
            Comunicado ret = this.proximoComunicado;
            this.proximoComunicado = null;
            return ret;
        } catch (Exception erro) {
            throw new Exception("Erro de recepcao");
        }
    }

    public Comunicado espie() throws Exception {
        try {
            // mutex é usado para que não se sobrescreva o que vai ser espiado
            this.mutEx.acquireUninterruptibly();
            if (this.proximoComunicado == null)
                this.proximoComunicado = (Comunicado) this.receptor.readObject();
            this.mutEx.release();
            return this.proximoComunicado;
        } catch (Exception erro) {
            throw new Exception("Erro de recepcao");
        }
    }

    public void adeus() throws Exception {
        try {
            this.transmissor.close();
            this.receptor.close();
            this.conexao.close();
        } catch (Exception erro) {
            throw new Exception("Erro de desconexao");
        }
    }
}
