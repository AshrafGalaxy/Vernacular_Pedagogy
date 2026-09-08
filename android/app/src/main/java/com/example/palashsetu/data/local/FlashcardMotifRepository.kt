package com.example.palashsetu.data.local

import android.content.Context
import android.util.Log
import com.example.palashsetu.data.model.MotifCategory
import com.example.palashsetu.data.model.MotifItem
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Offline in-memory repository for the 150 Cultural Realia Visual Motifs.
 * Loads and indexes schemas/motif_catalog.json from assets with zero latency.
 */
object FlashcardMotifRepository {

    private const val TAG = "FlashcardMotifRepo"
    private const val CATALOG_ASSET_PATH = "schemas/motif_catalog.json"

    private var cachedMotifs: List<MotifItem>? = null
    private val motifMap = mutableMapOf<String, MotifItem>()

    @Synchronized
    fun getMotifs(context: Context): List<MotifItem> {
        cachedMotifs?.let { return it }

        val list = mutableListOf<MotifItem>()
        try {
            context.assets.open(CATALOG_ASSET_PATH).use { stream ->
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val jsonString = reader.readText()
                val array = JSONArray(jsonString)

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val tagsArray = obj.optJSONArray("tags")
                    val tags = mutableListOf<String>()
                    if (tagsArray != null) {
                        for (t in 0 until tagsArray.length()) {
                            tags.add(tagsArray.getString(t))
                        }
                    }

                    val promptsArray = obj.optJSONArray("context_prompts")
                    val prompts = mutableListOf<String>()
                    if (promptsArray != null) {
                        for (p in 0 until promptsArray.length()) {
                            prompts.add(promptsArray.getString(p))
                        }
                    }

                    val item = MotifItem(
                        id = id,
                        filename = obj.getString("filename"),
                        assetPath = obj.getString("asset_path"),
                        nameHi = obj.getString("name_hi"),
                        nameOlchiki = obj.getString("name_olchiki"),
                        phoneticDeva = obj.getString("phonetic_deva"),
                        category = MotifCategory.fromCode(obj.getString("category")),
                        grade = obj.optInt("grade", 1),
                        aspectRatio = obj.optDouble("aspect_ratio", 1.83).toFloat(),
                        compressedSizeKb = obj.optDouble("compressed_size_kb", 25.0).toFloat(),
                        tags = tags,
                        contextPrompts = prompts
                    )
                    list.add(item)
                    motifMap[id] = item
                }
            }
            Log.i(TAG, "Successfully loaded ${list.size} cultural motifs into in-memory catalog")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load motif catalog from assets: ${e.message}", e)
        }

        cachedMotifs = list
        return list
    }

    fun getMotifById(context: Context, id: String): MotifItem? {
        if (cachedMotifs == null) {
            getMotifs(context)
        }
        return motifMap[id]
    }

    fun filterMotifs(
        context: Context,
        query: String = "",
        category: MotifCategory = MotifCategory.ALL,
        grade: Int? = null
    ): List<MotifItem> {
        val all = getMotifs(context)
        return all.filter { item ->
            val matchesCategory = (category == MotifCategory.ALL || item.category == category)
            val matchesGrade = (grade == null || item.grade == grade)
            val matchesSearch = if (query.isBlank()) true else item.matchesQuery(query)
            matchesCategory && matchesGrade && matchesSearch
        }
    }

    fun getRandomMotifs(
        context: Context,
        count: Int,
        category: MotifCategory = MotifCategory.ALL,
        grade: Int? = null
    ): List<MotifItem> {
        val filtered = filterMotifs(context, "", category, grade)
        return filtered.shuffled().take(count)
    }
}
