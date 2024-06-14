def language = "groovy"

testResults[language] = "ok"
if (threadedTest) {
    // explicit cast to String otherwise the testResult map key will contain a GString object.
    String key = "${language}${threadId}"
    testResults[key] = "ok"
}