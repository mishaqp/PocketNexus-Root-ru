package com.niki914.nexus.agentic.app.ui.nexus.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelCatalogLoaderTest {
    @Test
    fun parseModelIds_supportsDataAndGoogleModelsFormats() {
        val models = ModelCatalogLoader.parseModelIds(
            """
            {
              "data": [
                {"id": "gpt-4.1"},
                {"id": " gpt-5.4 "},
                {"id": "gpt-4.1"},
                {"id": ""}
              ],
              "models": [{"name": "models/ignored"}]
            }
            """.trimIndent(),
        )
        val googleModels = ModelCatalogLoader.parseModelIds(
            """{"models":[{"name":"models/gemini-2.5-pro"}]}""",
        )

        assertEquals(listOf("gpt-4.1", "gpt-5.4"), models)
        assertEquals(listOf("gemini-2.5-pro"), googleModels)
    }

    @Test
    fun modelCatalogUrl_replacesInferencePathAndRemovesQuery() {
        assertEquals(
            "https://api.openai.com/v1/models",
            ModelCatalogLoader.modelCatalogUrl(
                "https://api.openai.com/v1/chat/completions?unused=true",
                "openai",
            ),
        )
        assertEquals(
            "https://api.anthropic.com/v1/models",
            ModelCatalogLoader.modelCatalogUrl(
                "https://api.anthropic.com/v1/messages",
                "anthropic",
            ),
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models",
            ModelCatalogLoader.modelCatalogUrl(
                "https://generativelanguage.googleapis.com/custom/path?key=old",
                "google",
            ),
        )
    }
}
