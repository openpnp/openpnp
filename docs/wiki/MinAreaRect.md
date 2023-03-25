# MinAreaRect Pipeline Stage

The MinAreaRect stage finds the smallest rotated rectangle that encloses a subject. It considers pixels that fall within a given brightness range. Input should be a grayscale image. 

The stage can also detect only selected edges of a rectangular outline.

![MinAreaRect partial edges](https://user-images.githubusercontent.com/9963310/177054694-9e1e339c-3dd0-4335-ae8e-26ae7e798cd9.png)

- **thresholdMin**: the minimum brightness to include the pixel. 
- **thresholdMax**: the maximum brightness to include the pixel. 
- **expectedAngle**: the angle at which the rectangular outline is expected. This will usually be controlled by the pipeline caller. In bottom vision, it will supply the (pre-rotate) angle of the part to be detected. For use cases other than bottom vision, and conventional all-edges detection, the angle remains irrelevant. 
- **leftEdge**, **rightEdge**, **topEdge**, **bottomEdge**: the four edges of the rectangle to be detected. If a switch is **off**, the edge is not considered in fitting the rectangle, i.e. the subject can be non-rectangular on this side. This is typically the case when the camera can only get a partial view of a large subject. The sense of "left", "right", "top" and "bottom" is at neutral rotation, i.e. when the **expectedAngle** is at zero degrees, e.g., when the part to be detected in bottom vision is upright. This sense will be rotated correctly for other **expectedAngle**s. 
- **diagnostics**: this can be used to display the edge pixels that are considered for the convex hull of the subject.  
   ![hull pixels](https://user-images.githubusercontent.com/9963310/177055104-4aca4b62-8dbb-403f-8bd0-fc29448acd8b.png)
- **propertyName**: determines the pipeline property name under which this stage is controlled by the vision operation. If set, these will override some of the properties configured here. Use "MinAreaRect" for default control.