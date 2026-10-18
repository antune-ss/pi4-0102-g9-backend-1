package br.puc.campinas.co2nect.mqtt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Utilitário responsável por configurar o aperto de mão seguro (SSL/TLS)
 * com o Broker MQTT utilizando o certificado CA raiz da Autoridade Certificadora.
 */
public class MqttSslUtils {
    
    public static SSLSocketFactory getSocketFactory(String caCertPath) throws Exception {
        /**
         * A classe java.io.File é a ferramenta básica do Java para gerenciar o sistema de arquivos do computador.
         * A classe java.io.File representa apenas o caminho (o endereço) até o arquivo ou diretório, e não os dados binários ou o conteúdo dele.
         * 
         * 1. O que ela pode fazer (Gerenciamento):
         *      - Criar diretórios (mkdir()) ou listar os arquivos que estão dentro de uma pasta (list()).
         *      - Verificar se o arquivo realmente existe no computador (exists()), se é uma pasta (isDirectory()) ou um arquivo comum (isFile()).
         *      - Descobrir o tamanho do arquivo em bytes (length()), o caminho completo dele (getAbsolutePath()) ou quando ele foi modificado.
         *      - Criar um arquivo em branco (createNewFile()), deletar (delete()) ou renomear (renameTo()).
         * 
         * 2. O que ela não pode fazer (Conteúdo):
         *      - Ela não lê o que está dentro do arquivo.
         *      - Ela não escreve texto ou dados.
         *      - Para ler ou escrever o conteúdo, é necessário usar outras classes, como FileWriter, FileReader, Scanner ou BufferedReader.
         */
        File certFile = new File(caCertPath);
        if (!certFile.exists()) {
            certFile = new File("co2nect-server/" + caCertPath);
        }
        if (!certFile.exists()) {
            throw new IllegalArgumentException("CA Certificate was not found in: " + caCertPath);
        }

        /**
         * O CertificateFactory inicializa uma "fábrica" especializada para ler e criar certificados do padrão X.509.
         * 
         * O arquivo .crt é um texto em Base64 seguindo o padrão internacional X.509 (o formato padrão de certificados digitais da internet). 
         * O CertificateFactory faz o parse desse texto e extrai a chave pública da Autoridade Certificadora (CA), a data de validade e o nome do emissor.
         * 
         * O FileInputStream é oq realmente começa a ler os bytes do arquivo. 
         * Ele está dentro de um Try-with-resources para, assim q o Java terminar de ler o arquivo, fechar o FileInputStream automaticamente
         * 
         * O generateCertificate valida se esses bytes realmente formam um certificado digital legítimo.
         * Se estiver tudo certo, ele reconstrói e entrega um objeto do tipo X509Certificate.
         */
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (InputStream fis = new FileInputStream(certFile)) {
            caCert = (X509Certificate) certFactory.generateCertificate(fis);
        }

        /**
         * A classe KeyStore serve para armazenar chaves criptográficas e certificados.
         * O KeyStore.getDefaultType() pergunta para o Java qual é o formato de chaveiro padrão do sistema (geralmente é o formato chamado PKCS12 - Sla q bomba é essa).
         * 
         * O método load serve para carregar um chaveiro que já existe no seu computador (como um arquivo .jks ou .p12).
         * Ao passar null no arquivo e null na senha, isso cria um chaveiro totalmente novo e em branco
         * 
         * O setCertificateEntry guarda o certificado com o apelido "emqx-ca"
         */
        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        caKeyStore.setCertificateEntry("emqx-ca", caCert);

        /**
         * É quem decide quem pode se conectar com o servidor a partir do certificado que esta no KeyStore
         * 
         * O getDefaultAlgorithm pede ao Java o algoritmo padrão de validação de certificados do sistema (geralmente chamado de PKIX)
         */
        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(caKeyStore);

        /**
         * O SSLContext é o ambiente, o motor principal que gerencia toda a criptografia da conexão.
         * O "TLSv1.2" especifica exatamente qual versão do protocolo de segurança usar. 
         * O TLS (sucessor do antigo SSL) garante que os dados trafeguem pela rede de forma totalmente embaralhada e segura. (https://www.cloudflare.com/pt-br/learning/ssl/what-is-ssl/)
         * 
         * O método init configura o motor de criptografia e aceita três parâmetros (KeyManager[], TrustManager[], SecureRandom):
         *      - O KeyManager serve para você enviar o seu certificado para o servidor (autenticação mútua). Como está null, significa que a aplicação é apenas um cliente comum que não precisa se identificar com um certificado próprio para o servidor.
         *      - Define quem é confiável através do certificado
         *      - Gerador de números aleatórios para a criptografia. Passando null, o Java usa o gerador padrão do sistema.
         * 
         * O método getSocketFactory() devolve uma Fábrica de Sockets.
         * Um "Socket" é a ponta de um cano de comunicação na rede. 
         * Ao retornar o SocketFactory configurado com o seu sslContext, está sendo entregue para o resto da aplicação
         * uma ferramenta capaz de criar conexões que automaticamente usam criptografia TLS v1.2 e automaticamente confiam no servidor EMQX.
         */
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmFactory.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }
}
