# Hands on | Tracing

Há várias formas de coletar e enviar dados de tracing de aplicação Java para o Jaeger. Você pode utilizar **Micrometer**, **Spring Cloud Sleuth** e **OpenTelemetry** com ou sem Java Agent.

Neste hands on vamos utilizar o OpenTelemetry sem Java Agent. 

## Infra

Exemplo de estrutura do `docker-compose.yml` para provisionar o serviço do Jaeger:

```yaml
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: jaeger
    restart: unless-stopped
    ports:
      - "16686:16686" # the jaeger UI
      - "4318:4318"
```

Execute o comando para subir a infraestrutura localmente:

```shell
docker compose up -d
```

Para validar, acesse Jaeger UI pelo browser: `http://localhost:16686`.


## Instrumentação 

Adicione as dependências no `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- captura de eventos de tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- captura de eventos de tracing via micrometer -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- collector do open telemetry para integração com jaeger -->
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-spring-boot</artifactId>
    <version>1.0.2</version>
</dependency>
```

No `docker-compose.yml` da aplicação principal, inclua as variáveis seguir:

```yaml
environment:
  MANAGEMENT_OTLP_TRACING_ENDPOINT: http://host.docker.internal:4318/v1/traces
  MANAGEMENT_TRACING_SAMPLING_PROBABILITY: 1.0
```


## Considerações

### comunicação HTTP
Ao criar um `@Bean` do `RestTemplate` para comunicação inter-processos, garanta que ele seja criado usando `RestTemplateBuilder`, conforme exemplo a seguir: 

```java
@Bean
RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
}
```
Caso contrário, os dados de contexto não serão passados corretamente entre as requisições.

**OBS**: `FeignCliet` pode não funcionar corretamente, sendo potencialmente necessário fazer alguma implementação utilizando **AOP**.  

## banco de dados
Se quiser coletar traces de comunicação com o banco de dados, além do uso apropriado do `spring-boot-starter-data-jpa`, utilize a biblioteca a seguir:

```xml
<!-- collector do open telemetry para integração com jaeger -->
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-spring-boot</artifactId>
    <version>1.0.2</version>
</dependency>
```



## Docs
* detalhes jaeger: https://www.jaegertracing.io/docs/1.6/deployment/
* aplicação de exemplo: https://github.com/jaegertracing/jaeger/tree/main/examples/hotrod
* integração com Micrometer + OTel: https://medium.com/cloud-native-daily/how-to-send-traces-from-spring-boot-to-jaeger-229c19f544db
* integração com Open Tracing: https://github.com/opentracing-contrib/java-spring-jaeger
