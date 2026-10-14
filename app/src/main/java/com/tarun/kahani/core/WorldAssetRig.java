package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared geometry/rig data for non-human assets.
 *
 * <p>This is a renderer-neutral CPU mesh layer: vertices are stored in model space,
 * can be bound to up to four bones, and are skinned from a pose each frame. Static
 * world meshes (ground, walls, buildings, trees, furniture) use the same mesh format
 * with no bone weights. Props can use a small hierarchy and named attachment anchors.
 *
 * <p>Important: creating this data model does not by itself make an asset visible.
 * The film renderer must consume the resulting vertices and indices. Do not replace
 * the existing character renderer or its established Rig/Puppet pipeline with this
 * helper; integrate it as an additional path.
 */
public final class WorldAssetRig {
    private WorldAssetRig() {}

    public static final int MAX_INFLUENCES = 4;

    public static final class Vec3 {
        public float x, y, z;
        public Vec3() {}
        public Vec3(float x, float y, float z) { this.x=x; this.y=y; this.z=z; }
        public Vec3 copy() { return new Vec3(x,y,z); }
        public Vec3 add(Vec3 b) { return new Vec3(x+b.x,y+b.y,z+b.z); }
        public Vec3 sub(Vec3 b) { return new Vec3(x-b.x,y-b.y,z-b.z); }
        public Vec3 scale(float s) { return new Vec3(x*s,y*s,z*s); }
        public static Vec3 lerp(Vec3 a, Vec3 b, float t) {
            return new Vec3(a.x+(b.x-a.x)*t, a.y+(b.y-a.y)*t, a.z+(b.z-a.z)*t);
        }
    }

    /** A transform in local coordinates; rotations are Euler degrees in XYZ order. */
    public static final class Transform {
        public final Vec3 position = new Vec3();
        public final Vec3 rotation = new Vec3();
        public final Vec3 scale = new Vec3(1,1,1);
        public Transform copy() {
            Transform t = new Transform();
            t.position.x=position.x; t.position.y=position.y; t.position.z=position.z;
            t.rotation.x=rotation.x; t.rotation.y=rotation.y; t.rotation.z=rotation.z;
            t.scale.x=scale.x; t.scale.y=scale.y; t.scale.z=scale.z;
            return t;
        }
    }

    /** Parent index -1 means root. Rest transform is the bind pose; pose is animated. */
    public static final class Bone {
        public final String name;
        public final int parent;
        public final Transform bind = new Transform();
        public final Transform pose = new Transform();
        public Bone(String name, int parent) { this.name=name==null?"bone":name; this.parent=parent; }
    }

    /** One influence: bone index plus normalized weight. */
    public static final class Influence {
        public int bone = -1;
        public float weight;
        public Influence() {}
        public Influence(int bone, float weight) { this.bone=bone; this.weight=weight; }
    }

    public static final class Vertex {
        public final Vec3 position;
        public final Vec3 normal;
        public final float u, v;
        public final Influence[] skin = new Influence[MAX_INFLUENCES];
        public Vertex(Vec3 position, Vec3 normal, float u, float v) {
            this.position=position==null?new Vec3():position.copy();
            this.normal=normal==null?new Vec3(0,0,1):normal.copy();
            this.u=u; this.v=v;
            for (int i=0;i<skin.length;i++) skin[i]=new Influence();
        }
        public Vertex weight(int slot, int bone, float weight) {
            if (slot<0 || slot>=MAX_INFLUENCES) throw new IllegalArgumentException("skin slot");
            skin[slot].bone=bone; skin[slot].weight=Math.max(0,weight);
            return this;
        }
    }

    public static final class Mesh {
        public final String id;
        public final List<Vertex> vertices = new ArrayList<>();
        /** Triangle indices; each consecutive triple is one triangle. */
        public final List<Integer> triangles = new ArrayList<>();
        public Mesh(String id) { this.id=id==null?"mesh":id; }
        public Mesh vertex(Vertex v) { if (v!=null) vertices.add(v); return this; }
        public Mesh triangle(int a,int b,int c) {
            checkIndex(a); checkIndex(b); checkIndex(c);
            triangles.add(a); triangles.add(b); triangles.add(c); return this;
        }
        private void checkIndex(int i) {
            if (i<0 || i>=vertices.size()) throw new IllegalArgumentException("triangle index out of range: "+i);
        }
        public boolean isValid() {
            if (vertices.isEmpty() || triangles.size()%3!=0) return false;
            for (int i:triangles) if (i<0 || i>=vertices.size()) return false;
            for (Vertex v:vertices) {
                float total=0;
                for (Influence inf:v.skin) {
                    if (inf.weight>0) {
                        if (inf.bone<0) return false;
                        total+=inf.weight;
                    }
                }
                if (total>0.0001f && Math.abs(total-1f)>0.02f) return false;
            }
            return true;
        }
    }

    /** A named point on a rig, e.g. hand_grip, mouth, saddle, tail_tip, or door_handle. */
    public static final class Anchor {
        public final String name;
        public final int bone;
        public final Transform local = new Transform();
        public Anchor(String name, int bone) { this.name=name==null?"anchor":name; this.bone=bone; }
    }

