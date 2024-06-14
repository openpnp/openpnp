import javax.script.ScriptEngineManager

println('Scripting Environment')

// Print out the global variables accessible to the scripts.
println('  Global Variables')
println("   config: ${config}")
println("    machine: ${machine}")
println("    gui: ${gui}")
println("    scripting: ${scripting}")

println("  Scripts Directory: ${scripting.getScriptsDirectory()}")

// Show a list of scripting engines that are available along with the file
// extensions they can process.
println('  Scripting Engines')
def manager = new ScriptEngineManager()
def factories = manager.getEngineFactories()
factories.each { factory ->
    println("    Engine: ${factory.getEngineName()} (${factory.getEngineVersion()})")
    println("      Language: ${factory.getLanguageName()} (${factory.getLanguageVersion()})")
    println("      Extensions: ${factory.getExtensions()}")
}