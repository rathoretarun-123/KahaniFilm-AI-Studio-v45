package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Procedural, renderable mesh assets for stories when no suitable source model exists.
 * Each model is a real Studio3D triangle mesh, not a painted background. The deliberately
 * modest topology is suitable for phone-side previews; detailed imported GLB assets remain
 * preferable for hero close-ups.
 */
public final class ProceduralWorldAssets {
    private ProceduralWorldAssets() {}

    public static final class Part {
        public final String name;
        public final int firstVertex, endVertex;
        public final float px, py, pz;
        private final float[] rest;
        private final float[] restNormals;
        private float angle;
        Part(String name, Studio3D.Mesh mesh, int first, int end, float px, float py, float pz) {
            this.name=name; firstVertex=first; endVertex=end; this.px=px; this.py=py; this.pz=pz;
            rest=new float[(end-first)*3]; restNormals=new float[(end-first)*3];
            System.arraycopy(mesh.v,first*3,rest,0,rest.length);
            System.arraycopy(mesh.n,first*3,restNormals,0,restNormals.length);
        }
    }

    public static final class Model {
        public final String id, kind;
        public final Studio3D.Scene scene = new Studio3D.Scene();
        public final List<Part> parts = new ArrayList<Part>();
        public int triangles() { return scene.mesh.triangles(); }

        Model(String id, String kind) { this.id=id; this.kind=kind; }

        /** Time-based quadruped animation. phaseSeconds is absolute clip time, not frame count. */
        public void poseQuadruped(float phaseSeconds, float stride, float headLookDeg, float tailSwingDeg) {
            reset();
            float amount=Math.max(0,Math.min(1,Math.abs(stride)));
            float phase=phaseSeconds*(float)(Math.PI*2)*1.4f;
            for (Part p:parts) {
                String n=p.name.toLowerCase(Locale.ROOT);
                if (n.equals("front_leg_l")) p.angle=(float)Math.sin(phase)*24f*stride;
                else if (n.equals("front_leg_r")) p.angle=(float)Math.sin(phase+Math.PI)*24f*stride;
                else if (n.equals("hind_leg_l")) p.angle=(float)Math.sin(phase+Math.PI)*27f*stride;
                else if (n.equals("hind_leg_r")) p.angle=(float)Math.sin(phase)*27f*stride;
                else if (n.equals("head")) p.angle=headLookDeg;
                else if (n.equals("tail")) p.angle=tailSwingDeg+(float)Math.sin(phase*0.5f)*8f*amount;
                apply(p);
            }
            // Small body bob is a separate, deterministic root-level action.
            float bob=(float)Math.abs(Math.sin(phase*2))*0.025f*amount;
            for (int i=0;i<scene.mesh.nv;i++) scene.mesh.v[i*3+1]+=bob;
        }

        /** Generic articulated motion for props and environment parts, evaluated from absolute time. */
        public void poseProp(float timeSeconds, String action, float amount) {
            reset();
            String a=action==null?"idle":action.toLowerCase(Locale.ROOT);
            float u=Math.max(0,Math.min(1,amount));
            for (Part p:parts) {
                String n=p.name.toLowerCase(Locale.ROOT);
                if (a.contains("open") && (n.contains("lid") || n.contains("door") || n.contains("window") || n.contains("flap"))) {
                    p.angle=EverydayPhysics.ease(0,90*u,timeSeconds,0.35f);
                } else if ((a.contains("close") || a.contains("shut")) && (n.contains("lid") || n.contains("door") || n.contains("window") || n.contains("flap"))) {
                    p.angle=EverydayPhysics.ease(90*u,0,timeSeconds,0.35f);
                } else if (a.contains("rotate") || a.contains("fan") || a.contains("wheel") || a.contains("spin")) {
                    if (n.contains("blade") || n.contains("wheel") || n.contains("rotor") || n.contains("handle")) {
                        p.angle=(float)Math.toDegrees(timeSeconds*Math.max(0.05f,u)*6.2831853f);
                    }
                } else if ((a.contains("swing") || a.contains("flap") || a.contains("wind")) &&
                        (n.contains("shade") || n.contains("curtain") || n.contains("wing") || n.contains("leaf") || n.contains("sign"))) {
                    p.angle=EverydayPhysics.spring((float)Math.sin(timeSeconds*8f)*18f*u,0,timeSeconds,2.5f,0.8f);
                } else if ((a.contains("shake") || a.contains("rattle") || a.contains("vibrate")) &&
                        (n.contains("body") || n.contains("panel") || n.contains("handle") || n.contains("loose"))) {
                    p.angle=(float)Math.sin(timeSeconds*24f)*3.5f*u;
                } else if ((a.contains("pour") || a.contains("tilt") || a.contains("drink")) &&
                        (n.contains("body") || n.contains("cup") || n.contains("pot") || n.contains("kettle"))) {
                    p.angle=EverydayPhysics.ease(0,Math.min(75f,70f*u),timeSeconds,0.25f);
                } else if ((a.contains("idle") || a.contains("settle")) && n.contains("tail")) {
                    p.angle=(float)Math.sin(timeSeconds*2.5f)*2f*u;
                }
                apply(p);
            }
        }

