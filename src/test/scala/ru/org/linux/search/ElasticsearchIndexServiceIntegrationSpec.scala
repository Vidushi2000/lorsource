/*
 * Copyright 1998-2019 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.search

import java.nio.file.Files

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.mockito.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation._
import org.springframework.stereotype.{Repository, Service}
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import play.api.libs.ws.StandaloneWSClient
import ru.org.linux.auth.FloodProtector
import ru.org.linux.search.ElasticsearchIndexService.MessageIndex

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration]))
class ElasticsearchIndexServiceIntegrationSpec extends SpecificationWithJUnit {
  new TestContextManager(this.getClass).prepareTestInstance(this)

  @Autowired
  var indexService: ElasticsearchIndexService = _

  @Autowired
  var elastic: ElasticClient = _

  "ElasticsearchIndexService" should {
    "create index" in {
      indexService.createIndexIfNeeded()

      val exists = elastic execute { indexExists(MessageIndex) } await

      exists.result.isExists must beTrue
    }
  }
}

@Configuration
@ImportResource(Array("classpath:common.xml", "classpath:database.xml"))
@ComponentScan(
  basePackages = Array("ru.org.linux"),
  lazyInit = true,
  useDefaultFilters = false,
  includeFilters = Array(
    new ComponentScan.Filter(
      `type` = FilterType.ANNOTATION,
      value = Array(classOf[Service], classOf[Repository])))
)
class SearchIntegrationTestConfiguration {
  class LocalNodeProvider {
    val node = ElasticsearchConfiguration.createEmbedded("test-elastic", Files.createTempDirectory("test-elastic").toFile.getAbsolutePath)

    def close(): Unit = node.stop(true)
  }

  @Bean(destroyMethod="close")
  def elasticNode: LocalNodeProvider = new LocalNodeProvider()

  @Bean
  def elasticClient(node: LocalNodeProvider): ElasticClient = {
    ElasticClient(ElasticsearchClientUri("localhost", 9200))
  }

  @Bean
  def floodProtector: FloodProtector = Mockito.mock(classOf[FloodProtector])

  @Bean
  def httpClient: StandaloneWSClient = Mockito.mock(classOf[StandaloneWSClient])
}