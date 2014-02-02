/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openpnp.machine.reference.simulator;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
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

    private final AssetManager assetManager;
    private final Node machineNode;
    private final Node gantryNode;
    private final Node headNode;
    private final Spatial z1Stepper;
    private final Spatial z2Stepper;
    private final Spatial camera;
    private final Spatial actuatorPin;
    private final Material polishedStainlessTexture;
    private final Material brushedAluminumTexture;
    private final Material roughAluminumTexture;
    private final Material rawAluminumTexture;
    private final Material blackAluminumTexture;

    public Machine(AssetManager assetManager) {
        this.assetManager = assetManager;
        polishedStainlessTexture = basicTexture("Textures/radial-stainless-steel.jpg");
        brushedAluminumTexture = basicTexture("Textures/brushed_aluminum.jpg");
        rawAluminumTexture = basicTexture("Textures/raw-aluminum.jpg");
        roughAluminumTexture = basicTexture("Textures/rough-metal.jpg");
        blackAluminumTexture = basicTexture("Textures/black-aluminum.jpg");
        machineNode = createMachineNode();
        gantryNode = (Node) machineNode.getChild("gantry");
        headNode = (Node) gantryNode.getChild("head");
        Node z1Node = (Node) headNode.getChild("z1");
        Node z2Node = (Node) headNode.getChild("z2");
        z1Stepper = z1Node.getChild("stepper");
        z2Stepper = z2Node.getChild("stepper");
        camera = headNode.getChild("camera");
        Node actuator = (Node) headNode.getChild("actuator");
        actuatorPin = actuator.getChild("pin");
    }

    public Node getNode() {
        return machineNode;
    }

    public void update(float tpf) {
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

        machine.scale(0.001f);

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
        head.move(0, 0, 25 + 12 + 3);
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
        actuator.rotate((float) Math.PI / 2, 0f, 0f);
        actuator.move(8, -50 / 2 + 25 / 2, 8 + 3);
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
        actuator.attachChild(actuatorBody);

        Geometry actuatorPin = new Geometry("pin", new Cylinder(12, 12, 0.5f, 10, true));
        actuatorPin.move(0, 0, 25 / 2);
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
}
