package ex3.render.raytrace;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import math.Point3D;
import math.Ray;
import math.Vec;

/**
 * A Scene class containing all the scene objects including camera, lights and
 * surfaces. Some suggestions for code are in comment If you uncomment these
 * lines you'll need to implement some new types like Surface
 * 
 * You can change all methods here this is only a suggestion! This is your
 * world, add members methods as you wish
 */
public class Scene implements IInitable {

	protected List<Surface> surfaces = null;
	protected List<Light> lights = null;
	protected Camera camera = null;
	protected Vec backCol = null;
	protected String backTex = null;
	protected int recLevel;
	protected Vec ambient = null;
	protected double superSamp;
	protected int acceleration;

	public Scene() {
		surfaces = new LinkedList<Surface>();
		lights = new LinkedList<Light>();
		camera = new Camera();
		backCol = new Vec(0, 0, 0);
		recLevel = 10;
		ambient = new Vec(0, 0, 0);
		superSamp = 1;
		acceleration = 0;
	}

	/**
	 * Reset the scene parameters from XML
	 */
	public void init(Map<String, String> attributes) {
		if (attributes.containsKey("background-col")) {
			backCol = new Vec(attributes.get("background-col"));
		}
		if (attributes.containsKey("background-tex")) {
			backTex = attributes.get("background-tex");
		}
		if (attributes.containsKey("max-recursion-level")) {
			recLevel = Integer.parseInt(attributes.get("max-recursion-level"));
		}
		if (attributes.containsKey("ambient-light")) {
			ambient = new Vec(attributes.get("ambient-light"));
		}
		if (attributes.containsKey("super-samp-width")) {
			superSamp = Double.parseDouble(attributes.get("super-samp-width"));
		}
		if (attributes.containsKey("use-acceleration")) {
			acceleration = Integer.parseInt(attributes.get("use-acceleration"));
		}
	}

	/**
	 * Send ray return the nearest intersection. Return null if no intersection
	 * 
	 * @param ray
	 * @return
	 */
	public MinIntersection findIntersection(Ray ray) {
		double min = Double.MAX_VALUE;
		Surface min_surface = null;

		// For each surface check for nearest intersection.
		for (Surface surface : surfaces) {
			double curDist = surface.Intersect(ray);
			if ((curDist < min) && (curDist > Ray.eps)) {
				min_surface = surface;
				min = curDist;
			}
		}
		// In case of no intersection
		if (min == Double.MAX_VALUE) {
			return null;
		}
		Point3D intersection = new Point3D(ray.p, Vec.scale(min, ray.v));

		// In case intersection was found
		return new MinIntersection(intersection, min_surface, min);
	}

