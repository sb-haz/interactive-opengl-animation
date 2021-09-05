import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.input.Keyboard;
import org.newdawn.slick.opengl.Texture;
import GraphicsLab.*;

/**
 * Description: A night-time ocean scene.
 *    Both the sky and environment below the water surface are visible
 *    There is light from the moon (above water) and coral reef (below water)
 *    
 * Animation: There are two animations, one running by itself one controllable by user
 * Boat animation: if R key is pressed, boatMovingWest is set to true and boats offset is incremented
 *    else if L is pressed, boatMovingWest is set to false and boats offset is decremented
 * Fish animation: timed by an animation delta, if fish X offset reaches a predetermined end position,
 *    it's moved back to the start.  Keeps looping infinitely until animations are reset by user
 *
 * Controls:
 * 
 * Press the escape key to exit the application.
 * Hold the x, y and z keys to view the scene along the x, y and z axis, respectively
 * While viewing the scene along the x, y or z axis, use the up and down cursor keys
 *  to increase or decrease the viewpoint's distance from the scene origin
 * Press L, this makes the boat move left
 * Press R, this makes the boat move right
 * Press space bar, this resets the animations
 *
 * 
 * Scene graph:
 *  Scene origin
 *  |
 *  +-- [S(22, 1, 17) Rx(90) T(0, 0, -20)] Back plane
 *  |
 *  +-- [Rx(15) S(2, 1, 0.5) T(currentBoatX, boatPosY, -18)] Boat
 *  |
 *  +-- [S(0.3, 0.3, 1) T(fishPosX, fishPosY, -8)] Fish
 *  |   |
 *  |   +-- [T(-1, 0, 0) Ry(fishTilt)] Tail
 *  |   |
 *  |   +-- [T(1.15, 0.25, 0.2)] Left eye
 *  |   |
 *  |   +-- [T(1.15, 0.25, -0.2)] Right eye
 *
 */

public class OceanAnimation extends GraphicsLab {
	
	/** Display list ID for the Unit Plane */
	private final int planeList = 1;

	/** Display list ID for the Fish */
	private final int fishBodyList  = 2;
	private final int fishTailList  = 3;
	private final int fishEyeList  = 4;

	/** Display list ID for the Boat */
	private final int boatList = 5;
	
	/** Textures */
	private Texture oceanSkyTextures;
	//private Texture shipTextures;
	
	/** Boat current x position */
    private float currentBoatX = -10.0f;

    /** Boat start x position */
    private final float startBoatX = currentBoatX; 

    /** Boat end x position */
    private final float endBoatX  = 10.0f;

    /** If the boat is moving west (right) */
    private boolean boatMovingWest = true;

    /** Boat current y position */
    private float boatPosY = 0.0f;
    
    /** Fish current x position */
    private float fishPosX = -5f;

    /** Fish start x position */
    private float fishStartPosX = fishPosX;

    /** Fish end x position */
    private float fishEndPosX = 10f;

    /** Fish current y position */
    private float fishPosY = 0f;

    /** Fish rotation tilt in y axis */
    private float fishTilt = 0.0f;
    
    /** Animation timer (for fish to loop) */
    private float animationDelta = 0.0f;
    
    public static void main(String args[]){
    	new OceanAnimation().run(WINDOWED,"Ocean Scene",0.01f);
    }

    protected void initScene() throws Exception {
    	
    	/** Load background texture */
    	oceanSkyTextures = loadTexture("textures/ocean.bmp");
    	
    	/** Set global ambient light levels */
        float globalAmbient[]   = {0.2f,  0.2f,  0.2f, 1.0f};
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT,FloatBuffer.wrap(globalAmbient));
        
        /** Moonlight */
        /** First light for the scene is nearly white */
        float diffuse0[]  = { 0.8f,  0.8f, 0.8f, 1.0f};

        /** Dim ambient contribution */
        float ambient0[]  = { 0.1f,  0.1f, 0.1f, 1.0f};

        /** Is position above and in front viewpoint */
        float position0[] = { 0.0f, 10.0f, -10.0f, 1.0f};
        
