/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

public class WireProcessor implements SceneProcessor {    
 
    RenderManager renderManager;
    Material wireMaterial;
 
    public WireProcessor(AssetManager assetManager) {
        wireMaterial = new Material(assetManager, "/Common/MatDefs/Misc/Unshaded.j3md");
        wireMaterial.setColor("Color", ColorRGBA.Blue);
        wireMaterial.getAdditionalRenderState().setWireframe(true);
    }
 
    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
    }
 
    public void reshape(ViewPort vp, int w, int h) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
 
    public boolean isInitialized() {
        return renderManager != null;
    }
 
    public void preFrame(float tpf) {        
    }
 
    public void postQueue(RenderQueue rq) {
        renderManager.setForcedMaterial(wireMaterial);
    }
 
    public void postFrame(FrameBuffer out) {
        renderManager.setForcedMaterial(null);
    }
 
    public void cleanup() {
        renderManager.setForcedMaterial(null);
    }
 
}