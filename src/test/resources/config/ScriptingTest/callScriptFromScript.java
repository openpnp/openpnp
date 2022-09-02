testResults.put("base" + threadId, "ok");

thread = new Thread() {
    public void run() {
        scripting.on("testEvent", testGlobals);
    }  
};
thread.start();
thread.join();
