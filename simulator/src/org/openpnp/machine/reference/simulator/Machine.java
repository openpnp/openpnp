/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openpnp.machine.reference.simulator;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

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
 *                      pin
 *                  z1
 *                      rail
 *                      stepper
 *                  z2
 *                      rail
 *                      stepper
 * </pre>
 */
public class Machine {
    
    public enum Movable {
        Camera,
        Nozzle1,
        Nozzle2,
        Actuator
    };

    private final AssetManager assetManager;
    private final Node machine;
    private final Node gantry;
    private final Node head;
    private final Spatial n1;
    private final Spatial n2;
    private final Spatial camera;
    private final Spatial actuatorPin;
    private final Material polishedStainlessTexture;
    private final Material brushedAluminumTexture;
    private final Material roughAluminumTexture;
    private final Material rawAluminumTexture;
    private final Material blackAluminumTexture;
    
    private Vector3f n1Offsets, n2Offsets, actuatorPinOffsets, cameraOffsets = Vector3f.ZERO;
    
    private Vector3f gantryZero, headZero, n1Zero, n2Zero, actuatorPinZero;
    private Vector3f gantryTarget, headTarget;

    private long lastFrameTime;
    
    public Machine(AssetManager assetManager) {
        this.assetManager = assetManager;
        
        // Load textures
        polishedStainlessTexture = basicTexture("Textures/radial-stainless-steel.jpg");
        brushedAluminumTexture = basicTexture("Textures/brushed_aluminum.jpg");
        rawAluminumTexture = basicTexture("Textures/raw-aluminum.jpg");
        roughAluminumTexture = basicTexture("Textures/rough-metal.jpg");
        blackAluminumTexture = basicTexture("Textures/black-aluminum.jpg");
        
        // Create spatials
        machine = createMachineNode();
        machine.scale(0.001f);
        gantry = (Node) machine.getChild("gantry");
        head = (Node) gantry.getChild("head");
        Node z1Node = (Node) head.getChild("z1");
        Node z2Node = (Node) head.getChild("z2");
        n1 = z1Node.getChild("stepper");
        n2 = z2Node.getChild("stepper");
        camera = head.getChild("camera");
        Node actuator = (Node) head.getChild("actuator");
        actuatorPin = actuator.getChild("pin");
        
        // Calculate offsets of machine elements
        // Camera is basis fors all offsets
        // z1, z2, actuatorPin
        n1Offsets = getOffsets(camera, n1);
        n2Offsets = getOffsets(camera, n2);
        actuatorPinOffsets = getOffsets(camera, actuatorPin);
        
        System.out.println("cameraOffsets " + cameraOffsets);
        System.out.println("n1Offsets " + n1Offsets);
        System.out.println("n2Offsets " + n2Offsets);
        System.out.println("actuatorOffsets " + actuatorPinOffsets);
        
        // Position everything at zero
        gantry.move(0, 0, 240);
        head.move(-250, 0, 0);
        n1.move(0, 25, 0);
        n2.move(0, 25, 0);
        actuatorPin.move(0, -2, 0);
        
        // Grab the zero location so that we have a reference from here on.
        // We clone because these will change as we move things.
        gantryZero = gantry.getLocalTranslation().clone();
        headZero = head.getLocalTranslation().clone();
        n1Zero = n1.getLocalTranslation().clone();
        n2Zero = n2.getLocalTranslation().clone();
        actuatorPinZero = actuatorPin.getLocalTranslation().clone();
    }
    
    public void moveTo(Movable movable, double x, double y, double z, double c) throws Exception {
        /**
         * Movements of any of the four Movable are made up of a movement in
         * machine Y of gantry, machine X of head, machine Z of n1 or n2 and
         * machine C of the nozzle tip, not yet defined.
         */
        
        System.out.println("moveTo(" + x + ", " + y + ", " + z + ", " + c + ")");
        
        if (gantryTarget != null || headTarget != null) {
            throw new Exception("Movement not complete!");
        }
        gantryTarget = gantryZero.add(0f, 0f, (float) -y);
        headTarget = headZero.add((float) x, 0, 0);
    }
    
    public void pick(Movable movable) throws Exception {
        
    }
    
    public void place(Movable movable) throws Exception {
        
    }
    
    public void actuate(boolean on) throws Exception {
        
    }
    
    public void update(float tpf) {
        if (lastFrameTime == 0) {
            lastFrameTime = System.currentTimeMillis();
        }
        long t = System.currentTimeMillis() - lastFrameTime;
        
        if (headTarget != null) {
            // Determine the distance we can move based on the amount of
            // time elapsed since the last frame.
            float xDist = 250f / 1000 * t;
            // The distance left to move before reaching the target.
            float xDelta = headTarget.subtract(head.getLocalTranslation()).x;
            // If we've moving in the negative direction, invert the distance
            // we'll move.
            if (xDelta < 0) {
                xDist = -xDist;
            }
            // Make the movement.
            head.move(xDist, 0, 0);
            // If the distance moved is greater than or equal to the distance
            // to move we're done with this move.
            if (Math.abs(xDist) >= Math.abs(xDelta)) {
                head.setLocalTranslation(headTarget);
                headTarget = null;
            }
        }
        
        if (gantryTarget != null) {
            float yDist = 250f / 1000 * t;
            float yDelta = gantryTarget.subtract(gantry.getLocalTranslation()).z;
            if (yDelta < 0) {
                yDist = -yDist;
            }
            gantry.move(0, 0, yDist);
            if (Math.abs(yDist) >= Math.abs(yDelta)) {
                gantry.setLocalTranslation(gantryTarget);
                gantryTarget = null;
            }
        }
        
        lastFrameTime = System.currentTimeMillis();
    }