        /** Supply OpenGL with first lights properties */
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, FloatBuffer.wrap(ambient0));
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, FloatBuffer.wrap(diffuse0));
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, FloatBuffer.wrap(diffuse0));
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, FloatBuffer.wrap(position0));

        /** Enable the first light */
        GL11.glEnable(GL11.GL_LIGHT0);
        
        /** Coral reef light */
        /** Second light for the scene is white */
        float diffuse1[]  = { 0.1f,  0.1f, 0.1f, 1.0f};

        /** Dim ambient contribution */
        float ambient1[]  = { 0.2f,  0.2f, 0.2f, 1.0f};

        /** Position below and behind the viewpoint */
        float position1[] = { 0.0f, -10.0f, 10.0f, 1.0f};
        
        /** Supply OpenGL with the second lights properties */
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, FloatBuffer.wrap(ambient1));
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, FloatBuffer.wrap(diffuse1));
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_SPECULAR, FloatBuffer.wrap(diffuse1));
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, FloatBuffer.wrap(position1));

        /** Enable the second light */
        GL11.glEnable(GL11.GL_LIGHT1);

        /** Enable lighting calculations */
        GL11.glEnable(GL11.GL_LIGHTING);

        /** Ensure that all normals are re-normalised after transformations automatically */
        GL11.glEnable(GL11.GL_NORMALIZE);
        
        /** Prepare display list: back plane */
        GL11.glNewList(planeList,GL11.GL_COMPILE);
        drawUnitPlane();
        GL11.glEndList();
        
        /** Prepare display list: fish body */
        GL11.glNewList(fishBodyList, GL11.GL_COMPILE);
        drawTriangle(1f);
        GL11.glEndList();
        
        /** Prepare display list: fish tail */
        GL11.glNewList(fishTailList, GL11.GL_COMPILE);
        drawTriangle(0.5f);
        GL11.glEndList();
        
        /** Prepare display list: fish eye */
        GL11.glNewList(fishEyeList, GL11.GL_COMPILE);
        drawTriangle(0.2f);
        GL11.glEndList();
        
        /** Prepare display list: boat */
        GL11.glNewList(boatList, GL11.GL_COMPILE);
        drawBoat();
        GL11.glEndList();
                
    }
            
    /** Method for input controls */
    protected void checkSceneInput(){

    	/** If L key is pressed */
        if(Keyboard.isKeyDown(Keyboard.KEY_L)){
        	boatMovingWest = false;
        }

        /** Else if R is pressed */
        else if(Keyboard.isKeyDown(Keyboard.KEY_R)){   
        	boatMovingWest = true;
        }

        /** Else if space is pressed */
        else if(Keyboard.isKeyDown(Keyboard.KEY_SPACE)){
        	resetAnimations();
        }
    }
    
    protected void updateScene(){
    	animationDelta += getAnimationScale();
    	
    	/** Tilt the fish tale */
    	fishTilt = (float) (Math.sin(animationDelta)/Math.PI) * 80;

    	/** Control the fish Y position to mimic bobbing */
    	fishPosY = (float) (Math.sin(animationDelta)/10) - 2;

    	/** If fish has reached the end, move it back to start */
    	if(fishPosX >= fishEndPosX) {
    		fishPosX = fishStartPosX;

        /** Otherwise update increment its X position */
    	} else {
    		fishPosX += 0.25f * getAnimationScale();
    	}
    	
    	/** Boat Y position, to mimic bobbing */
    	boatPosY = (float) (Math.sin(animationDelta/Math.PI)/10);
    	
    	/** If boat is moving west (right) and hasn't reached the farthest point on the right
    	    then increment boat's X position */
    	if(boatMovingWest && currentBoatX <= endBoatX) {
    		currentBoatX += 0.5f * getAnimationScale();
    		
    	/** If boat is moving east (left) and hasn't reached the left point on the right
           then decrement boat's X position */
    	} else if (!boatMovingWest && currentBoatX >= startBoatX) {
    		currentBoatX -= 0.5f * getAnimationScale();
    	}
    }
    
    protected void renderScene(){
    	/** Draw the back plane */
    	GL11.glPushMatrix();
        {
        	/** Disable lighting calculations so that they don't affect
        	    the appearance of the texture */
            GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);

            /** Change the geometry colour to white so that the texture
                is bright and details can be seen clearly */
            Colour.WHITE.submit();

            /** Enable texturing and bind an appropriate texture */
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,oceanSkyTextures.getTextureID()); 
            
            /** Position, scale and draw the back plane using its display list */
            GL11.glTranslatef(0.0f,0.0f,-20.0f);
            GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
            GL11.glScalef(22f, 1.0f, 17.0f);
            GL11.glCallList(planeList);
            
            /** Disable textures and reset any local lighting changes */
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPopAttrib();
        }

        GL11.glPopMatrix();
                
        /** Draw boat */
        GL11.glPushMatrix();
        {
        	/** How shiny are the front faces of the boat (specular exponent) */
	        float boatFrontShininess  = 2.0f;

	        /** Specular reflection of the front faces of the boat */
	        float boatFrontSpecular[] = {0.6f, 0.6f, 0.6f, 1.0f};

	        /** Diffuse reflection of the front faces of the boat */
	        float boatFrontDiffuse[]  = {0.6f, 0.6f, 0.6f, 1.0f};
	
	        /** Set the material properties for the sun using OpenGL */
	        GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, boatFrontShininess);
	        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, FloatBuffer.wrap(boatFrontSpecular));
	        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, FloatBuffer.wrap(boatFrontDiffuse));
	
	        /** Position and draw boat */
	        GL11.glTranslatef(currentBoatX, boatPosY, -18f);
	        GL11.glScalef(2.0f, 1.0f, 0.5f);
	        GL11.glRotatef(15f, 1f, 0f, 0f);
	        GL11.glCallList(boatList);
        }

        GL11.glPopMatrix();
        
        /** Draw fish */
        GL11.glPushMatrix();
        {
        	/** How shiny are the front faces of the fish (specular exponent) */
            float fishFrontShininess  = 2.0f;

            /** Specular reflection of the front faces of the fish */
            float fishFrontSpecular[] = {0.9f, 0.6f, 0.0f, 1.0f};

            /** Diffuse reflection of the front faces of the fish */
            float fishFrontDiffuse[]  = {0.9f, 0.6f, 0.0f, 1.0f};

            /** Set the material properties for the sun using OpenGL */
            GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, fishFrontShininess);
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, FloatBuffer.wrap(fishFrontSpecular));
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, FloatBuffer.wrap(fishFrontDiffuse));

            /** Position and draw fish body, use fishPosX and fishPosY to create animation */
            GL11.glTranslatef(fishPosX, fishPosY, -8f);
            GL11.glScalef(0.3f, 0.3f, 1f);
            GL11.glCallList(fishBodyList);
            
            /** Position and draw fish tail */
            GL11.glPushMatrix();
            {
	            /** Use fishTilt variable to create animation */
	            GL11.glRotatef(fishTilt, 0f, 1f, 0f);
	            GL11.glTranslatef(-1f, 0f, 0f);
	            GL11.glCallList(fishTailList);
            }

            GL11.glPopMatrix();
            
            /** Position and draw fish left eye */
            GL11.glPushMatrix();
            {
	            /** How shiny are the front faces of the fish (specular exponent) */
	            float fishEyeFrontShininess  = 2.0f;
	            /** Specular reflection of the front faces of the fish */
	            float fishEyeFrontSpecular[] = {1f, 1f, 1f, 1.0f};
	            /** Diffuse reflection of the front faces of the fish */
	            float fishEyeFrontDiffuse[]  = {1f, 1f, 1f, 1.0f};
	
	            /** Set the material properties for the sun using OpenGL */
	            GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, fishEyeFrontShininess);
	            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, FloatBuffer.wrap(fishEyeFrontSpecular));
	            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, FloatBuffer.wrap(fishEyeFrontDiffuse));
	            
	            GL11.glTranslatef(1.15f, 0.25f, 0.2f);
	            GL11.glCallList(fishEyeList);
            }
            GL11.glPopMatrix();
            
            /** Position and draw fish right eye */
            GL11.glPushMatrix();
            {
	            /** How shiny are the front faces of the fish (specular exponent) */
	            float fishEyeFrontShininess  = 2.0f;

	            /** Specular reflection of the front faces of the fish */
	            float fishEyeFrontSpecular[] = {1f, 1f, 1f, 1.0f};

	            /** Diffuse reflection of the front faces of the fish */
	            float fishEyeFrontDiffuse[]  = {1f, 1f, 1f, 1.0f};
	
	            /** Set the material properties for the sun using OpenGL */
	            GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, fishEyeFrontShininess);
	            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, FloatBuffer.wrap(fishEyeFrontSpecular));
	            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, FloatBuffer.wrap(fishEyeFrontDiffuse));
	            
	            GL11.glTranslatef(1.15f, 0.25f, -0.2f);
	            GL11.glCallList(fishEyeList);
            }

            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
        
    }

    protected void setSceneCamera(){
        /** Call the default behaviour defined in GraphicsLab. This will set a default perspective projection
            and default camera settings ready for some custom camera positioning below... */
        super.setSceneCamera();
   }
    
    protected void resetAnimations(){

    	/** Move fish and boat to start */
    	fishPosX = fishStartPosX;
    	currentBoatX = startBoatX;
    }

    protected void cleanupScene(){
    }
        
    private void drawBoat() {
    	Vertex v1 = new Vertex(-0.5f, 0.0f, 0.0f);
    	Vertex v2 = new Vertex( 0.5f, 0.0f, 0.0f);
    	Vertex v3 = new Vertex( 0.5f, 1.0f, 0.5f);
    	Vertex v4 = new Vertex(-0.5f, 1.0f, 0.5f);
    	Vertex v5 = new Vertex( 0.5f, 1.0f,-0.5f);
    	Vertex v6 = new Vertex(-0.5f, 1.0f,-0.5f);
    	
    	Vertex v7 = new Vertex( 1.5f, 1.0f, 0.0f);
    	Vertex v8 = new Vertex(-1.5f, 1.0f, 0.0f);
    	    	
    	/** Front face */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
	    	new Normal(v1.toVector(), v2.toVector(), v3.toVector(), v4.toVector()).submit();
	    	v1.submit();
	    	v2.submit();
	    	v3.submit();
	    	v4.submit();
	  	}

    	GL11.glEnd();
    	
    	/** Back face */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
	    	new Normal(v1.toVector(), v6.toVector(), v5.toVector(), v2.toVector()).submit();
	    	v1.submit();
	    	v6.submit();
	    	v5.submit();
	    	v2.submit();
	  	}

    	GL11.glEnd();
    	
    	/** Right front */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
	    	new Normal(v2.toVector(), v7.toVector(), v3.toVector()).submit();
	    	v2.submit();
	    	v7.submit();
	    	v3.submit();
	    	GL11.glEnd();
    	}
    	
    	/** Right back */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
	    	new Normal(v2.toVector(), v5.toVector(), v7.toVector()).submit();
	    	v2.submit();
	    	v5.submit();
	    	v7.submit();
	    	GL11.glEnd();
    	}
    	
    	/** Left front */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
	    	new Normal(v1.toVector(), v4.toVector(), v8.toVector()).submit();
	    	v1.submit();
	    	v4.submit();
	    	v8.submit();
	    	GL11.glEnd();
    	}
    	
    	/** Left back */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
	    	new Normal(v1.toVector(), v8.toVector(), v6.toVector()).submit();
	    	v1.submit();
	    	v8.submit();
	    	v6.submit();
	    	GL11.glEnd();
    	}
    	
    	/** Top cover center */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
    		new Normal(v4.toVector(), v3.toVector(), v5.toVector(), v6.toVector()).submit();
	    	v4.submit();
	    	v3.submit();
	    	v5.submit();
	    	v6.submit();
	    	GL11.glEnd();
    	}
    	
    	/** Top cover right */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
    		new Normal(v3.toVector(), v7.toVector(), v5.toVector()).submit();
	    	v3.submit();
	    	v7.submit();
	    	v5.submit();
	    	GL11.glEnd();
    	}
    	
    	/** Top cover left */
    	GL11.glBegin(GL11.GL_POLYGON);
    	{
    		new Normal(v4.toVector(), v6.toVector(), v8.toVector()).submit();
	    	v4.submit();
	    	v6.submit();
	    	v8.submit();
	    	GL11.glEnd();
    	}
    }
    
    private void drawTriangle(float size) {
   
    	Vertex v1 = new Vertex(size*0.0f,size*-1.0f, size*0.2f); //front, bottom
    	Vertex v2 = new Vertex(size*0.0f, size*1.0f, size*0.2f); //front, top
    	Vertex v3 = new Vertex(size*2.5f, size*0.0f, size*0.2f); //front, right
    	Vertex v4 = new Vertex(size*0.0f,size*-1.0f,size*-0.2f); //back, bottom
    	Vertex v5 = new Vertex(size*0.0f, size*1.0f,size*-0.2f); //back, top
    	Vertex v6 = new Vertex(size*2.5f, size*0.0f,size*-0.2f); //back, right
    	
    	/** Draw the front face */
        GL11.glBegin(GL11.GL_POLYGON);
        {
            new Normal(v1.toVector(),v3.toVector(),v2.toVector()).submit();
            v1.submit();
            v3.submit();
            v2.submit();
        }
        GL11.glEnd();
        
        /** Draw the back face */
        GL11.glBegin(GL11.GL_POLYGON);
        {
            new Normal(v4.toVector(),v6.toVector(),v5.toVector()).submit();
            v4.submit();
            v6.submit();
            v5.submit();
        }
        GL11.glEnd();
        
        /** Draw the top face */
        GL11.glBegin(GL11.GL_POLYGON);
        {
            new Normal(v2.toVector(),v3.toVector(),v6.toVector(),v5.toVector()).submit();
            v2.submit();
            v3.submit();
            v6.submit();
            v5.submit();
        }
        GL11.glEnd();
        
        /** Draw the bottom face */
        GL11.glBegin(GL11.GL_POLYGON);
        {
            new Normal(v1.toVector(),v4.toVector(),v6.toVector(),v3.toVector()).submit();
            v1.submit();
            v4.submit();
            v6.submit();
            v3.submit();
        }
        GL11.glEnd();
        
        /** Draw the left face */
        GL11.glBegin(GL11.GL_POLYGON);
        {
            new Normal(v1.toVector(),v2.toVector(),v5.toVector(),v4.toVector()).submit();
            v1.submit();
            v2.submit();
            v5.submit();
            v4.submit();
        }
        GL11.glEnd();
    	
    }
    
    /**
     * Draws a plane aligned with the X and Z axis, with its front face toward positive Y.
     *  The plane is of unit width and height, and uses the current OpenGL material settings
     *  for its appearance
     */
    private void drawUnitPlane(){
    	
        Vertex v1 = new Vertex(-0.5f, 0.0f,-0.5f);
        Vertex v2 = new Vertex( 0.5f, 0.0f,-0.5f);
        Vertex v3 = new Vertex( 0.5f, 0.0f, 0.5f);
        Vertex v4 = new Vertex(-0.5f, 0.0f, 0.5f);
        
        /** Draw the plane geometry. order the vertices so that the plane faces up */
        GL11.glBegin(GL11.GL_POLYGON);
        {
            new Normal(v4.toVector(),v3.toVector(),v2.toVector(),v1.toVector()).submit();
            
            GL11.glTexCoord2f(0.0f,0.0f);
            v4.submit();
            
            GL11.glTexCoord2f(1.0f,0.0f);
            v3.submit();
            
            GL11.glTexCoord2f(1.0f,1.0f);
            v2.submit();
            
            GL11.glTexCoord2f(0.0f,1.0f);
            v1.submit();
        }

        GL11.glEnd();
        
        /** If the user is viewing an axis, then also draw this plane
            using lines so that axis aligned planes can still be seen */
        if(isViewingAxis()){
        	
        	/** Also disable textures when drawing as lines
                so that the lines can be seen more clearly */
            GL11.glPushAttrib(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            {
                v4.submit();
                v3.submit();
                v2.submit();
                v1.submit();
            }

            GL11.glEnd();
            GL11.glPopAttrib();
        }
    }

}
