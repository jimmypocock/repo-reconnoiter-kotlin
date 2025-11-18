package com.reconnoiter.api.shell.commands

import com.reconnoiter.api.repository.CategoryRepository
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class CategoryCommands(private val categoryRepository: CategoryRepository) {

    @ShellMethod(key = ["category list", "categories"], value = "List all categories")
    fun listCategories(
        @ShellOption(defaultValue = ShellOption.NULL) type: String?
    ): String {
        val categories = if (type != null) {
            categoryRepository.findByCategoryType(type)
        } else {
            categoryRepository.findAll().toList()
        }

        if (categories.isEmpty()) {
            return if (type != null) {
                "No categories found for type: $type"
            } else {
                "No categories found."
            }
        }

        val output = StringBuilder()
        output.appendLine()
        output.appendLine("📁 Categories (${categories.size})")
        output.appendLine("━".repeat(100))
        output.appendLine(String.format("%-5s %-30s %-20s %-20s",
            "ID", "Name", "Slug", "Type"))
        output.appendLine("━".repeat(100))

        categories.sortedBy { it.categoryType }.forEach { category ->
            output.appendLine(String.format("%-5d %-30s %-20s %-20s",
                category.id ?: 0,
                category.name.take(30),
                category.slug.take(20),
                category.categoryType
            ))
        }

        output.appendLine("━".repeat(100))
        output.appendLine("\nFilter by type with: category list --type <type>")
        output.appendLine("Types: problem_domain, architecture_pattern, maturity_level")

        return output.toString()
    }

    @ShellMethod(key = ["category show"], value = "Show category details by ID")
    fun showCategory(@ShellOption id: Long): String {
        val category = categoryRepository.findById(id).orElse(null)
            ?: return "❌ Category not found: $id"

        val output = StringBuilder()
        output.appendLine()
        output.appendLine("📁 Category Details")
        output.appendLine("━".repeat(80))
        output.appendLine("ID:          ${category.id}")
        output.appendLine("Name:        ${category.name}")
        output.appendLine("Slug:        ${category.slug}")
        output.appendLine("Type:        ${category.categoryType}")
        output.appendLine("Description: ${category.description ?: "N/A"}")
        output.appendLine("━".repeat(80))

        return output.toString()
    }

    @ShellMethod(key = ["category count"], value = "Count categories by type")
    fun countCategories(): String {
        val total = categoryRepository.count()
        val problemDomain = categoryRepository.findByCategoryType("problem_domain").size
        val architecturePattern = categoryRepository.findByCategoryType("architecture_pattern").size
        val maturityLevel = categoryRepository.findByCategoryType("maturity_level").size

        val output = StringBuilder()
        output.appendLine()
        output.appendLine("📊 Category Statistics")
        output.appendLine("━".repeat(40))
        output.appendLine("Total:               $total")
        output.appendLine("Problem Domains:     $problemDomain")
        output.appendLine("Architecture:        $architecturePattern")
        output.appendLine("Maturity Levels:     $maturityLevel")
        output.appendLine("━".repeat(40))

        return output.toString()
    }
}
