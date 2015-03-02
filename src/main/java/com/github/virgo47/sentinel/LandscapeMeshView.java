package com.github.virgo47.sentinel;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class LandscapeMeshView extends MeshView {

	private static final Logger log = Logger.getLogger(LandscapeMeshView.class.getName());

	public void setLandscape(Landscape landscape) throws FileNotFoundException {
		TriangleMesh landscapeMesh = new TriangleMesh();
		for (int x = 0; x <= landscape.sizeX; x++) {
			for (int y = 0; y <= landscape.sizeY; y++) {
				landscapeMesh.getPoints().addAll(x, y, landscape.pointHeight(x, y));
				log.finest("Added point: " + x + ", " + y + ", " + landscape.pointHeight(x, y));
			}
		}

		landscapeMesh.getTexCoords().addAll(
			0.0f, 0.0f,
			0.5f, 0.0f,
			0.5f, 0.5f,
			0.0f, 0.5f,
			0.5f, 0.0f,
			1.0f, 0.0f,
			1.0f, 0.5f,
			0.5f, 0.5f,
			0.0f, 0.5f,
			0.5f, 0.5f,
			0.5f, 1.0f,
			0.0f, 1.0f
		);

		// north = positive y, east = positive x
		for (int x = 0; x < landscape.sizeX; x++) {
			for (int y = 0; y < landscape.sizeY; y++) {
				int arraySizeY = landscape.sizeY + 1;
				int pointSW = x * arraySizeY + y;
				int pointNW = x * arraySizeY + (y + 1);
				int pointNE = (x + 1) * arraySizeY + y + 1;
				int pointSE = (x + 1) * arraySizeY + y;

				float zsw = landscapeMesh.getPoints().get(pointSW * 3 + 2);
				float zse = landscapeMesh.getPoints().get(pointSE * 3 + 2);
				float zne = landscapeMesh.getPoints().get(pointNE * 3 + 2);
				float znw = landscapeMesh.getPoints().get(pointNW * 3 + 2);

				// by default we make triangles: SW-SE-NE and SW-NE-NW, other way is "inverted"
				boolean triangle1IsFlat = zsw == zse && zsw == zne;
				boolean triangle2IsFlat = zsw == zne && zsw == znw;
				boolean triangleInverse1IsFlat = zsw == zse && zsw == znw;
				boolean triangleInverse2IsFlat = znw == zne && zse == zne;

				// if the whole square is not flat, but any of default triangles are flat, we want to inverse the triangulation
				boolean plainSqure = triangle1IsFlat && triangle2IsFlat;
				boolean invertBecauseOfPartialFlatness = !plainSqure && (triangle1IsFlat || triangle2IsFlat);
				boolean invertedTrianglesSumIsLower = (2 * zsw + zse + 2 * zne + znw) > (zsw + 2 * zse + zne + 2 * znw);

				boolean inverseTriangulation = invertBecauseOfPartialFlatness
					// we also want to prefer valleys instead of high ridges IF it doesn't create new partially-flat squares
					|| invertedTrianglesSumIsLower && !triangleInverse1IsFlat && !triangleInverse2IsFlat;

				log.finer("Height(" + x + ',' + y + "): " + zsw + ", " + zse + ", " + zne + ", " + znw +
					" - triangulation " + (inverseTriangulation ? "inverse" : "default") +
					", t1flat " + triangle1IsFlat + ", t2flat " + triangle2IsFlat +
					", ti1flat " + triangleInverse1IsFlat + ", ti2flat " + triangleInverse2IsFlat +
					", pflat " + invertBecauseOfPartialFlatness + ", isum " + invertedTrianglesSumIsLower);

				int textureIndex = plainSqure ? (x + y) % 2 * 4 : 8;

				if (inverseTriangulation) {
					landscapeMesh.getFaces().addAll(
						pointSW, textureIndex, pointSE, textureIndex + 1, pointNW, textureIndex + 3,
						pointSE, textureIndex + 1, pointNE, textureIndex + 2, pointNW, textureIndex + 3
					);
				} else {
					landscapeMesh.getFaces().addAll(
						pointSW, textureIndex, pointSE, textureIndex + 1, pointNE, textureIndex + 2,
						pointSW, textureIndex, pointNE, textureIndex + 2, pointNW, textureIndex + 3
					);
				}
			}
		}
		setMesh(landscapeMesh);
		double scale = 10;
		setScaleX(scale);
		setScaleY(scale);
		setScaleZ(scale);

		setCullFace(CullFace.NONE);
		setDrawMode(DrawMode.FILL);

		PhongMaterial mat = new PhongMaterial();
		Image simpleTexture = new Image(getClass().getClassLoader().getResourceAsStream("textures-wood.jpg"));
		mat.setDiffuseMap(simpleTexture);
		setMaterial(mat);
	}
}
