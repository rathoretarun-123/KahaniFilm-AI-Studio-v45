package com.tarun.kahani.core;

/**
 * Deterministic, frame-rate-independent motion primitives for everyday character and prop animation.
 *
 * This is a foundation, not a full rigid-body solver or a substitute for a production character rig.
 * Every method derives its result from absolute time so frames can be rendered out of order safely.
 */
public final class EverydayPhysics {
    private EverydayPhysics() {}

    public static final float GRAVITY = 9.80665f;

    public static final class Vec3 {
        public final float x, y, z;
        public Vec3(float x, float y, float z) { this.x=x; this.y=y; this.z=z; }
        public Vec3 add(Vec3 b) { return new Vec3(x+b.x,y+b.y,z+b.z); }
        public Vec3 scale(float k) { return new Vec3(x*k,y*k,z*k); }
        public float length() { return (float)Math.sqrt(x*x+y*y+z*z); }
    }

    public static final class State {
        public final Vec3 position, velocity;
        public final boolean grounded;
        public State(Vec3 p, Vec3 v, boolean g) { position=p; velocity=v; grounded=g; }
    }

    /** Ballistic motion with a floor, coefficient of restitution, and tangential friction. */
    public static State projectile(Vec3 p0, Vec3 v0, float elapsed, float floorY,
                                   float restitution, float friction) {
        float t=Math.max(0,elapsed), e=clamp(restitution,0,0.95f), f=clamp(friction,0,1);
        float x=p0.x+v0.x*t, z=p0.z+v0.z*t;
        float y=p0.y+v0.y*t-0.5f*GRAVITY*t*t, vy=v0.y-GRAVITY*t;
        if (p0.y<=floorY && v0.y<=0) return new State(new Vec3(x,floorY,z),new Vec3(v0.x,v0.y,v0.z),true);
        // Resolve repeated bounces analytically by stepping a fixed number of contact events.
        float remaining=t, py=p0.y, pv=v0.y, px=p0.x, pz=p0.z, vx=v0.x, vz=v0.z;
        for(int hit=0;hit<12 && remaining>0;hit++) {
            float disc=pv*pv+2*GRAVITY*(py-floorY);
            if(disc<0) break;
            float impact=(pv+(float)Math.sqrt(disc))/GRAVITY;
            if(impact<0 || impact>remaining) {
                y=py+pv*remaining-0.5f*GRAVITY*remaining*remaining;
                vy=pv-GRAVITY*remaining; x=px+vx*remaining; z=pz+vz*remaining;
                return new State(new Vec3(x,Math.max(floorY,y),z),new Vec3(vx,Math.max(0,vy),vz),y<=floorY+1e-4f && vy<=0);
            }
            px+=vx*impact; pz+=vz*impact; remaining-=impact; py=floorY;
            pv=(pv-GRAVITY*impact)*e; vx*=1-f; vz*=1-f;
            if(Math.abs(pv)<0.12f || e<0.01f) {
                float slide=Math.min(remaining,0.25f); px+=vx*slide; pz+=vz*slide;
                return new State(new Vec3(px,floorY,pz),new Vec3(vx,0,vz),true);
            }
        }
        return new State(new Vec3(x,Math.max(floorY,y),z),new Vec3(v0.x,vy,v0.z),y<=floorY+1e-4f);
    }

    /** Critically damped-style spring approximation useful for hands, head settle, cloth and carried props. */
    public static float spring(float target, float start, float elapsed, float frequency, float damping) {
        float t=Math.max(0,elapsed), w=Math.max(0.01f,frequency), d=clamp(damping,0,1.5f);
        float e=(float)Math.exp(-d*w*t);
        return target+(start-target)*(1+w*t)*e;
    }

    /** Smooth anticipation / acceleration / deceleration for a walk, reach, sit or stand transition. */
    public static float ease(float from, float to, float elapsed, float duration) {
        float u=clamp(elapsed/Math.max(1e-4f,duration),0,1);
        u=u*u*(3-2*u);
        return from+(to-from)*u;
    }

    /** Normalized gait cycle. Returns [left foot lift, right foot lift, body bounce]. */
    public static float[] walkCycle(float time, float cadenceHz, float stride, float weight) {
        float phase=(float)(time*Math.max(0.1f,cadenceHz)*Math.PI*2);
        float left=(float)Math.max(0,Math.sin(phase))*stride;
        float right=(float)Math.max(0,Math.sin(phase+(float)Math.PI))*stride;
        float bounce=(float)Math.max(0,Math.sin(phase*2))*weight;
        return new float[]{left,right,bounce};
    }

    /** Stable deterministic noise: avoids random flicker when frames are rendered in parallel. */
    public static float noise(int seed, float time, float frequency) {
        float f=Math.max(0.001f,frequency), cell=(float)Math.floor(time*f), u=time*f-cell;
        float a=hash(seed+(int)cell), b=hash(seed+(int)cell+1);
        u=u*u*(3-2*u);
        return a+(b-a)*u;
    }

    /** Soft contact response for a foot/hand approaching a surface; 1 at contact, 0 outside radius. */
    public static float contact(float distance, float radius) {
        float u=1-clamp(Math.abs(distance)/Math.max(1e-4f,radius),0,1);
        return u*u*(3-2*u);
    }

    private static float hash(int n) {
        int x=n; x=(x^61)^(x>>>16); x+=x<<3; x^=x>>>4; x*=0x27d4eb2d; x^=x>>>15;
        return (x&0x7fffffff)/(float)0x3fffffff-1f;
    }
    private static float clamp(float x,float lo,float hi) { return Math.max(lo,Math.min(hi,x)); }
}