        /** Deterministic gait for animals; callers choose speed/stride and should provide species-specific timing. */
        public void poseAnimal(float timeSeconds, String action, float intensity, float headLookDeg) {
            String a=action==null?"idle":action.toLowerCase(Locale.ROOT);
            if (kind.equals("ANIMAL_BIRD")) {
                poseBird(timeSeconds, a.contains("fly")||a.contains("flap")?intensity:0, headLookDeg);
            } else if (kind.equals("ANIMAL_QUADRUPED")) {
                float stride=(a.contains("walk")||a.contains("run")||a.contains("trot")||a.contains("gallop"))?intensity:0;
                poseQuadruped(timeSeconds,stride,headLookDeg,a.contains("wag")?18f*intensity:0);
            }
        }

        /** A simple expressive bird flap and head-turn action. */
        public void poseBird(float phaseSeconds, float flapAmount, float headLookDeg) {
            reset();
            float flap=(float)Math.sin(phaseSeconds*(float)(Math.PI*2)*4)*35f*Math.max(0,Math.min(1,flapAmount));
            for (Part p:parts) {
                if (p.name.equals("wing_l")) p.angle=flap;
                else if (p.name.equals("wing_r")) p.angle=-flap;
                else if (p.name.equals("head")) p.angle=headLookDeg;
                apply(p);
            }
        }

        public void reset() {
            Studio3D.Mesh m=scene.mesh;
            for (Part p:parts) {
                System.arraycopy(p.rest,0,m.v,p.firstVertex*3,p.rest.length);
                System.arraycopy(p.restNormals,0,m.n,p.firstVertex*3,p.restNormals.length);
                p.angle=0;
            }
        }

        private void apply(Part p) {
            if (Math.abs(p.angle)<0.001f) return;
            Studio3D.Mesh m=scene.mesh;
            double a=Math.toRadians(p.angle); float c=(float)Math.cos(a), s=(float)Math.sin(a);
            for (int i=p.firstVertex;i<p.endVertex;i++) {
                int k=(i-p.firstVertex)*3, j=i*3;
                float x=p.rest[k]-p.px, y=p.rest[k+1]-p.py;
                m.v[j]=p.px+x*c-y*s;
                m.v[j+1]=p.py+x*s+y*c;
                m.v[j+2]=p.rest[k+2];
                float nx=p.restNormals[k], ny=p.restNormals[k+1];
                m.n[j]=nx*c-ny*s; m.n[j+1]=nx*s+ny*c; m.n[j+2]=p.restNormals[k+2];
            }
        }

        /** Render the current pose with the same rasterizer used by 3D sets and figure previews. */
        public Studio3D.Picture render(int width,int height,int supersampling) {
            return Studio3D.render(scene,width,height,supersampling);
        }
    }

    private static Part begin(Model m,String name,float px,float py,float pz) {
        return new Part(name,m.scene.mesh,m.scene.mesh.nv,m.scene.mesh.nv,px,py,pz);
    }
    private static void finish(Model m,Part p) {
        // Replace the empty capture with a complete immutable bind-pose range.
        m.parts.add(new Part(p.name,m.scene.mesh,p.firstVertex,m.scene.mesh.nv,p.px,p.py,p.pz));
    }

