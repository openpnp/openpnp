package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.util.Matrix;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public abstract class AbstractHeadMountable extends AbstractModelObject implements ReferenceHeadMountable {
    private AbstractAxis axisX;
    private AbstractAxis axisY;
    private AbstractAxis axisZ;
    private AbstractAxis axisRotation;

    @Attribute(required = false)
    private String axisXId;
    @Attribute(required = false)
    private String axisYId;
    @Attribute(required = false)
    private String axisZId;
    @Attribute(required = false)
    private String axisRotationId;

    public AbstractHeadMountable() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                axisX = (AbstractAxis) configuration.getMachine().getAxis(axisXId);
                axisY = (AbstractAxis) configuration.getMachine().getAxis(axisYId);
                axisZ = (AbstractAxis) configuration.getMachine().getAxis(axisZId);
                axisRotation = (AbstractAxis) configuration.getMachine().getAxis(axisRotationId);
            }
        });
    }

    @Override
    public AbstractAxis getAxisX() {
        return axisX;
    }
    public void setAxisX(AbstractAxis axisX) {
        assert axisX.getType() == Axis.Type.X;
        this.axisX = axisX;
        this.axisXId = (axisX == null) ? null : axisX.getId();
    }
    @Override
    public AbstractAxis getAxisY() {
        return axisY;
    }
    public void setAxisY(AbstractAxis axisY) {
        assert axisY.getType() == Axis.Type.Y;
        this.axisY = axisY;
        this.axisYId = (axisY == null) ? null : axisY.getId();
    }
    @Override
    public AbstractAxis getAxisZ() {
        return axisZ;
    }
    public void setAxisZ(AbstractAxis axisZ) {
        assert axisZ.getType() == Axis.Type.Z;
        this.axisZ = axisZ;
        this.axisZId = (axisZ == null) ? null : axisZ.getId();
    }
    @Override
    public AbstractAxis getAxisRotation() {
        return axisRotation;
    }
    public void setAxisRotation(AbstractAxis axisRotation) {
        assert axisRotation.getType() == Axis.Type.Rotation;
        this.axisRotation = axisRotation;
        this.axisRotationId = (axisRotation == null) ? null : axisRotation.getId();
    }

    @Override
    public  AbstractAxis getAxis(Axis.Type type) {
        switch (type) {
            case X:
                return getAxisX();
            case Y:
                return getAxisY();
            case Z:
                return getAxisZ();
            case Rotation:
                return getAxisRotation();
            default:
                return null;
        }
    }

    public  void setAxis(AbstractAxis axis) {
        switch (axis.getType()) {
            case X:
                setAxisX(axis);
                break;
            case Y:
                setAxisY(axis);
                break;
            case Z:
                setAxisZ(axis);
                break;
            case Rotation:
                setAxisRotation(axis);
                break;
        }
    }

    @Override 
    public Length getEffectiveSafeZ() {
        return getSafeZ();
    }

    @Override
    public void moveTo(Location location, double speed, MoveToOption... options) throws Exception {
        Logger.debug("{}.moveTo({}, {})", getName(), location, speed);
        location = toHeadLocation(location, getLocation());
        getHead().moveTo(this, location, getHead().getMaxPartSpeed() * speed, options);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        Logger.debug("{}.moveToSafeZ({})", getName(), speed);
        Location l = getLocation();
        Length safeZ = this.getEffectiveSafeZ().convertToUnits(l.getUnits());
        l = l.derive( null, null, safeZ.getValue(), null);
        moveTo(l, speed);
    }

    @Override
    public void moveTo(Location location, MoveToOption... options) throws Exception {
        moveTo(location, getHead().getMachine().getSpeed(), options);
    }

    @Override
    public void moveToSafeZ() throws Exception {
        moveToSafeZ(getHead().getMachine().getSpeed());
    }

    @Override
    public MappedAxes getMappedAxes() {
        return new MappedAxes(
                AbstractAxis.getControllerAxis(axisX),
                AbstractAxis.getControllerAxis(axisY),
                AbstractAxis.getControllerAxis(axisZ),
                AbstractAxis.getControllerAxis(axisRotation));
    }

    public Location toHeadLocation(Location location, Location currentLocation) {
        // Shortcut Double.NaN. Sending Double.NaN in a Location is an old API that should no
        // longer be used. It will be removed eventually:
        // https://github.com/openpnp/openpnp/issues/255
        // In the mean time, since Double.NaN would cause a problem for transformations, we shortcut
        // it here by replacing any NaN values with the current value from the axes.
        location = location.derive(currentLocation, 
                Double.isNaN(location.getX()), 
                Double.isNaN(location.getY()), 
                Double.isNaN(location.getZ()), 
                Double.isNaN(location.getRotation())); 
        // Subtract the Head offset.
        location = location.subtract(getHeadOffsets());
        return location;
    }
    final public Location toHeadLocation(Location location) {
        return toHeadLocation(location, getLocation());
    }

    public Location fromHeadLocation(Location location, Location currentLocation) {
        if (currentLocation != null) {
            // Shortcut Double.NaN. Sending Double.NaN in a Location is an old API that should no
            // longer be used. It will be removed eventually:
            // https://github.com/openpnp/openpnp/issues/255
            // In the mean time, since Double.NaN would cause a problem for transformations, we shortcut
            // it here by replacing any NaN values with the current value from the axes.
            location = location.derive(currentLocation, 
                    Double.isNaN(location.getX()), 
                    Double.isNaN(location.getY()), 
                    Double.isNaN(location.getZ()), 
                    Double.isNaN(location.getRotation()));
        }
        // Add the Head offset.
        location = location.add(getHeadOffsets());
        return location;
    }
    final public Location fromHeadLocation(Location location) {
        return fromHeadLocation(location, null);
    }

    @Override
    public Location toTransformed(Location location) {
        // The forward transformation is easy, as it can be done axis by axis.
        double x = AbstractTransformedAxis.toTransformed(axisX, location);
        double y = AbstractTransformedAxis.toTransformed(axisY, location);
        double z = AbstractTransformedAxis.toTransformed(axisZ, location);
        double rotation = AbstractTransformedAxis.toTransformed(axisRotation, location);
        return new Location (location.getUnits(), x, y, z, rotation);
    }

    @Override
    public Location toRaw(Location location) {
        // The backward transformation is a bit more complicated, as it may have transformations 
        // based on multiple input axis. Currently we only allow linear transformations and only
        // at the last stage. Given this simplification we can solve the inverse by inverting the 
        // 5D Affine Transform Matrix. 

        // Query each axis for an Affine Transform vector.
        double  [][] affineTransform = new double [][] {
            AbstractTransformedAxis.getLinearTransform(axisX, location.getUnits()),
            AbstractTransformedAxis.getLinearTransform(axisY, location.getUnits()),
            AbstractTransformedAxis.getLinearTransform(axisZ, location.getUnits()),
            AbstractTransformedAxis.getLinearTransform(axisRotation, location.getUnits()),
            { 0, 0, 0, 0, 1 }
        };

        // Compute the inverse.
        double [][] invertedAffineTransform = Matrix.inverse(affineTransform);

        // We provide the inverted Affine Tranform for those axes that actually do the linear transform.
        double x = AbstractTransformedAxis.toRaw(axisX, location, invertedAffineTransform);
        double y = AbstractTransformedAxis.toRaw(axisY, location, invertedAffineTransform);
        double z = AbstractTransformedAxis.toRaw(axisZ, location, invertedAffineTransform);
        double rotation = AbstractTransformedAxis.toRaw(axisRotation, location, invertedAffineTransform);

        return new Location (location.getUnits(), x, y, z, rotation);
    }

    @Override
    public Location getLocation() {
        MappedAxes mappedAxes = getMappedAxes();
        Location location = toTransformed(mappedAxes.getLocation(null));
        // From head to HeadMountable.
        location = fromHeadLocation(location);
        return location;
    }
}
