# Hands on | Metrics

## passo 1 - dependências
Inclua as dependências no arquivo `pom.xml`:

```xml
<!-- built-in endpoints, como: /metrics e /health -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- micrometer para disponibilizar métrics no formato do prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```


## passo 2 - properties
Exponha a rota do prometheus (e outras rotas) via arquivo `application.yml` / `application.properties` ou variável de ambiente. Ex via `application.yml`:

```yaml
# expondo rotas: /actuator/<health, info, metrics, prometheus>
management.endpoints.web.exposure.include: health,info,metrics,prometheus

# o base path padrão é /actuator, mas você pode alterar através do seguinte parâmetro
management.endpoints.web.base-path: /actuator

# para exibir o histograma de latência de requisições, use a seguinte configuraçao
# https://docs.spring.io/spring-boot/reference/actuator/metrics.html#actuator.metrics.export.prometheus
management.metrics.distribution.slo[http.server.requests]: "25ms, 50ms, 100ms, 200ms, 400ms, 800ms, 1500ms"
```


## passo 3 - testando as rotas
Acesse a rota `/actuator` e confira se as rotas `/actuator/metrics` e `/actuator/prometheus` estão habilitadas. Se estiverem, elas aparecerão na lista, conforme exemplo:

```json
{
    "_links": {
        "self": {
            "href": "http://localhost:8080/actuator",
            "templated": false
        },
        "health-path": {
            "href": "http://localhost:8080/actuator/health/{*path}",
            "templated": true
        },
        "health": {
            "href": "http://localhost:8080/actuator/health",
            "templated": false
        },
        "info": {
            "href": "http://localhost:8080/actuator/info",
            "templated": false
        },
        "prometheus": {
            "href": "http://localhost:8080/actuator/prometheus",
            "templated": false
        },
        "metrics-requiredMetricName": {
            "href": "http://localhost:8080/actuator/metrics/{requiredMetricName}",
            "templated": true
        },
        "metrics": {
            "href": "http://localhost:8080/actuator/metrics",
            "templated": false
        }
    }
}
```

Você pode acessar as rotas diretamente, ex:

> http://localhost:8080/actuator/metrics

> http://localhost:8080/actuator/prometheus



## passo 4 - uso da rota /metrics

É possível realizar queries sobre as métricas através da rota `/atuator/metrics`.

> Query: listando conexões do Hikari
> http://localhost:28080/actuator/metrics/hikaricp.connections

Query: filtro de JVM memory por `area = heap`
> http://localhost:8080/actuator/metrics/jvm.memory.max?tag=area:heap

Query: filtro de JVM memory por `id = heap`
> http://localhost:8080/actuator/metrics/jvm.memory.max?tag=area:heap

Query: filtro combinado de JVM memory por `area = heap` e `id = G1 Old Gen`
> http://localhost:8080/actuator/metrics/jvm.memory.max?tag=area:heap&tag=id:G1+Old+Gen



## passo 5 - uso da rota /prometheus

Em vez de ter que realizar queries uma a uma, o micrometer já expõe essas métricas em uma única consulta, no formato compatível com o prometheus.

Vamos vamos configurar a infra do Prometheus para executar localmente via `docker compose`.

Configure o arquivo `prometheus/prometheus.yml` conforme o exemplo a seguir, para instruir o prometheus sobre o local onde ele deve buscar as métricas:

```yaml
scrape_configs:

  - job_name: 'store-api-pull'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 3s
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          application: 'store-api'
```

Configure o `docker-compose.yml` com o serviço do prometheus, conforme exemplo a seguir:

```yaml
services:

  prometheus:
    image: prom/prometheus
    container_name: prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    ports:
      - 9090:9090
    restart: unless-stopped
    volumes:
      - ./prometheus:/etc/prometheus
      - prom_data:/prometheus

volumes:
  prom_data:
```


Após executar `docker compose up -d` para subir o serviço do prometheus, com a aplicação executando, acesse `http://localhost:9090` e execute algumas queries:

```shell
# todas requisições com status 4xx e 5xx
http_server_requests_seconds_count{status =~ '4.+'}
http_server_requests_seconds_count{status =~ '5.+'}

# todas as requisições, exceto /actuator/prometheus
http_server_requests_seconds_count{uri !~ ".+prometheus"}

# múltiplos filtros: url exceto prometheus e status 2xx
http_server_requests_seconds_count{uri !~ ".+prometheus", status =~ "2.+"}

# múltiplos filtros: url exceto prometheus/metrics/health e status 2xx
http_server_requests_seconds_count{uri !~ ".+prometheus|.+metrics|.+health", status =~ "2.+"}

# requisições 2xx em relação ao tempo (resolução)
http_server_requests_seconds_sum{uri !~ ".+prometheus|.+metrics|.+health", status =~ "2.+"}
```


