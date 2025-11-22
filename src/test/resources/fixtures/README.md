# Test Fixtures

This directory contains JSON fixtures for testing. These files mirror the structure of production seed data but contain **test data only** - no real credentials or sensitive information.

## Files

### Core Data
- **`categories.json`** - Test category data (copied from seeds)
- **`repositories.json`** - Test repository data
- **`comparisons.json`** - Test comparison data
- **`users.json`** - Test user accounts

### Sensitive Data (Test Only)
- **`api_keys.json`** - Test API keys with dummy BCrypt hashes (not real keys)
- **`whitelisted_users.json`** - Test whitelisted users with fake GitHub IDs

### Relational Data
- **`repository_categories.json`** - Test repository-category associations
- **`comparison_repositories.json`** - Test comparison-repository associations
- **`comparison_categories.json`** - Test comparison-category associations
- **`analyses.json`** - Test AI analysis data
- **`ai_costs.json`** - Test AI cost tracking data

## Usage in Tests

### Loading Fixtures in Tests

```kotlin
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource

class MyTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `test with fixtures`() {
        // Load fixture
        val json = ClassPathResource("fixtures/categories.json").inputStream.readAllBytes()
        val categories: List<Category> = mapper.readValue(json)

        // Use in test
        categories.forEach { category ->
            categoryRepository.save(category)
        }

        // Assert test behavior
        assertThat(categoryRepository.count()).isEqualTo(categories.size)
    }
}
```

### Database Test Setup

```kotlin
@TestConfiguration
class TestDataConfiguration {

    @Bean
    fun loadTestFixtures(
        categoryRepository: CategoryRepository,
        userRepository: UserRepository,
        apiKeyRepository: ApiKeyRepository
    ): CommandLineRunner {
        return CommandLineRunner {
            // Load fixtures only in test profile
            if (environment.activeProfiles.contains("test")) {
                loadFixture("categories.json", categoryRepository)
                loadFixture("users.json", userRepository)
                loadFixture("api_keys.json", apiKeyRepository)
            }
        }
    }

    private fun <T> loadFixture(filename: String, repository: JpaRepository<T, *>) {
        val json = ClassPathResource("fixtures/$filename").inputStream.readAllBytes()
        val entities: List<T> = jacksonObjectMapper().readValue(json)
        repository.saveAll(entities)
    }
}
```

## Important Notes

⚠️ **These are TEST fixtures - never use in production!**

- API keys contain dummy BCrypt hashes (not real keys)
- GitHub IDs are fake/test values
- Email addresses use `@example.com` domain
- All data is safe to commit to git

## Differences from Seeds

| Seeds (`src/main/resources/seeds/`) | Fixtures (`src/test/resources/fixtures/`) |
|-------------------------------------|-------------------------------------------|
| Used for development database seeding | Used for automated tests |
| May contain real-ish looking data | Contains obvious test data |
| **No sensitive data** (api_keys, whitelisted_users deleted) | **Includes test versions** of sensitive data |
| Loaded via `./gradlew dbSeed` | Loaded in test setup methods |

## Updating Fixtures

When the database schema changes:

1. Update seed files in `src/main/resources/seeds/`
2. Copy to fixtures: `cp src/main/resources/seeds/*.json src/test/resources/fixtures/`
3. Update test-specific fixtures (`api_keys.json`, `whitelisted_users.json`)
4. Run tests to verify: `./gradlew test`

## Security

✅ **Safe to commit** - All data is fake test data
✅ **No real credentials** - API key hashes are dummy values
✅ **No PII** - All emails use `@example.com`
✅ **Obvious test data** - GitHub usernames like "test-user-1", "octocat"