	public Vec calcColor(Ray ray, int curLevel, MinIntersection intersection) {
		if(recLevel == curLevel) {
			return new Vec(0,0,0);
		}
		// If no intersection, chose background color
		if(intersection == null) {
			return this.backCol;
		}
		Point3D intersectionPoint = intersection.intersectionPoint;
		Surface surface = intersection.minSurface;
		
		//Material Emission
		Vec Ie = new Vec(surface.emission);
		//Global*Material ambient
		Vec kaIa = Vec.scale(surface.ambient, this.ambient);
		// Material diffuse
		Vec Kd = surface.diffuse;
		// Intersection Normal
		Vec N = surface.normal(intersection.intersectionPoint);
		// Material specular
		Vec Ks = surface.specular;
		// Intersection to eye
		Vec V = ray.v;
		V.normalize();
		// Material reflection
		double Kr = surface.reflectance;
		// Material shininess
		double n = surface.shininess;
		// Intersection to light.
		Vec Li = new Vec();
		// Intersection to reflected light.
		Vec Ri = new Vec();
		// Light shadow.
		double Si;
		// Light source intensity.
		Vec Ii = new Vec();

		// I = Emission + Ambient
		Vec I = new Vec();
		Vec.add(Ie, kaIa);
		Vec SigmaColor;
		Light light;

		
		// Diffuse & Specular calculations
		for (int i = 0; i < lights.size(); i++) {
			light = lights.get(i);
			Si = 1;
			Li = light.getDir(intersectionPoint);
			
			// Check if there is a surface blocking the ray from the light
			// source. Used to set if a shadow is needed.
			Ray rayToLightSrc = new Ray(intersectionPoint, Li);
			MinIntersection rayNearestIntersection = findIntersection(rayToLightSrc);
			// Set the shadow if there is no surface on the way from the light
			// source.
			if (rayNearestIntersection != null) {
				Si = light.getShadow(intersectionPoint,rayNearestIntersection.dist);
			}

			// The calculation is needed only if Si is not 0.
			if (Si != 0) {
				SigmaColor = new Vec();

				double NdotLi = Vec.dotProd(N, Li);
				// Check is because of double calculations. Add Kd(NdotLi) to
				// the color that will be received from formula.
				if (NdotLi > Ray.eps) {
					SigmaColor.add(Vec.scale(NdotLi, Kd));
				}

				// Add Ks(VdotRi)^n to the color that will be received from
				// formula.
				Ri = Li.reflect(N);
				double VdotRi = Vec.dotProd(V, Ri);
				if (VdotRi > Ray.eps) {
					double VdorRiPowern = Math.pow(VdotRi, n);
					SigmaColor.add(Vec.scale(VdorRiPowern, Ks));
				}

				SigmaColor.scale(Si);

				Ii = light.getColor(intersectionPoint);
				SigmaColor.scale(Ii);

				I.add(SigmaColor);
			}

		}

		// Get the reflection direction of the intersection to the eye.
		Vec reflection = V.reflect(N);
		reflection.normalize();

		Ray reflectionRay = new Ray(intersectionPoint, reflection);
		MinIntersection reflectionHit = findIntersection(reflectionRay);

		curLevel++;

		// Calculate KrIr recursively as needed and add it to I.
		I.mac(Kr,
				calcColor(reflectionRay, curLevel, reflectionHit));

		return I;
	
	}

	/**
	 * Add objects to the scene by name
	 * 
	 * @param name
	 *            Object's name
	 * @param attributes
	 *            Object's attributes
	 */
	public void addObjectByName(String name, Map<String, String> attributes) {
		Scanner scan = null;
		Surface surface = null;
		Light light = null;

		if ("sphere".equals(name))
			surface = new sphere();

		if ("disc".equals(name)) {
			surface = new disc();
		}
		
		if ("trimesh".equals(name)) {
			for (String key : attributes.keySet()) {
				if (key.startsWith("tri")) {
					// The triangle 3 points are given in 3 coordinates each.
					double triP0x, triP0y, triP0z;
					double triP1x, triP1y, triP1z;
					double triP2x, triP2y, triP2z;
					Point3D p1, p2, p3;
					Triangle triangle;

					scan = new Scanner(attributes.get(key));
					triP0x = scan.nextDouble();
					triP0y = scan.nextDouble();
					triP0z = scan.nextDouble();
					p1 = new Point3D(triP0x, triP0y, triP0z);

					triP1x = scan.nextDouble();
					triP1y = scan.nextDouble();
					triP1z = scan.nextDouble();
					p2 = new Point3D(triP1x, triP1y, triP1z);

					triP2x = scan.nextDouble();
					triP2y = scan.nextDouble();
					triP2z = scan.nextDouble();
					p3 = new Point3D(triP2x, triP2y, triP2z);

					triangle = new Triangle(p1, p2, p3);
					triangle.init(attributes);
					this.surfaces.add(triangle);
				}
			}
		}
			
		// Lights objects
		if ("omni-light".equals(name))
			light = new omniLight();
		if ("dir-light".equals(name))
			light = new dirLight();
		if ("spot-light".equals(name))
			light = new spotLight();

		// Adds a surface to the list of surfaces
		if (surface != null) {
			surface.init(attributes);
			surfaces.add(surface);
		}

		// Adds a light to the list of lights
		if (light != null) {
			light.init(attributes);
			lights.add(light);
		}

	}

	public void setCameraAttributes(Map<String, String> attributes) {
		this.camera.init(attributes);
	}
}
