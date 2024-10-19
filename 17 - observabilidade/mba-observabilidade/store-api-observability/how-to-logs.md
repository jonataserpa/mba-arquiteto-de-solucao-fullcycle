# Hands on | Logs

## passo 1
**Implementar a interface de logs em cada classe.**

Utilize um objeto estático para garantir reuso para todas as instâncias

```java
private static final Logger log = LoggerFactory.getLogger(NomeDaClasse.class);
```

## passo 2
**Implementar os logs conforme necessário**

```java
// o método permite interpolar certo conteúdo com o log
log.info("mensagem do log - parametro: {}", objeto);
log.info("mensagem do log - p1: {}, p2: {}", arg1, arg2);

// logs de debug
log.debug("agora somando os dois números: {} + {} = {}", v1, v2, v3);

// exemplo de exceção
log.error("mensagem de erro - detalhe da exceção: {}", ex);
```


## passo 3
**Usar uma biblioteca específica**

No hands on vamos usar o LogBack, e as suas dependências são as seguintes:

```xml
<!-- LogBack core, possui as funcionalidades básicas de escrita e formatação -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-core</artifactId>
    <version>1.5.6</version>
</dependency>

<!-- funções mais sofisticadas de logging, como formatação em JSON -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

## passo 4
**Usar `MDC` para adicionar dados de contexto.**

Para aplicações Web/API, você pode usar um interceptor nativo do framework - ex: `HandlerInterceptor` do Spring (Java); já aplicações "worker", como batch ou que recebem estímulos de fila ou tópico, você pode usar fazer o enriquecimento do MDC no primeiro ponto de contato da aplicação - ex: classe/método `main()`

```java
@Component
class RequestMdcInterceptor implements HandlerInterceptor {

    // capturando propriedades geradas no build
    @Autowired
    private BuildProperties buildProperties;

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler) throws Exception {
        MDC.put("appName", buildProperties.getName());
        MDC.put("appVersion", buildProperties.getVersion());
        MDC.put("appBuildDate", buildProperties.getTime().toString());
        MDC.put("traceId", request.getHeader("x-trace-id"));
        MDC.put("host", request.getHeader("Host"));
        return true;
    }
}

// no caso do Spring, é necessário registrar o intereptor para ser reconhecido
@Configuration
class InterceptorsAdapterConfig implements WebMvcConfigurer {

