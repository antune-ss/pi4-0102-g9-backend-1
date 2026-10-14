# ADR 0002: usar Docker para padronizar a execução dos componentes

**Status:** proposto

**Contexto:** A solução reúne componentes de software com responsabilidades diferentes, incluindo o backend Java, o banco de dados e o frontend do dashboard. O grupo precisa executar e demonstrar esses componentes em um ambiente de desenvolvimento reproduzível. A configuração manual de dependências e versões em cada máquina pode gerar diferenças de ambiente e dificultar a preparação da demonstração.

**Decisão:** Executar o backend Java, o banco de dados e o frontend do dashboard em contêineres Docker separados, mantendo suas configurações documentadas e versionadas no repositório.

**Alternativas consideradas:**
- Instalar e executar todas as dependências diretamente no sistema operacional: descartada porque exigiria configurar manualmente cada ambiente e aumentaria o risco de diferenças entre as máquinas dos integrantes.
- Empacotar todos os componentes em uma única imagem sem separação de responsabilidades: descartada porque tornaria mais difícil configurar, atualizar e diagnosticar individualmente o backend, o banco de dados e o frontend.

**Consequências:**
- Positivas: torna a configuração do ambiente mais reproduzível, reduz diferenças entre máquinas e facilita iniciar os componentes necessários para executar e demonstrar o protótipo.
- Negativas: exige criar e manter arquivos de configuração, administrar rede e variáveis de ambiente dos contêineres e garantir a persistência dos dados do banco; o uso de Docker também acrescenta uma etapa de configuração e diagnóstico quando a execução falha.