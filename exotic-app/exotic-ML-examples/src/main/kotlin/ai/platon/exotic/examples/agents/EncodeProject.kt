package ai.platon.exotic.examples.agents

import ai.platon.pulsar.common.RequiredDirectory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime

class EncodeProject(
    /**
     * The project id
     * */
    val id: String,
    /**
     * The project type
     * Can be one of the following:
     *
     * training: training project
     * predict: prediction project
     * */
    val type: Type
) {
    enum class Type {
        TRAINING, PREDICT
    }
    
    val baseDir = when(type) {
        Type.TRAINING -> MLPaths.trainingDatasetBaseDir.resolve(id)
        Type.PREDICT -> MLPaths.predictionDatasetBaseDir.resolve(id)
    }
    
    @RequiredDirectory
    val htmlBaseDir = baseDir.resolve("html")
    
    val htmlExportInfoFile = baseDir.resolve("htmlExportInfo.txt")
    
    val datasetPath = baseDir.resolve("dataset-$id.csv")
    
    val encodeInfoFile = baseDir.resolve("encodeInfo.txt")
    
    val configFile = baseDir.resolve("config.properties")
    
    @Throws(IOException::class)
    fun createEncodeInfo(params: Map<String, Any>) {
        val map = params.toMutableMap()
        map["Build time"] = OffsetDateTime.now()
        val info = map.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        Files.writeString(encodeInfoFile, info)
    }
    
    fun createConfigFile(properties: Map<String, Any>) {
        val info = properties.entries.joinToString("\n") { "${it.key}=${it.value}" }
        Files.writeString(configFile, info)
    }
    
    fun clearEncodeInfo() {
        Files.deleteIfExists(encodeInfoFile)
    }
    
    fun createDirectories() {
        EncodeProject::class.java.declaredFields
            .filter { it.annotations.any { it is RequiredDirectory } }
            .mapNotNull { it.get(this) as? Path }
            .forEach { it.takeUnless { Files.exists(it) }?.let { Files.createDirectories(it) } }
    }
}