# Criando métricas customizadas

## passo 1 - dependência
Adicione a dependência do micrometer core:

```xml
<!-- https://mvnrepository.com/artifact/io.micrometer/micrometer-core -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

## passo 2 - MeterRegistry
Na classe onde você quer contabilizar as métricas customizadas, inclua o @Bean preferencialmente utilizando algum mecanismo de injeção de dependências.

**Exemplo 1** - construtor:

```java
private final MeterRegistry meterRegistry;

// construtor
public ClassEspecifica(final MeterRegistry meterRegistry, ...) {
  this.meterRegistry = meterRegistry;
  // ... demais configurações da classe
}
```

**Exemplo 2** - Spring Boot autowired:

```java
@Autowired
private MeterRegistry meterRegistry;
```

## passo 3 - coleta de métricas

### Counter

Pode apenas aumentar ou resetar, ex: número de requisições, quantidade de erros, etc.

```java
final Counter counterMetricaCustomizada;

// construtor
public ClassEspecifica(...) {
  // ...
  counterMetricaCustomizada = Counter.builder("<NOME DA MÉTRICA>")
    .tag("<TAG 1>", "<VALOR DA TAG 1>")
    .tag("<TAG 2>", "<VALOR DA TAG 2>")
    .description("<DESCRIÇÃO QUE EXPLICA A MÉTRICA>")
    .register(meterRegistry);
}

public void metodoDaClasse(...) {
  // lógica do método
  // ....
  counterMetricaCustomizada.increment();
}
```

### Gauge

Pode aumentar ou diminuir, útil para métricas como número de pods/tasks em um cluster, quantidade de mensagens em uma fila, etc.

```java
final AtomicInteger gaugeValueMetricaCustomizada;

// construtor
public ClassEspecifica(...) {
  // ...
  Gauge.builder("<NOME DA METRICA>", gaugeValueMetricaCustomizada::get)
    .description("<DESCRIÇÃO DA MÉTRICA>")
    .register(meterRegistry);
}

public void metodoDaClasse(...) {
  // lógica do método
  // ....
  gaugeValueMetricaCustomizada.set(<NOVO VALOR>);
}
```


# Métricas com grafana

O Prometheus permite que você realize consultas sobre as métricas capturadas e armazenadas em sua camadade storage, mas ele não consegue entregar a usabilidade e vesatilidade de um sistema especializado em UI.

Com o Grafana você pode configurar dashboards e alarmes sofisticados, utilizando o Prometheus e muitos outros sistemas como fonte de dados.

## passo 1 - infra docker

Você pode subir o serviço do Grafana utilizando o seguinte trecho de código do docker compose.

```yaml
grafana:
  image: grafana/grafana
  container_name: grafana
  ports:
    - 3000:3000
  restart: unless-stopped
  environment:
    - GF_SECURITY_ADMIN_USER=admin
    - GF_SECURITY_ADMIN_PASSWORD=grafana
  volumes:
    - ./grafana:/etc/grafana/provisioning/datasources
```

Você pode conectar o Grafana a múltiplas fontes de dados. Para integrá-lo ao Prometheus em tempo de inicialização, crie o arquivo `grafana/datasources.yml` coma seguinte estrutura (esse arquivo está sendo referenciado no trecho do docker compose):

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

## passo 2 - toques finais na aplicação

Uma solução de monitoramento mais assertiva precisa ter minimamente as métricas relacionadas aos 4 golden signals.




# Plus: Prometheus com push Gateway

Nem sempre é possível expor um endpoint que seja acessível pelo Prometheus. Por exemplo, você pode ter aplicações batch ou workers que são estimulados de forma ad-hoc, agendados ou a partir de eventos de fila, que podem executar em infraestrutura mais tradicional, como servidores/VMs Linux e Windows. E mesmo utilizando um ambiente conteinerizado, pode haver alguma limitação que torne a configuração muito complexa, por exemplo o service discovery para acesso à aplicação com as métricas, considerando a vida curta das aplicações.

Nesse caso você precisa instrumentar a aplicação para enviar proativamente as métricas ao Prometheus através do recurso chamado Push Gateway.

## passo 1 - infra docker
Inclua o serviço do `pushgateway` no `docker-compose.yml`. Ele geralmente expõe a porta `9091` para integração.

```yaml
services:
  # ....
  # outros serviços
  # ....
  pushgateway:
    image: prom/pushgateway
    container_name: pushgateway
    restart: unless-stopped
    expose:
      - 9091
    ports:
      - "9091:9091"
    labels:
    org.label-schema.group: "monitoring"
