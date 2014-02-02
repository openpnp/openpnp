package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;

/**
 * test
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1920, 1080);
        Main app = new Main();
        app.setShowSettings(false);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setDragToRotate(true);
//        getViewPort().addProcessor(new WireProcessor(assetManager));
//        attachCoordinateAxes(Vector3f.ZERO);

        Node machine = createMachineNode();

        rootNode.attachChild(machine);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(2f));
        rootNode.addLight(sun);
    
        cam.setLocation(new Vector3f(0.9789996f, 0.6054432f, 0.9655635f));
        cam.setRotation(new Quaternion(-0.08566449f, 0.9023052f, -0.21124944f, -0.36589697f));
    }    


    /**
     * <pre>
     *      machine
     *          table
     *          y_rail_left
     *          y_rail_right
     *          gantry
     *              gantry_tube
     *              x_rail
     *              head
     *                  base_plate
     *                  camera
     *                  actuator
     *                      body
     *                  z1
     *                      rail
     *                      stepper
     *                  z2
     *                      rail
     *                      stepper
     * </pre>
     */
    private Node createMachineNode() {
        Node machine = new Node("machine");

        Geometry table = new Geometry("table", new Box(600 / 2, 12.7f / 2, 600 / 2));
        table.setMaterial(basicMaterial(ColorRGBA.Green));
        machine.attachChild(table);

        Geometry yRailLeft = new Geometry("y_rail_left", new Box(6, 6, 600 / 2));
        yRailLeft.setMaterial(basicMaterial(ColorRGBA.Red));
        yRailLeft.move(-300 + 6, 12.7f, 0);
        machine.attachChild(yRailLeft);

        Geometry yRailRight = new Geometry("y_rail_right", new Box(6, 6, 600 / 2));
        yRailRight.setMaterial(basicMaterial(ColorRGBA.Red));
        yRailRight.move(300 - 6, 12.7f, 0);
        machine.attachChild(yRailRight);

        Node gantry = createGantryNode();
        gantry.move(0, 50 / 2 + 12.7f + 6, 0);
        machine.attachChild(gantry);;

        machine.scale(0.001f);

        return machine;
    }

    private Node createGantryNode() {
        Node gantry = new Node("gantry");

        Geometry gantryTube = new Geometry("gantry_tube", new Box(600 / 2, 50 / 2, 50 / 2));
        gantryTube.setMaterial(basicMaterial(createColor(61, 110, 198)));
        gantry.attachChild(gantryTube);

        Geometry xRail = new Geometry("x_rail", new Box(600 / 2, 12 / 2, 12 / 2));
        xRail.setMaterial(basicMaterial(ColorRGBA.Red));
        xRail.move(0, 0, 25 + 6);
        gantry.attachChild(xRail);

        Node head = createHeadNode();
        head.move(0, 0, 25 + 12 + 3);
        gantry.attachChild(head);

        return gantry;
    }

    private Node createHeadNode() {
        Node head = new Node("head");

        Geometry basePlate = new Geometry("base_plate", new Box(50 / 2, 50 / 2, 6 / 2));
        basePlate.setMaterial(basicMaterial(ColorRGBA.Magenta));
        head.attachChild(basePlate);

        Geometry camera = new Geometry("camera", new Cylinder(12, 12, 8, 15, true));
        camera.setMaterial(basicMaterial(ColorRGBA.Orange));
        camera.rotate((float) Math.PI / 2, 0f, 0f);
        camera.move(-6, 0, 8 + 3);
        head.attachChild(camera);

        Node actuator = createActuatorAssmNode("actuator");
        actuator.rotate((float) Math.PI / 2, 0f, 0f);
        actuator.move(8, 0, 8 + 3);
        head.attachChild(actuator);

        Node z1 = createZAssmNode("z1");
        z1.move(-25 + 2, 0, 3 + 2);
        head.attachChild(z1);

        Node z2 = createZAssmNode("z2");
        z2.move(25 - 2, 0, 3 + 2);
        head.attachChild(z2);


        return head;
    }

    private Node createActuatorAssmNode(String name) {
        Node actuator = new Node(name);

        Geometry actuatorBody = new Geometry("body", new Cylinder(12, 12, 4, 25, true));
        actuatorBody.setMaterial(basicMaterial(ColorRGBA.Yellow));
        actuator.attachChild(actuatorBody);

        return actuator;
    }

    private Node createZAssmNode(String name) {
        Node zAssm = new Node(name);

        Geometry rail = new Geometry("rail", new Box(4 / 2, 50 / 2, 4 / 2));
        rail.setMaterial(basicMaterial(ColorRGBA.Red));
        zAssm.attachChild(rail);

        Geometry stepper = new Geometry("stepper", new Box(15 / 2, 25 / 2, 15 / 2));
        stepper.setMaterial(basicMaterial(ColorRGBA.Cyan));
        stepper.move(0, 0, 2 + 15 / 2);
        zAssm.attachChild(stepper);

        return zAssm;
    }

    private Material basicMaterial(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", color);
        mat.setColor("Diffuse", color);
        
        
//        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.setColor("Color", color);
        
        
        return mat;
    }

    private ColorRGBA createColor(int r, int g, int b) {
        return new ColorRGBA(1.0f / 255f * r, 1.0f / 255f * g, 1.0f / 255f * b, 1.0f);
    }

    private void attachCoordinateAxes(Vector3f pos) {
        Arrow arrow = new Arrow(Vector3f.UNIT_X);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Red).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Y);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Green).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Z);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Blue).setLocalTranslation(pos);
    }

    private Geometry putShape(Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        rootNode.attachChild(g);
        return g;
    }
    
    float yDir = 1, xDir = 1, z1Dir = 1, z2Dir = -1;
    long t = System.currentTimeMillis();

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
        Node gantry = (Node) rootNode.getChild("gantry");
        Node head = (Node) gantry.getChild("head");
        Node z1 = (Node) head.getChild("z1");
        Spatial z1Stepper = z1.getChild("stepper");
        Node z2 = (Node) head.getChild("z2");
        Spatial z2Stepper = z2.getChild("stepper");
        
        // move at 250mm per second
        long d = System.currentTimeMillis() - t;
        float dist = 250f / 1000f * d;

        gantry.move(0, 0, dist * yDir);
        if (gantry.getLocalTranslation().getZ() > 300) {
            yDir = -1;
        } else if (gantry.getLocalTranslation().getZ() < -300) {
            yDir = 1;
        }

        head.move(dist * xDir, 0, 0);
        if (head.getLocalTranslation().getX() > 300) {
            xDir = -1;
        } else if (head.getLocalTranslation().getX() < -300) {
            xDir = 1;
        }
        
        dist = 50f / 1000f * d;
        
        z1Stepper.move(0, dist * z1Dir, 0);
        if (z1Stepper.getLocalTranslation().getY() > 25) {
            z1Dir = -1;
        } else if (z1Stepper.getLocalTranslation().getY() < -25) {
            z1Dir = 1;
        }
        
        z2Stepper.move(0, dist * z2Dir, 0);
        if (z2Stepper.getLocalTranslation().getY() > 25) {
            z2Dir = -1;
        } else if (z2Stepper.getLocalTranslation().getY() < -25) {
            z2Dir = 1;
        }
        
        t = System.currentTimeMillis();
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
