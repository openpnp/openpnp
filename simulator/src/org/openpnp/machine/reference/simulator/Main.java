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
        machine = new Machine(assetManager);
        
        flyCam.setDragToRotate(true);

        rootNode.attachChild(machine.getNode());

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(2f));
        rootNode.addLight(sun);

//        cam.setLocation(new Vector3f(0.9789996f, 0.6054432f, 0.9655635f));
//        cam.setRotation(new Quaternion(-0.08566449f, 0.9023052f, -0.21124944f, -0.36589697f));
        
//        cam.setLocation(new Vector3f(0.07781982f, 0.27936003f, 2.0324063f));
//        cam.setRotation(new Quaternion(-0.0011174536f, 0.9975934f, -0.06732103f, -0.01655898f));
        
        cam.setLocation(new Vector3f(-1.3480331f, 1.098075f, 1.3006098f));
        cam.setRotation(new Quaternion(0.107952856f, 0.8841834f, -0.25264242f, 0.37780634f));
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
        settings.setResolution(1920, 1080);
        Main app = new Main();
        app.setShowSettings(false);
        app.setSettings(settings);
        app.start();
    }
}
