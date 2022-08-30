one = new Thread() {
    public void run() {
        scripting.on("testEvent", testGlobals);
    }  
};

one.start();
one.join();
