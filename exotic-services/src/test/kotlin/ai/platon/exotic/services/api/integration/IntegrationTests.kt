package ai.platon.exotic.services.api.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests(
    @Autowired val restTemplate: TestRestTemplate
) {
    @Test
    fun `if get a product then success`() {
//        val entities: ResponseEntity<String> = restTemplate.getForEntity("/api/products/B08FR5NTDR")
//        println(entities.body)
//
//        assertThat(entities.statusCode).isEqualTo(HttpStatus.OK)
//        assertThat(entities.body).isNotEmpty

//        val entities: ResponseEntity<JacksonPageImpl<AsinSyncUtf8mb4Entity>> = restTemplate.getForEntity("/pages")
//
//        assertThat(entities.statusCode).isEqualTo(HttpStatus.OK)
//        assertThat(entities.body).isNotEmpty
    }
}
