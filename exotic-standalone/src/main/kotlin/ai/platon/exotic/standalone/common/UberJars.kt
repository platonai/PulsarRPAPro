package ai.platon.exotic.standalone.common

import ai.platon.exotic.standalone.agents.AgentStarter
import ai.platon.exotic.standalone.agents.LauncherOptions
import ai.platon.pulsar.common.getLogger
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Paths

object UberJars {
    private val logger = getLogger(this)
    
    val JAVA = Paths.get(SystemUtils.JAVA_HOME + "/bin/java")
    val JAR = this.javaClass.protectionDomain.codeSource.location.toURI().path
    
    val agents = mutableListOf<AgentStarter>()
    
    val deadAgents = mutableListOf<AgentStarter>()
    
    fun runUberJar(args: List<String>): AgentStarter {
        val options = LauncherOptions("harvest", JAVA, listOf("-jar", JAR) + args)
        return launch(options)
    }
    
    fun launch(options: LauncherOptions): AgentStarter {
        val agent = AgentStarter(options).also { it.start() }
        agents.add(agent)
        return agent
    }
    
    fun monitor() {
        agents.filter { it.isExpired }.forEach { it.stop() }
        
        agents.iterator().forEachRemaining { agent ->
            if (!agent.isAlive) {
                logger.warn("Agent [${agent.name}] is dead, create a new one")
                agents.remove(agent)
                deadAgents.add(agent)
                
                launch(agent.options)
            }
        }
    }
}