    public static final class Asset {
        public final String id;
        public final String kind; // ANIMAL, PROP, ENVIRONMENT, VEHICLE, FURNITURE, ARCHITECTURE
        public final List<Bone> bones = new ArrayList<>();
        public final List<Mesh> meshes = new ArrayList<>();
        public final List<Anchor> anchors = new ArrayList<>();
        public final Transform world = new Transform();
        public Asset(String id, String kind) {
            this.id=id==null?"asset":id;
            this.kind=kind==null?"PROP":kind.toUpperCase(java.util.Locale.ROOT);
        }
        public int addBone(String name,int parent) {
            if (parent>=bones.size() || parent < -1) throw new IllegalArgumentException("parent must precede child");
            bones.add(new Bone(name,parent)); return bones.size()-1;
        }
        public Asset addMesh(Mesh mesh) {
            if (mesh==null || !mesh.isValid()) throw new IllegalArgumentException("invalid mesh");
            meshes.add(mesh); return this;
        }
        public Asset addAnchor(String name,int bone,Transform local) {
            if (bone < -1 || bone>=bones.size()) throw new IllegalArgumentException("anchor bone");
            Anchor a=new Anchor(name,bone);
            if (local!=null) copy(local,a.local);
            anchors.add(a); return this;
        }
        public Anchor anchor(String name) {
            for (Anchor a:anchors) if (a.name.equals(name)) return a;
            return null;
        }
    }

    /**
     * Apply the current pose to mesh vertices. This conservative first implementation
     * uses hierarchical translation and XYZ rotation with normalized linear blending.
     * Production integration should cache matrices per frame and use inverse-bind
     * matrices; keep model-space source vertices immutable.
     */
    public static Vec3[] skin(Asset asset, Mesh mesh) {
        if (asset==null || mesh==null) return new Vec3[0];
        Vec3[] out=new Vec3[mesh.vertices.size()];
        Transform[] world=new Transform[asset.bones.size()];
        for (int i=0;i<asset.bones.size();i++) {
            Bone b=asset.bones.get(i);
            world[i]=compose(b.parent<0?null:world[b.parent],b.pose);
        }
        for (int i=0;i<mesh.vertices.size();i++) {
            Vertex v=mesh.vertices.get(i);
            Vec3 sum=new Vec3(); float total=0;
            for (Influence inf:v.skin) if (inf.bone>=0 && inf.bone<world.length && inf.weight>0) {
                Vec3 p=transformPoint(world[inf.bone],v.position);
                sum=sum.add(p.scale(inf.weight)); total+=inf.weight;
            }
            out[i]=total>0.0001f?sum.scale(1f/total):v.position.copy();
        }
        return out;
    }

    /** Apply smooth, bounded animal locomotion to a conventional quadruped skeleton. */
    public static void poseQuadruped(Asset a,float phase,float stride,float headLook,float tailSwing) {
        if (a==null) return;
        float s=Math.max(-1f,Math.min(1f,stride));
        for (Bone b:a.bones) {
            String n=b.name.toLowerCase(java.util.Locale.ROOT);
            if (n.contains("front_leg_l") || n.contains("foreleg_l")) b.pose.rotation.z=(float)Math.sin(phase)*22f*s;
            else if (n.contains("front_leg_r") || n.contains("foreleg_r")) b.pose.rotation.z=(float)Math.sin(phase+(float)Math.PI)*22f*s;
            else if (n.contains("hind_leg_l") || n.contains("rear_leg_l")) b.pose.rotation.z=(float)Math.sin(phase+(float)Math.PI)*25f*s;
            else if (n.contains("hind_leg_r") || n.contains("rear_leg_r")) b.pose.rotation.z=(float)Math.sin(phase)*25f*s;
            else if (n.contains("head")) b.pose.rotation.y=headLook;
            else if (n.contains("tail")) b.pose.rotation.y=tailSwing;
        }
    }

    /** Reset every animated bone to its bind pose before a new action clip is evaluated. */
    public static void resetPose(Asset a) {
        if (a==null) return;
        for (Bone b:a.bones) copy(b.bind,b.pose);
    }

    private static Transform compose(Transform parent,Transform local) {
        Transform r=local.copy();
        if (parent==null) return r;
        Vec3 p=transformPoint(parent,local.position);
        r.position.x=p.x; r.position.y=p.y; r.position.z=p.z;
        r.rotation.x+=parent.rotation.x; r.rotation.y+=parent.rotation.y; r.rotation.z+=parent.rotation.z;
        r.scale.x*=parent.scale.x; r.scale.y*=parent.scale.y; r.scale.z*=parent.scale.z;
        return r;
    }
    private static Vec3 transformPoint(Transform t,Vec3 p) {
        float x=p.x*t.scale.x, y=p.y*t.scale.y, z=p.z*t.scale.z;
        double ax=Math.toRadians(t.rotation.x), ay=Math.toRadians(t.rotation.y), az=Math.toRadians(t.rotation.z);
        float y1=(float)(y*Math.cos(ax)-z*Math.sin(ax)), z1=(float)(y*Math.sin(ax)+z*Math.cos(ax));
        float x2=(float)(x*Math.cos(ay)+z1*Math.sin(ay)), z2=(float)(-x*Math.sin(ay)+z1*Math.cos(ay));
        float x3=(float)(x2*Math.cos(az)-y1*Math.sin(az)), y3=(float)(x2*Math.sin(az)+y1*Math.cos(az));
        return new Vec3(x3+t.position.x,y3+t.position.y,z2+t.position.z);
    }
    private static void copy(Transform from,Transform to) {
        to.position.x=from.position.x; to.position.y=from.position.y; to.position.z=from.position.z;
        to.rotation.x=from.rotation.x; to.rotation.y=from.rotation.y; to.rotation.z=from.rotation.z;
        to.scale.x=from.scale.x; to.scale.y=from.scale.y; to.scale.z=from.scale.z;
    }
}
