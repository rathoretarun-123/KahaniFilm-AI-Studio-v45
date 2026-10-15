package com.tarun.kahani.jvm;

import com.tarun.kahani.core.ProceduralWorldAssets;
import com.tarun.kahani.core.Studio3D;
import com.tarun.kahani.core.Nature;
import com.tarun.kahani.core.StoryReasoner;
import com.tarun.kahani.core.EverydayPhysics;

/** Lightweight deterministic smoke tests for generated animal, prop and environment meshes. */
public final class WorldAssetSmokeTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static int visible(Studio3D.Picture p) {
        int n=0;
        if (p==null || p.px==null) return 0;
        for (int c:p.px) if ((c>>>24)>8) n++;
        return n;
    }

    private static long difference(Studio3D.Picture a, Studio3D.Picture b) {
        long n=0;
        for (int i=0;i<Math.min(a.px.length,b.px.length);i++) if (a.px[i]!=b.px[i]) n++;
        return n;
    }

    private static void phenomenonReaderChecks() {
        java.util.List<StoryReasoner.PhenomenonPlan> plans = StoryReasoner.phenomena(
                "At night lightning flashes, thunder rolls, the ceiling fan rotates, and stars twinkle.");
        boolean lightning=false, thunder=false, fan=false, stars=false;
        for (StoryReasoner.PhenomenonPlan p : plans) {
            lightning |= p.kind.equals("LIGHTNING");
            thunder |= p.kind.equals("THUNDER");
            fan |= p.kind.equals("FAN");
            stars |= p.kind.equals("STARS");
        }
        check(lightning && thunder && fan && stars,
                "reader should identify environmental cues and provide director-facing guidance");
    }

    private static void natureChecks() {
        check(Nature.lightningFlash(1.0f,1.0f,1.0f)>0.9f,"lightning should flash at strike time");
        check(Nature.lightningFlash(2.0f,1.0f,1.0f)==0f,"lightning should fade after its envelope");
        check(Nature.localFlameLight(0,3,1,1)>Nature.localFlameLight(4,3,1,1),
                "flame light must be local and fall off with distance");
        float fan=Nature.fanSpeed(2,0,1,true);
        check(fan>0.9f && fan<=1,"fan should approach its target speed");
        float angle=Nature.fanAngle(1,2,0);
        check(angle>0,"fan blade angle should advance over time");
        float flap=Nature.flapAngle(0.2f,0,1,0.5f,0.5f);
        check(Math.abs(flap)<=0.5f,"window/shade flap must stay within hinge limits");
        float star=Nature.starBrightness(1,5);
        check(star>=0.35f && star<=1,"star twinkle must stay bounded");
        check(Nature.vehicleDistance(0.5f,0,1,100)>0 && Nature.vehicleDistance(0.5f,0,1,100)<100,
                "vehicle travel should interpolate along its path");
        check(Nature.ambienceEnvelope(0.5f,0,1,0.2f)>0,"ambience should fade in smoothly");
    }

    private static void activityReaderChecks() {
        java.util.List<StoryReasoner.ActivityPlan> plans = StoryReasoner.activities(
                "She drives a car, typing message on phone, climb stairs, drink tea, watch TV.");
        check(plans.size() >= 5, "reader should recognise multiple explicit everyday activities");
        boolean car=false, text=false, stairs=false, tea=false, tv=false;
        for (StoryReasoner.ActivityPlan p : plans) {
            car |= p.family.equals("DRIVE_CAR");
            text |= p.family.equals("PHONE_TEXT");
            stairs |= p.family.equals("STAIRS");
            tea |= p.family.equals("TEA");
            tv |= p.family.equals("WATCH_TV");
        }
        check(car && text && stairs && tea && tv, "reader should return the expected activity families");
        check(StoryReasoner.activities("They quietly look at the sky.").isEmpty(),
                "reader must not invent an activity when no rule matches");
    }

    private static void physicsChecks() {
        float a=EverydayPhysics.ease(0,10,0.5f,1f);
        check(a>0 && a<10,"ease should smoothly interpolate");
        float settle=EverydayPhysics.spring(1,0,1f,5f,1f);
        check(settle>0.9f && settle<=1.01f,"spring should settle toward target");
        float[] gait=EverydayPhysics.walkCycle(0.2f,2f,0.25f,0.08f);
        check(gait.length==3 && gait[0]>=0 && gait[1]>=0,"walk cycle should return valid foot/body motion");
        EverydayPhysics.State flight=EverydayPhysics.projectile(
                new EverydayPhysics.Vec3(0,2,0),new EverydayPhysics.Vec3(1,0,0),
                0.2f,0,0.45f,0.1f);
        check(flight.position.y>=0,"projectile must not pass through the floor");
        float n1=EverydayPhysics.noise(7,1.25f,2f), n2=EverydayPhysics.noise(7,1.25f,2f);
        check(n1==n2,"noise must be deterministic for repeatable rendering");
        check(EverydayPhysics.contact(0,1)==1f,"surface contact should be full at zero distance");
    }

    public static void main(String[] args) {
        physicsChecks();
        activityReaderChecks();
        natureChecks();
        phenomenonReaderChecks();
        ProceduralWorldAssets.Model animal=ProceduralWorldAssets.quadruped("smoke-dog",0xFF9C704A,0xFF4D3326);
        check(animal.triangles()>100,"quadruped should contain a real triangle mesh");
        Studio3D.Picture idle=animal.render(96,96,1);
        check(visible(idle)>100,"quadruped render should contain visible pixels");
        animal.poseQuadruped(0.4f,1f,12f,25f);
        Studio3D.Picture walking=animal.render(96,96,1);
        check(difference(idle,walking)>5,"animal pose must change the rendered image");

        ProceduralWorldAssets.Model bird=ProceduralWorldAssets.bird("smoke-bird",0xFF4C8DCE,0xFFFFB52E);
        check(bird.triangles()>50,"bird should contain a triangle mesh");
        bird.poseBird(0.2f,1f,8f);
        check(visible(bird.render(96,96,1))>50,"bird render should contain visible pixels");

        ProceduralWorldAssets.Model prop=ProceduralWorldAssets.prop("radio",0xFF4C5966);
        check(prop.triangles()>10,"radio prop should contain geometry");

        ProceduralWorldAssets.Model place=ProceduralWorldAssets.location("village",42);
        check(place.triangles()>100,"village should contain ground and modular geometry");
        check(visible(place.render(128,96,1))>1000,"location render should have a visible environment");

        System.out.println("WORLD_ASSET_SMOKE_OK animalTriangles="+animal.triangles()
                +" birdTriangles="+bird.triangles()+" propTriangles="+prop.triangles()
                +" locationTriangles="+place.triangles());
    }
}