```

Configure o Prometheus para buscar as métricas na API do pushgateway no arquivo `prometheus/prometheus.yml`:

```yaml
scrape_configs:
  # ...
  # outros jobs
  # ...
  - job_name: 'pushgateway'
    honor_labels: true
    scrape_interval: 3s
    static_configs:
      - targets: ['pushgateway:9091']

```

Reinicie os serviços via docker compose, ex:

```shell
docker compose up -d
```


## passo 2 - instrumentação da aplicação
Além das dependências já adicionadas, inclua no `pom.xml` a lib `io.prometheus.simpleclient_pushgateway`:

```xml
<!-- Pushgatweay para envio proativo das métricas ao Prometheus -->
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient_pushgateway</artifactId>
</dependency>
```

No arquivo `src/main/resources/application.yml`, configure a integração com o pushgateway:

```yaml
server.port: 8080
spring.application.name: store-api
management:
  endpoints.web.exposure.include: health,info,metrics,prometheus
  prometheus:
    metrics:
      export:
        pushgateway:
          enabled: true
          base-url: http://localhost:9091
          shutdown-operation: POST
          push-rate: PT1S
          job: store-api-push
```

Por default a lib `io.prometheus.simpleclient_pushgateway` inicializa um `@Bean` (objeto) do `PrometheusPushGatewayManager`, componente responsável por integrar efetivamente com o serviço do Pushgateway. Caso queira **opcionalmente** customizar a criação desse componente, você pode criar uma classe de configuração (`@Configuration`) conforme o exemplo a seguir:

```java
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.MalformedURLException;
import java.net.URI;

@Configuration
public class PushGatewayConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    @Primary
    public PrometheusPushGatewayManager prometheusPushGatewayManager(
            CollectorRegistry collectorRegistry,
            PrometheusProperties prometheusProperties) throws MalformedURLException {

        final PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
        return new PrometheusPushGatewayManager(
                new PushGateway(URI.create(properties.getBaseUrl()).toURL()),
                collectorRegistry,
                properties.getPushRate(),
                applicationName,
                properties.getGroupingKey(),
                properties.getShutdownOperation());
    }

     @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(@Value("${spring.profiles.active:default}") String activeEnvProfile) {
        return registry -> registry.config()
                .commonTags(
                        "env", activeEnvProfile
                );
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

}
```


# Plus: Alert manager

Queries sobre consumo de memória:

```shell

# consumo de memória HEAP no último minuto
jvm_memory_used_bytes{area="heap"}[1m]

# média do consumo de memória HEAP no  último minuto 
avg_over_time(jvm_memory_used_bytes{area="heap"}[1m]))

# soma da média de consumo de memória
sum(avg_over_time(jvm_memory_used_bytes{area="heap"}[1m]))

# soma da média de consumo de memória, porém agrupando por "application" e "instance"
sum(avg_over_time(jvm_memory_used_bytes{area="heap"}[1m])) by (application,instance)

# percentual de consumo de memória do último minuto
sum(avg_over_time(jvm_memory_used_bytes{area="heap"}[1m]))by(application,instance) * 100 / sum(avg_over_time(jvm_memory_max_bytes{area="heap"}[1m]))by(application,instance) >= 80
```


```shell

# média de uso de conexões sobre o connection pool
sum(avg_over_time(hikaricp_connections_active[1m]))by(application,instance) * 100 / sum(avg_over_time(hikaricp_connections[1m]))by(application, instance)

sum(avg_over_time(jdbc_connections_active[1m]))by(application,instance) * 100 / sum(avg_over_time(jdbc_connections_max[1m]))by(application, instance)
```

```shell
sum(avg_over_time(process_cpu_usage[1m]))by(application,instance)
```


## docs
* https://github.com/prometheus/client_java
* https://github.com/docker/awesome-compose/tree/master/prometheus-grafana
* https://prometheus.io/docs/tutorials/understanding_metric_types/
* https://prometheus.io/docs/prometheus/latest/querying/basics/