    /** Builds a four-legged animal with a head, tail and independently swinging limbs. */
    public static Model quadruped(String id,int coatColor,int muzzleColor) {
        Model m=new Model(id,"ANIMAL_QUADRUPED");
        Studio3D.Scene s=m.scene;
        int coat=s.mesh.mat(Studio3D.fur(coatColor));
        int muzzle=s.mesh.mat(Studio3D.fur(muzzleColor));
        int eye=s.mesh.mat(Studio3D.eye(0xFF241B17));
        part(m,"body",0,0.72f,0,new Build() { public void run() { s.mesh.sphere(0,0.76f,0,0.52f,0.31f,0.28f,12,coat); } });
        part(m,"head",0.42f,1.00f,0,new Build() { public void run() {
            s.mesh.sphere(0.43f,1.04f,0,0.24f,0.22f,0.21f,10,coat);
            s.mesh.sphere(0.60f,0.98f,0,0.13f,0.10f,0.13f,8,muzzle);
            s.mesh.sphere(0.48f,1.11f,0.19f,0.035f,0.035f,0.025f,6,eye);
            s.mesh.sphere(0.48f,1.11f,-0.19f,0.035f,0.035f,0.025f,6,eye);
        } });
        part(m,"front_leg_l",-0.29f,0.55f,0.13f,new Build() { public void run() { s.mesh.capsule(-0.29f,0.59f,0.14f,-0.34f,0.08f,0.14f,0.085f,0.055f,7,coat); } });
        part(m,"front_leg_r",-0.29f,0.55f,-0.13f,new Build() { public void run() { s.mesh.capsule(-0.29f,0.59f,-0.14f,-0.34f,0.08f,-0.14f,0.085f,0.055f,7,coat); } });
        part(m,"hind_leg_l",0.32f,0.55f,0.14f,new Build() { public void run() { s.mesh.capsule(0.32f,0.59f,0.14f,0.36f,0.08f,0.14f,0.12f,0.055f,7,coat); } });
        part(m,"hind_leg_r",0.32f,0.55f,-0.14f,new Build() { public void run() { s.mesh.capsule(0.32f,0.59f,-0.14f,0.36f,0.08f,-0.14f,0.12f,0.055f,7,coat); } });
        part(m,"tail",-0.48f,0.83f,0,new Build() { public void run() { s.mesh.capsule(-0.45f,0.84f,0,-0.78f,1.00f,0,0.075f,0.025f,7,coat); } });
        configureCamera(s,0.8f,0.6f,4.4f,0.75f,0.65f,0);
        return m;
    }

    /** Builds a bird with a beak, eyes, tail and flappable wings. */
    public static Model bird(String id,int featherColor,int beakColor) {
        Model m=new Model(id,"ANIMAL_BIRD"); Studio3D.Scene s=m.scene;
        int feather=s.mesh.mat(Studio3D.fur(featherColor)), beak=s.mesh.mat(Studio3D.plastic(beakColor)), eye=s.mesh.mat(Studio3D.eye(0xFF171717));
        part(m,"body",0,0.6f,0,new Build() { public void run() { s.mesh.sphere(0,0.67f,0,0.32f,0.25f,0.24f,10,feather); } });
        part(m,"head",0.24f,0.91f,0,new Build() { public void run() {
            s.mesh.sphere(0.24f,0.94f,0,0.18f,0.17f,0.17f,9,feather);
            s.mesh.capsule(0.36f,0.91f,0,0.53f,0.88f,0,0.07f,0.005f,6,beak);
            s.mesh.sphere(0.28f,0.99f,0.145f,0.025f,0.025f,0.02f,5,eye);
            s.mesh.sphere(0.28f,0.99f,-0.145f,0.025f,0.025f,0.02f,5,eye);
        } });
        part(m,"wing_l",0,0.75f,0.17f,new Build() { public void run() { s.mesh.blob(-0.02f,0.76f,0.20f,0.27f,0.09f,0.055f,8,feather); } });
        part(m,"wing_r",0,0.75f,-0.17f,new Build() { public void run() { s.mesh.blob(-0.02f,0.76f,-0.20f,0.27f,0.09f,0.055f,8,feather); } });
        part(m,"tail",-0.25f,0.62f,0,new Build() { public void run() { s.mesh.blob(-0.30f,0.62f,0,0.18f,0.08f,0.10f,7,feather); } });
        part(m,"feet",0,0.22f,0,new Build() { public void run() {
            s.mesh.capsule(-0.05f,0.38f,0.08f,-0.05f,0.08f,0.08f,0.035f,0.025f,5,beak);
            s.mesh.capsule(0.08f,0.38f,-0.08f,0.08f,0.08f,-0.08f,0.035f,0.025f,5,beak);
        } });
        configureCamera(s,0.6f,0.65f,3.6f,0.55f,0.65f,0);
        return m;
    }