    @Autowired
    private RequestMdcInterceptor requestMdcInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(requestMdcInterceptor);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
```

Para capturar os dados de build através da classe `BuildProperties`, é necessário incluir mais alguns dados de build no plugin do maven.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <!-- execution para gerar o arquivo de metadados
                     que a classe BuildProperties vai ler -->
                <execution>
                    <id>build-info</id>
                    <goals>
                        <goal>build-info</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```



## passo 5
**Aplicar as configurações de formato e destino dos logs.**

No caso do LogBack, uma das formas de instrumentação é através da parametrização do arquivo `src/main/resources/logback.xml`, ex:

```xml
<configuration>
    <!-- é possível importar as classes em vez de referenciar toda vez o caminho completo -->
    <import class="ch.qos.logback.core.ConsoleAppender" />
    <import class="ch.qos.logback.core.FileAppender" />
    <import class="ch.qos.logback.classic.encoder.PatternLayoutEncoder" />
    <import class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder" />

     <timestamp key="timestamp" datePattern="yyyyMMdd'T'HHmm"/>

    <!-- appender específico para arquivo -->
    <appender name="FILE" class="FileAppender">
        <!-- local onde o arquivo será gravado
            OBS: é possível adicionar varávis de ambiente para customizar o nome e o local do arquivo gerado (ex: data/hora)
        -->
        <file>logs/application-${timestamp}.log</file>
        <encoder class="PatternLayoutEncoder">
            <!-- 
            pattern de escrita de cada linha de log
                * %d{HH:mm:ss.SSSZZ} - timestamp do evento de log
                * [%thread] - thread da aplicação em que o log foi gerado
                * %-5level - nível de log (ERROR, INFO, DEBUG, etc.)
                * %X{traceId} - variável específica "traceID" no MDC
                                poderia colocar %X para exibir todas
                                as variáveis
                * %X{host} - variável específica "host" no MDC
                            poderia colocar %X para exibir todas
                            as variáveis
                * %logger{36} - classe em que o log foi gerado
                                nome limitado a 36 caracteres
                * %msg%n - mensagem acompanhada de exceção + stack trace
            -->
            <pattern>
                %d{HH:mm:ss.SSSZZ} [%thread] %-5level %X{traceId} %X{host} %logger{36} - %msg%n
            </pattern>
        </encoder>
    </appender>

    <!-- é possível ter múltiplos appenders -->
    <appender name="STDOUT_PLAIN" class="ConsoleAppender">
        <encoder class="PatternLayoutEncoder">
            <!-- ... e com formatos diferentes -->
            <pattern>
                %d{HH:mm:ss.SSSZZ} [%thread] %-5level %X %logger{36} - %msg%n
            </pattern>
        </encoder>
    </appender>

    <appender name="STDOUT_JSON" class="ConsoleAppender">
        <!-- classe net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder permite usar formato JSON -->
        <encoder class="LoggingEventCompositeJsonEncoder">
            <providers>
                <!-- a biblioteca conhece o campo timestamp... -->
                <timestamp>
                    <!-- ... mas é possível customizar o nome e até o formato -->
                    <fieldName>ts</fieldName>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <loggerName>
                    <fieldName>logger</fieldName>
                </loggerName>
                <logLevel>
                    <fieldName>level</fieldName>
                </logLevel>
                <callerData>
                    <classFieldName>class</classFieldName>
                    <methodFieldName>method</methodFieldName>
                    <lineFieldName>line</lineFieldName>
                    <fileFieldName>file</fileFieldName>
                </callerData>
                <threadName>
                    <fieldName>thread</fieldName>
                </threadName>
                <mdc />
                <stackTrace>
                    <fieldName>stack</fieldName>
                </stackTrace>
                <message>
                    <fieldName>msg</fieldName>
                </message>

                <!-- 
                    é possível capturar argumentos dos métodos em que o log foi registrado
                    OBS: cuidado com as políticas de segurança e privacidade de dados  -->
                <arguments>
                    <includeNonStructuredArguments>true</includeNonStructuredArguments>
                    <nonStructuredArgumentsFieldPrefix>argument:</nonStructuredArgumentsFieldPrefix>
                </arguments>
            </providers>
        </encoder>
    </appender>

    <!-- 
        mesmo tendo uma configuração global de nível de log, 
        é possível customizar os níveis segundo o package de cada
        classe que estive logando -->
    <logger name="ch.qos.logback" level="WARN" />
    <logger name="org.mortbay.log" level="WARN" />
    <logger name="org.springframework" level="INFO" />
    <logger name="org.springframework.beans" level="WARN" />

    <!-- por exemplo para a aplicação estou habilitando a opção de DEBUG -->
    <logger name="store" level="DEBUG" />

    <!-- 
        aqui é onde você habilita/desabilita quais Appenders vai utilizar
        o que facilita a manutenção, sem ter que ficar movendo código -->
    <root level="INFO">
        <!-- OBS: Appender STDOUT_PLAIN está desativado-->
        <!--<appender-ref ref="STDOUT_PLAIN" />-->

        <!-- é possível utilizar vários Appenders simultaneamente -->
        <appender-ref ref="STDOUT_JSON" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```


# Plus: integrando ao Splunk Search via Docker

## passo 1 - subir a infra
Suba a infra do Splunk, conforme exemplos na documentação: https://splunk.github.io/docker-splunk/EXAMPLES.html

## passo 2 - criar índice e token

Realize os procedimentos a seguir e anote:
* ***token*** do HTTP Collector
* ***índice*** que será usado na busca 

### Splunk: criando índice

1. vá para o menu `Settings`
2. no grupo `Data`, vá para `Indexes`
3. clique em `New Index`
4. informe o nome do índice em `Index Name` (ex: logs_vendorservice, raw_salesapp)
5. parametrize demais dados que fizerem sentido, ou apenas deixe tudo como default
6. clique em `Save`


### Splunk: configuração de token

1. vá para o menu `Settings`
2. no grupo `Data`, vá para `Data inputs`
3. em `Local inputs` > `Type`, clique em `+ Add new` na opção `HTTP Event Collector`
4. informe o nome em `Name` que seja possível identificar (sugestão: utilize um nome similar ao índice para facilitar a gestão)
5. clique em `Next`
6. selecione o índice default (criado anteriormente)
7. clique em `Review` e depois em `Submit`


## passo 3 - instrumentação
Configure o LogDriver para a aplicação. Exemplo utilizando `docker-compose.yml`:

```yaml
services:
  app:
    #...
    logging:
      driver: splunk
      options:
        splunk-url: http://<host>:8088
        splunk-index: <index name>
        splunk-token: <token xxxx-yyyy-zzzz>
        splunk-source: <app name>
        splunk-format: "json"
        splunk-insecureskipverify: "true"
        splunk-verify-connection: "false"
```

## passo 3
Vá para `Apps` > `Searching & Reporting` e faça uma busca no splunk.

```shell
index=logs_storeapp
| spath
| search properties.appName=store-observability properties.appVersion="0.0.*" severity=ERROR
```



# Plus: integrando ao Elastic Search via Docker

## passo 1
Instale o pluging do Elastic Search para integrá-lo ao log driver.

```sh
docker plugin install elastic/elastic-logging-plugin:8.7.1
```

## passo 3 - subir a infra
Suba a infra do ELK conforme a documentação oficial:
https://www.elastic.co/blog/getting-started-with-the-elastic-stack-and-docker-compose

Esse procedimento irá provisionar a infraestrutura com Logstash, Elastic Search e Kibana.

## passo 3
Configure o LogDriver para a aplicação no `docker-compose.yml`, utilizando os parâmetros obtidos na instalação do ELK.

```yaml
services:
  app:
    logging:
    driver: elastic/elastic-logging-plugin:8.7.1
    options:
      hosts: https://localhost:9200
      user: elastic
      password: changeme
      index: storeapp
```

# Plus: integrando ao Splunk com Appender do Logback
ref: https://github.com/splunk/splunk-library-javalogging/blob/main/src/test/resources/logback.xml

## passo 1
Além das bibliotecas do Logstash que adicionamos anteriormente, inclus a biblioteca de logging do Splunk no `pom.xml`:

```xml
<dependency>
    <groupId>com.splunk.logging</groupId>
    <artifactId>splunk-library-javalogging</artifactId>
    <version>1.9.0</version>
</dependency>
```

Aparentemente o Splunk não está disponível mais no repositório do Maven. Para baixar suas bibliotecas, é necessário incluir o link do repositório do Artifactory do Splunk no arquivo de `.m2/settings.xml` ou no próprio `pom.xml`:

```xml
<repositories>
    <!-- repositório do maven (padrão) -->
    <repository>
        <id>maven2</id>
        <name>Maven 2</name>
        <url>https://repo.maven.apache.org/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>

    <!-- repositório adicional: Splunk -->
    <repository>
        <id>splunk</id>
        <name>Splunk Artifactory</name>
        <url>https://splunk.jfrog.io/splunk/ext-releases-local</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```


## passo 2
Configure o arquivo `logback.xml` da seguinte maneira:

```xml
<configuration>

    <import class="net.logstash.logback.layout.LogstashLayout" />
    <import class="com.splunk.logging.HttpEventCollectorLogbackAppender" />

    <appender name="splunk_http" class="HttpEventCollectorLogbackAppender">
        <url>http://<HOST>:8088</url>
        <token>TOKEN</token>
        <sourcetype>logback</sourcetype>
        <index>INDICE</index>
        <messageFormat>json</messageFormat>
        <middleware>HttpEventCollectorUnitTestMiddleware</middleware>
        <connectTimeout>5000</connectTimeout>
        <terminationTimeout>2000</terminationTimeout>
        <layout class="LogstashLayout" />
    </appender>

    <root level="INFO">
        <appender-ref ref="splunk_http" />
    </root>

</configuration>
```

**OBS**: os parâmetros `<url>`, `<token>` e `<index>` devem ser obtidos conforme ambiente do Spluk. Especificamente para o token e index, siga o passo a passo a seguir.

### Splunk: criando índice

1. vá para o menu `Settings`
2. no grupo `Data`, vá para `Indexes`
3. clique em `New Index`
4. informe o nome do índice em `Index Name` (ex: logs_vendorservice, raw_salesapp)
5. parametrize demais dados que fizerem sentido, ou apenas deixe tudo como default
6. clique em `Save`


### Splunk: configuração de token

1. vá para o menu `Settings`
2. no grupo `Data`, vá para `Data inputs`
3. em `Local inputs` > `Type`, clique em `+ Add new` na opção `HTTP Event Collector`
4. informe o nome em `Name` que seja possível identificar (sugestão: utilize um nome similar ao índice para facilitar a gestão)
5. clique em `Next`
6. selecione o índice default (criado anteriormente)
7. clique em `Review` e depois em `Submit`


## passo 3
Vá para `Apps` > `Searching & Reporting` e faça uma busca no splunk.

```shell
index=logs_storeapp
| spath
| search properties.appName=store-observability properties.appVersion="0.0.*" severity=ERROR
```


# Plus: integrando ao Elastic Search com Appender do LogBack
ref: https://github.com/internetitem/logback-elasticsearch-appender

## passo 1
Instale a dependência no `pom.xml`

```xml
<!-- logback appender - ElasticSearch -->
<dependency>
    <groupId>com.internetitem</groupId>
    <artifactId>logback-elasticsearch-appender</artifactId>
    <version>1.6</version>
</dependency>
```

## passo 2
Configure o appender no `logback.xml`

```xml
<configuration>
    <import class="com.internetitem.logback.elasticsearch.ElasticsearchAppender" />
    
    <appender name="ELASTIC" class="ElasticsearchAppender">
        <url>https://localhost:9200</url>
        <index>logs-%date{yyyy-MM-dd}</index>
        <type>tester</type>
        <loggerName>es-logger</loggerName> <!-- optional -->
        <errorLoggerName>es-error-logger</errorLoggerName> <!-- optional -->
        <connectTimeout>30000</connectTimeout> <!-- optional (in ms, default 30000) -->
        <errorsToStderr>false</errorsToStderr> <!-- optional (default false) -->
        <includeCallerData>false</includeCallerData> <!-- optional (default false) -->
        <logsToStderr>false</logsToStderr> <!-- optional (default false) -->
        <maxQueueSize>104857600</maxQueueSize> <!-- optional (default 104857600) -->
        <maxRetries>3</maxRetries> <!-- optional (default 3) -->
        <readTimeout>30000</readTimeout> <!-- optional (in ms, default 30000) -->
        <sleepTime>250</sleepTime> <!-- optional (in ms, default 250) -->
        <rawJsonMessage>false</rawJsonMessage> <!-- optional (default false) -->
        <includeMdc>true</includeMdc> <!-- optional (default false) -->
        <maxMessageSize>100</maxMessageSize> <!-- optional (default -1 -->
        <authentication class="com.internetitem.logback.elasticsearch.config.BasicAuthentication" /> <!-- optional -->
        <properties>
            <property>
                <name>host</name>
                <value>${HOSTNAME}</value>
                <allowEmpty>false</allowEmpty>
            </property>
            <property>
                <name>severity</name>
                <value>%level</value>
            </property>
            <property>
                <name>thread</name>
                <value>%thread</value>
            </property>
            <property>
                <name>stacktrace</name>
                <value>%ex</value>
            </property>
            <property>
                <name>logger</name>
                <value>%logger</value>
            </property>
        </properties>
        <headers>
            <header>
                <name>Content-Type</name>
                <value>application/json</value>
            </header>
        </headers>
    </appender>

    <logger name="es-logger" level="INFO" additivity="false">
        <appender name="ES_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <!-- ... -->
            <encoder>
                <pattern>%msg</pattern> <!-- This pattern is important, otherwise it won't be the raw Elasticsearch format anyomre -->
            </encoder>
        </appender>
    </logger>

    <root level="info">
        <appender-ref ref="ELASTIC" />
    </root>
</configuration>
```