package br.puc.campinas.co2nect;

import br.puc.campinas.co2nect.mqtt.MqttSubscriber;
import br.puc.campinas.co2nect.repository.ReadingRepository;
import br.puc.campinas.co2nect.service.SensorReadingService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        loadEnv();

        String dbUrl;
        String dbUser;
        String dbPass;

        String brokerUrl;
        String mqttUser;
        String mqttPass;
        String mqttCaCert;

        int profile;
        String envProfile = System.getenv("APP_PROFILE");

        // Precisa disso para o caso de rodar esse servidor na nuvem para ele nn tentar pegar a proxima linha no terminal. Lá define o APP_PROFILE.
        if (envProfile != null && !envProfile.trim().isEmpty()) {
            profile = envProfile.equalsIgnoreCase("localhost") ? 1 : 2;
        } else {
            profile = promptProfile();
        }

        switch (profile) {
            case 1:
                dbUrl = "jdbc:postgresql://localhost:5432/co2nect_db";
                dbUser = "co2nect_user";
                dbPass = "co2nect_pass";

                brokerUrl = "tcp://localhost:1883";
                mqttUser = null;
                mqttPass = null;
                mqttCaCert = null;
                
                break;
            case 2:
                dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/co2nect_db");
                dbUser = getEnv("DB_USER", "co2nect_user");
                dbPass = getEnv("DB_PASSWORD", "co2nect_pass");

                brokerUrl = getEnv("MQTT_BROKER_URL", "tcp://localhost:1883");
                mqttUser = getEnv("MQTT_USER", null);
                mqttPass = getEnv("MQTT_PASSWORD", null);
                mqttCaCert = getEnv("MQTT_CA_CERT", null);

                break;
            default:
                dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/co2nect_db");
                dbUser = getEnv("DB_USER", "co2nect_user");
                dbPass = getEnv("DB_PASSWORD", "co2nect_pass");

                brokerUrl = getEnv("MQTT_BROKER_URL", "tcp://localhost:1883");
                mqttUser = getEnv("MQTT_USER", null);
                mqttPass = getEnv("MQTT_PASSWORD", null);
                mqttCaCert = getEnv("MQTT_CA_CERT", null);
                
                break;
        }

        String[] topics = { "co2nect/+/telemetry", "co2nect/+/status" };

        ReadingRepository repository = new ReadingRepository(dbUrl, dbUser, dbPass);
        SensorReadingService service = new SensorReadingService(repository);
        MqttSubscriber subscriber = new MqttSubscriber(brokerUrl, mqttUser, mqttPass, mqttCaCert, topics, service);

        /**
         * - Quando Ctrl + C é apertado no terminal (ou quando o Docker manda um comando docker stop), o sistema operacional envia um sinal de morte súbita (SIGINT ou SIGTERM) para a JVM.
         *      - Se nada for feito, o Java fecha os sockets abruptamente sem avisar ninguém.
         *      - O Mosquitto fica sem saber se o Java caiu, e os arquivos de conexão podem ficar corrompidos.
         * 
         * - O método addShutdownHook registra uma Thread adormecida dentro da Máquina Virtual do Java (JVM).
         *      - Essa thread nunca roda enquanto o programa estiver funcionando normalmente.
         *      - Mas, no milissegundo em que a JVM percebe que alguém apertou Ctrl + C para fechar o programa, ela pausa o desligamento, acorda essa thread e executa o código dela.
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[App] Shutting down CO2NNECT Server...");
            subscriber.stop();
        }));

        try {
            subscriber.start();
            System.out.println("[App] CO2NNECT Server is running. Press Ctrl+C to terminate.");

            /**
             * - No Java, quando o método public static void main chega na sua última linha (a chave de fechamento }), a thread principal morre.
             *      - Quando o subscriber.start() é chamado, a biblioteca Paho cria threads de segundo plano (background threads) para escutar o Mosquitto.
             *      - Se o main chegasse ao fim do arquivo, a thread principal morreria imediatamente. Dependendo de como a biblioteca configura as threads internas, a JVM entenderia q o programa terminou e fecharia tudo 1 segundo depois de dar o start.
             * 
             *  - Quando se escreve: Thread.currentThread().join();
             *      - Vc diz à thread principal do main para esperar pelo shutdown.
             *      - Como ela nunca pode morrer enquanto estiver esperando, ela entra em estado de repouso absoluto (WAITING).
             *      - Ela não gasta ciclos de processamento por isso é melhor q um: while(true) Thread.sleep(1000)).
             *      - O processo Java fica vivo, permitindo que o Paho e o PostgreSQL trabalhem nas threads de evento em segundo plano até alguém apertar Ctrl + C
             */
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("[App] Fatal error starting server: " + e.getMessage());
        }
    }

    /**
     * Recupera o valor de uma configuração respeitando uma ordem de precedência hierárquica (Fallback):
     * 
     * 1. Variáveis de ambiente do Sistema Operacional / Docker (System.getenv).
     * 2. Propriedades carregadas na JVM a partir do arquivo .env (System.getProperty).
     * 3. Valor padrão (defaultValue) fornecido como contingência.
     * 
     * Esse padrão (12-Factor App) garante flexibilidade: em produção (Docker/Nuvem), 
     * as variáveis do contêiner têm prioridade máxima; em desenvolvimento local, 
     * o arquivo .env é utilizado; e na ausência de ambos, o sistema não quebra 
     * graças ao valor padrão.
     * 
     * @param key          chave identificadora da configuração (ex: "DB_URL", "MQTT_BROKER_URL")
     * @param defaultValue valor padrão a ser retornado caso a variável não exista ou esteja em branco
     * @return o valor configurado encontrado ou o defaultValue se nenhuma fonte definir a chave
     */
    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    /**
     * Carrega variáveis de ambiente a partir do arquivo .env, caso exista.
     * Prioriza variáveis já definidas no sistema operacional (System.getenv),
     * injetando as do arquivo nas propriedades da JVM (System.setProperty).
     */
    public static void loadEnv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("co2nect-server/.env");
        }
        if (!envFile.exists()) {
            return;
        }

        /**
         * BufferedReader + FileReader ➔ Feito para TEXTO: try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8)))
         * O FileReader abre o arquivo e lê caractere por caractere (com o padrão StandardCharsets.UTF_8 garantindo que acentos como á, ç, õ não quebrem).
         * O BufferedReader funciona como uma "memória cache". Em vez de ir ao disco rígido buscar uma letra de cada vez (o que é muito lento), ele traz um bloco inteiro de texto para a memória RAM de uma vez só. 
         * Além disso, ele permite ler o arquivo linha por linha usando o método .readLine().
         * 
         * FileInputStream ➔ Feito para BYTES: try (InputStream fis = new FileInputStream(certFile))
         * Ele lê Bytes puros (zeros e uns, dados binários), sem se importar se aquilo forma uma palavra ou não.
         * Ele abre o arquivo e entrega os bytes exatamente como estão guardados no disco rígido, em formato binário.
         */
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Ignora linhas vazias ou comentários
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();

                    // Remove aspas simples ou duplas ao redor do valor, se houver
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        if (value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }

                    // Apenas define como System.getProperty se ainda não existir
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[App] Failed to read .env file:  " + e.getMessage());
        }
    }

    private static int promptProfile() {
        System.out.println("========================================");
        System.out.println("  CO2NNECT - Seleção de Ambiente");
        System.out.println("  1. Local (Docker: Mosquitto + Postgres)");
        System.out.println("  2. Nuvem (EMQX Cloud + Supabase)");
        System.out.println("========================================");
        System.out.println("Escolha o perfil [1 ou 2] (padrão: 2): ");

        // Nn pode dar close() nessa bomba senão eu nn vou poder mais pegar input no terminal em nenhuma parte do código, o Suppres é para tirar o warn
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String input = scanner.nextLine();
            
            if (input.isEmpty() || input.equals("2")) {
                return 2;
            } else if (input.equals("1")) {
                return 1;
            } else {
                System.out.print("Opção inválida! Digite 1 ou 2 (pressione Enter para padrão 2): ");
            }
        }
    }
}