    /** Rigid prop factory. All props have a stable origin so they can be attached to character anchors. */
    public static Model prop(String type,int color) {
        String k=type==null?"generic":type.toLowerCase(Locale.ROOT);
        Model m=new Model("prop:"+k,"PROP"); Studio3D.Scene s=m.scene;
        int main=s.mesh.mat(new Studio3D.Material(color));
        int dark=s.mesh.mat(Studio3D.stone(0xFF49423B));
        int metal=s.mesh.mat(Studio3D.metal(0xFFAAAEB5));
        if (k.contains("stick") || k.contains("lathi")) {
            s.mesh.capsule(0,0,0,0,1.2f,0,0.07f,0.045f,8,main);
        } else if (k.contains("basket")) {
            s.mesh.torus(0,0.65f,0,0.32f,0.045f,1,10,main);
            s.mesh.cylinder(-0.26f,0.05f,0,0.26f,0.05f,0,0.25f,0.25f,10,main);
            s.mesh.capsule(-0.28f,0.62f,0,0,1.03f,0,0.035f,0.035f,7,main);
            s.mesh.capsule(0,1.03f,0,0.28f,0.62f,0,0.035f,0.035f,7,main);
        } else if (k.contains("lota") || k.contains("kettle") || k.contains("pot")) {
            s.mesh.sphere(0,0.38f,0,0.28f,0.32f,0.28f,10,main);
            s.mesh.cylinder(0,0.63f,0,0,0.76f,0,0.14f,0.10f,8,main);
            s.mesh.torus(0,0.75f,0,0.10f,0.025f,1,8,metal);
            if (k.contains("kettle")) s.mesh.capsule(0.19f,0.47f,0,0.38f,0.68f,0,0.04f,0.02f,7,main);
        } else if (k.contains("radio")) {
            s.mesh.box(0,0.38f,0,0.72f,0.42f,0.25f,main);
            s.mesh.box(-0.12f,0.40f,0.132f,0.36f,0.26f,0.012f,dark);
            s.mesh.sphere(0.23f,0.37f,0.14f,0.055f,0.055f,0.025f,6,metal);
            s.mesh.cylinder(-0.23f,0.59f,-0.04f,-0.31f,0.96f,-0.04f,0.012f,0.006f,5,metal);
        } else if (k.contains("key")) {
            s.mesh.torus(-0.12f,0.52f,0,0.13f,0.035f,2,10,metal);
            s.mesh.cylinder(0.02f,0.52f,0,0.48f,0.52f,0,0.035f,0.025f,8,metal);
            s.mesh.box(0.36f,0.46f,0,0.06f,0.12f,0.06f,metal);
        } else if (k.contains("lantern")) {
            s.mesh.box(0,0.42f,0,0.28f,0.42f,0.22f,dark);
            s.mesh.box(0,0.42f,0.116f,0.17f,0.29f,0.012f,main);
            s.mesh.torus(0,0.69f,0,0.09f,0.018f,1,7,metal);
            s.mesh.cylinder(0,0.62f,0,0,0.80f,0,0.018f,0.018f,6,metal);
        } else {
            s.mesh.box(0,0.35f,0,0.5f,0.5f,0.5f,main);
        }
        configureCamera(s,0.4f,0.5f,3.8f,0.35f,0.5f,0);
        return m;
    }

