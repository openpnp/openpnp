package org.openpnp.machine.reference.simulator;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;

/**
 * test
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    private Machine machine;
    
    @Override
    public void simpleInitApp() {
        setPauseOnLostFocus(false);
        
        machine = new Machine(assetManager);
        try {
            Server server = new Server(machine);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        flyCam.setDragToRotate(true);

        rootNode.attachChild(machine.getNode());

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(2f));
        rootNode.addLight(sun);

        cam.setLocation(new Vector3f(-1.3480331f, 1.098075f, 1.3006098f));
        cam.setRotation(new Quaternion(0.107952856f, 0.8841834f, -0.25264242f, 0.37780634f));
        
//        cam.setLocation(new Vector3f(-0.100799635f, 0.10030662f, 1.7509058f));
//        cam.setRotation(new Quaternion(-0.0012744298f, 0.998338f, -0.024377229f, -0.052204806f));        
        
//        Thread t = new Thread() {
//            public void run() {
//                while (true) {
//                    try {
//                        machine.moveTo(
//                                Math.random() > 0.5 ? Movable.Nozzle1 : Movable.Nozzle2, 
//                                Math.random() * 500, 
//                                Math.random() * 500, 
//                                Math.random() * -50, 
//                                0);
//                        machine.actuate(true);
//                        machine.home();
//                        machine.actuate(false);
//                    }
//                    catch (Exception e) {
//
//                    }
//                }
//            }
//        };
//        t.setDaemon(true);
//        t.start();
    }

    @Override
    public void simpleUpdate(float tpf) {
        machine.update(tpf);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1024, 768);
        Main app = new Main();
        app.setShowSettings(false);
        app.setSettings(settings);
        app.start();
    }
}
