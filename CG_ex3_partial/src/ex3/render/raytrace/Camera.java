package ex3.render.raytrace;

import java.util.Map;
import math.Point3D;
import math.Ray;
import math.Vec;

/**
 * Represents the scene's camera.
 * 
 */
public class Camera implements IInitable {

	protected Point3D eye = null;
	protected Vec direction = null;
	protected Point3D lookAt = null;
	protected Vec upDirection = null;
	protected double screenDist;
	protected double screenWidth;

	public Camera() {
		screenDist = 0;
		screenWidth = 2;
	}

	public void init(Map<String, String> attributes) {
		if (attributes.containsKey("eye")) {
			eye = new Point3D(attributes.get("eye"));
		}
		if (attributes.containsKey("direction")) {
			direction = new Vec(attributes.get("direction"));
		}
		if (attributes.containsKey("look-at")) {
			lookAt = new Point3D(attributes.get("look-at"));
		}
		if (attributes.containsKey("up-direction")) {
			upDirection = new Vec(attributes.get("up-direction"));
		}
		if (attributes.containsKey("screen-dist")) {
			Double.parseDouble(attributes.get("screen-dist"));
		}
		if (attributes.containsKey("screen-width")) {
			Double.parseDouble(attributes.get("screen-width"));
		}
	}

	/**
	 * Transforms image xy coordinates to view pane xyz coordinates. Returns the
	 * ray that goes through it.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */

	public Ray constructRayThroughPixel(double x, double y, double height,
			double width) {
		if (direction == null && lookAt == null) {
			System.err.println("insuficient data for camera");
		} else if (direction == null) {
			direction = new Vec(eye, lookAt);
		}
		return new Ray(eye, direction);
	}
}