    private Vector3f getOffsets(Spatial from, Spatial to) {
        Vector3f offsets = from.getWorldTranslation();
        offsets = offsets.subtract(to.getWorldTranslation());
        return offsets;
    }
    
    public Node getNode() {
        return machine;
    }

    private Node createMachineNode() {
        Node machine = new Node("machine");

        Geometry table = new Geometry("table", new Box(600 / 2, 12.7f / 2, 600 / 2));
        table.setMaterial(roughAluminumTexture);
        machine.attachChild(table);

        Geometry yRailLeft = new Geometry("y_rail_left", new Box(6, 6, 600 / 2));
        yRailLeft.setMaterial(polishedStainlessTexture);
        yRailLeft.move(-300 + 6, 12.7f, 0);
        machine.attachChild(yRailLeft);

        Geometry yRailRight = new Geometry("y_rail_right", new Box(6, 6, 600 / 2));
        yRailRight.setMaterial(polishedStainlessTexture);
        yRailRight.move(300 - 6, 12.7f, 0);
        machine.attachChild(yRailRight);

        Node gantry = createGantryNode();
        gantry.move(0, 50 / 2 + 12.7f + 6, 0);
        machine.attachChild(gantry);;

        return machine;
    }

    private Node createGantryNode() {
        Node gantry = new Node("gantry");

        Geometry gantryTube = new Geometry("gantry_tube", new Box(600 / 2, 50 / 2, 50 / 2));
        gantryTube.setMaterial(brushedAluminumTexture);
        gantry.attachChild(gantryTube);

        Geometry xRail = new Geometry("x_rail", new Box(600 / 2, 12 / 2, 12 / 2));
        xRail.setMaterial(polishedStainlessTexture);
        xRail.move(0, 0, 25 + 6);
        gantry.attachChild(xRail);

        Node head = createHeadNode();
        head.move(0, 10, 25 + 12 + 3);
        gantry.attachChild(head);

        return gantry;
    }

    private Node createHeadNode() {
        Node head = new Node("head");

        Geometry basePlate = new Geometry("base_plate", new Box(50 / 2, 50 / 2, 6 / 2));
        basePlate.setMaterial(rawAluminumTexture);
        head.attachChild(basePlate);

        Geometry camera = new Geometry("camera", new Cylinder(12, 12, 8, 15, true));
        camera.setMaterial(blackAluminumTexture);
        camera.rotate((float) Math.PI / 2, 0f, 0f);
        camera.move(-6, -50 / 2 + 15 / 2, 8 + 3);
        head.attachChild(camera);

        Node actuator = createActuatorAssmNode("actuator");
        actuator.move(8, -50 / 2 + 25 / 2, 4 + 3);
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
        actuatorBody.setMaterial(brushedAluminumTexture);
        actuatorBody.rotate((float) Math.PI / 2, 0f, 0f);
        actuator.attachChild(actuatorBody);

        Geometry actuatorPin = new Geometry("pin", new Cylinder(12, 12, 0.5f, 25, true));
        actuatorPin.rotate((float) Math.PI / 2, 0f, 0f);
        actuatorPin.setMaterial(polishedStainlessTexture);
        actuator.attachChild(actuatorPin);

        return actuator;
    }

    private Node createZAssmNode(String name) {
        Node zAssm = new Node(name);

        Geometry rail = new Geometry("rail", new Box(4 / 2, 50 / 2, 4 / 2));
        rail.setMaterial(polishedStainlessTexture);
        zAssm.attachChild(rail);

        Geometry stepper = new Geometry("stepper", new Box(15 / 2, 25 / 2, 15 / 2));
        stepper.setMaterial(blackAluminumTexture);
        stepper.move(0, 0, 2 + 15 / 2);
        zAssm.attachChild(stepper);

        return zAssm;
    }

    private Material basicMaterial(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", color);
        mat.setColor("Diffuse", color);
        return mat;
    }

    private Material basicTexture(String path) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture(path));
        return mat;
    }

    private ColorRGBA createColor(int r, int g, int b) {
        return new ColorRGBA(1.0f / 255f * r, 1.0f / 255f * g, 1.0f / 255f * b, 1.0f);
    }
    
    class Movable_ {
        private final Spatial spatial;
        private final Vector3f zero;
        
        public Movable_(Spatial spatial, Vector3f zero) {
            this.spatial = spatial;
            this.zero = zero;
        }
        
        public Spatial getSpatial() {
            return spatial;
        }
        
        public Vector3f getZero() {
            return zero;
        }
    }
}