    /** Modular environment set with actual ground, trunks, foliage, rocks and a small village hut. */
    public static Model location(String type,int seed) {
        String k=type==null?"forest":type.toLowerCase(Locale.ROOT);
        Model m=new Model("location:"+k,"ENVIRONMENT"); Studio3D.Scene s=m.scene;
        java.util.Random r=new java.util.Random(seed);
        int ground=s.mesh.mat(Studio3D.stone(k.contains("cave")?0xFF62594F:0xFF6D8650));
        int trunk=s.mesh.mat(Studio3D.wood(0xFF67452F));
        int leaves=s.mesh.mat(Studio3D.leaf(0xFF3D7945));
        int stone=s.mesh.mat(Studio3D.stone(0xFF817A70));
        int wall=s.mesh.mat(Studio3D.stone(0xFFC9A779));
        int roof=s.mesh.mat(Studio3D.wood(0xFF814A32));
        int caveDark=s.mesh.mat(new Studio3D.Material(0xFF211D19));
        s.mesh.box(0,-0.14f,-1.8f,12f,0.25f,12f,ground);
        if (k.contains("cave")) {
            s.mesh.sphere(0,1.4f,-3.5f,3.2f,2.3f,2.1f,14,stone);
            s.mesh.sphere(0,1.25f,-2.2f,2.15f,1.55f,1.5f,12,ground);
            s.mesh.box(0,0.65f,-1.0f,2.4f,1.3f,0.2f,caveDark);
        } else {
            for(int i=0;i<9;i++) {
                float x=(r.nextFloat()-0.5f)*10f, z=-2.5f-r.nextFloat()*6f, h=1.7f+r.nextFloat()*1.8f;
                s.mesh.cylinder(x,0,z,x,h,z,0.12f,0.08f,7,trunk);
                s.mesh.sphere(x,h+0.42f,z,0.65f,0.78f,0.62f,9,leaves);
            }
            if(k.contains("village")||k.contains("room")||k.contains("courtyard")) {
                s.mesh.box(0,0.85f,-4.0f,2.8f,1.7f,1.5f,wall);
                s.mesh.box(0,1.85f,-4.0f,3.1f,0.22f,1.8f,roof);
                s.mesh.box(0,0.55f,-3.22f,0.58f,1.1f,0.08f,trunk);
                s.mesh.box(-0.75f,1.05f,-3.20f,0.48f,0.42f,0.08f,stone);
                s.mesh.box(0.75f,1.05f,-3.20f,0.48f,0.42f,0.08f,stone);
            }
            for(int i=0;i<7;i++) {
                float x=(r.nextFloat()-0.5f)*8f, z=-1.2f-r.nextFloat()*5f;
                s.mesh.sphere(x,0.12f,z,0.25f+r.nextFloat()*0.35f,0.18f,0.22f,7,stone);
            }
        }
        s.skyTop=0xFF91B9E8; s.skyBottom=0xFFD9D8B5;
        s.fogColor=0xFFD0D5CB; s.fogStart=7; s.fogEnd=20; s.dof=0.35f;
        configureCamera(s,0,2.0f,11f,0,1.0f,-4f);
        return m;
    }

    private interface Build { void run(); }
    private static void part(Model m,String name,float px,float py,float pz,Build build) {
        int start=m.scene.mesh.nv; build.run();
        m.parts.add(new Part(name,m.scene.mesh,start,m.scene.mesh.nv,px,py,pz));
    }
    private static void configureCamera(Studio3D.Scene s,float x,float y,float z,float lx,float ly,float lz) {
        s.camX=x; s.camY=y; s.camZ=z; s.lookX=lx; s.lookY=ly; s.lookZ=lz;
        s.fovDeg=35; s.shadows=true; s.ao=true; s.aoRadius=0.08f;
        s.keyX=-0.45f; s.keyY=1f; s.keyZ=0.75f; s.keyStrength=1.15f;
        s.fillStrength=0.4f; s.rimStrength=0.45f;
    }
}
