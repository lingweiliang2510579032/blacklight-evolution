package com.codex.blacklight.movement;

/** Wall-local traversal shared by server authority and client prediction. Units: blocks/tick. */
public final class WallTraversal {
    public enum Mode { NONE, CLING, CLIMB, RUN, LEAP, MANTLE, VAULT }
    public record Input(int forward, int sideways, boolean sprint, boolean jump, boolean detach) {
        public Input { forward = Integer.signum(forward); sideways = Integer.signum(sideways); }
        public boolean moving() { return forward != 0 || sideways != 0; }
    }
    public record Surface(boolean torso, boolean lower, boolean blockedAbove,
                          double normalX, double normalZ, double approachDot) { }
    public record Frame(Mode mode, boolean handled, double x, double y, double z) { }

    private Mode mode = Mode.NONE;
    private double normalX, normalZ, lastY = Double.NaN, runSpeed;
    private int leapTicks, arcTicks, stallTicks, detachTicks;
    private boolean previousJump;
    private int chargeTicks;
    private double leapCharge;
    public int chargeTicks(){return chargeTicks;}
    public int visualChargeTicks(){return Math.round(chargeTicks/20F*MovementTuningSettings.jumpChargeTicks());}

    public Mode mode() { return mode; }
    public boolean attached() { return mode != Mode.NONE; }
    public double normalX() { return normalX; }
    public double normalZ() { return normalZ; }

    public Frame tick(Input input, Surface surface, double y, double incomingSpeed,
                      double configuredRun, double upgrade) {
        boolean jumpPressed = input.jump() && !previousJump;
        boolean jumpReleased = !input.jump() && previousJump;
        previousJump = input.jump();
        if (input.detach()) {
            boolean wasAttached = attached();
            double x = normalX * .18, z = normalZ * .18;
            clearSurface(); detachTicks = 8;
            return new Frame(Mode.NONE, wasAttached, x, -.08, z);
        }
        if (detachTicks > 0) { detachTicks--; return idle(); }
        if (mode == Mode.MANTLE || mode == Mode.VAULT) {
            int tick = arcTicks++;
            boolean vault = mode == Mode.VAULT;
            double inward = vault && tick < 3 ? -.32 : vault ? .42 : .48;
            double vertical = vault ? (tick < 3 ? .72 : .32) : (tick < 2 ? .32 : -.12);
            Frame result = frame(inward, vertical, 0);
            if (arcTicks >= 7) clearSurface();
            return result;
        }
        boolean wasAttached = attached();
        if (!wasAttached && (!surface.torso() || input.forward() <= 0 || surface.approachDot() > -.28)) return idle();
        if (!surface.torso() && !surface.lower()) {
            if (mode == Mode.LEAP && leapTicks > 0) return leap();
            clearSurface(); return idle();
        }
        if (surface.torso() || surface.lower()) {
            double length = Math.hypot(surface.normalX(), surface.normalZ());
            if (!Double.isFinite(length) || length < .001) { clearSurface(); return idle(); }
            normalX = surface.normalX()/length; normalZ = surface.normalZ()/length;
        }
        if (wasAttached && !surface.torso() && surface.lower() && (input.forward() > 0 || mode == Mode.LEAP)) {
            mode = Mode.MANTLE; arcTicks = 0; leapTicks = 0; stallTicks = 0;chargeTicks=0;
            return frame(.48, .44, 0);
        }
        double multiplier = Double.isFinite(upgrade) ? Math.clamp(upgrade, .25, 3) : 1;
        double target = Math.min(1.4, WallRunPhysics.climbVelocity(configuredRun,
                Math.max(incomingSpeed, runSpeed)) * 1.2 * multiplier);
        runSpeed = wasAttached ? runSpeed + Math.clamp(target-runSpeed, -.10, .10) : target;
        if (jumpPressed || !wasAttached&&input.jump()) chargeTicks=1;
        else if(input.jump()&&chargeTicks>0)chargeTicks=Math.min(20,chargeTicks+1);
        if (jumpReleased && chargeTicks>0) {
            leapCharge=chargeTicks/20.0;chargeTicks=0;
            mode = Mode.LEAP; leapTicks = 8; stallTicks = 0;
            return leap();
        }
        if (mode == Mode.LEAP && leapTicks > 0) return leap();
        mode = !input.moving() ? Mode.CLING : input.sprint() ? Mode.RUN : Mode.CLIMB;
        double speed = mode == Mode.RUN ? runSpeed : Math.min(.22, Math.max(.08, configuredRun*.32)*multiplier);
        double length = Math.max(1, Math.hypot(input.forward(),input.sideways()));
        double vertical = input.forward()*speed/length;
        double lateral = input.sideways()*speed/length;
        boolean stuck = vertical > .05 && surface.blockedAbove() && Double.isFinite(lastY) && y-lastY < .02;
        stallTicks = stuck ? stallTicks+1 : 0; lastY = y;
        if (stallTicks >= 3) {
            mode = Mode.VAULT; arcTicks = 0; stallTicks = 0;
            return frame(-.32, .72, 0);
        }
        return frame(.055, vertical, lateral);
    }

    private Frame leap() {
        mode = Mode.LEAP;
        double vertical = Math.max(runSpeed, .98 + leapCharge*.65 + runSpeed*.20 - (8-leapTicks)*.065);
        leapTicks--;
        return frame(.055, vertical, 0);
    }
    private Frame frame(double inward, double vertical, double lateral) {
        // Looking into the wall: D follows its right tangent, W goes upward.
        return new Frame(mode, true, -normalX*inward + normalZ*lateral,
                vertical, -normalZ*inward - normalX*lateral);
    }
    private Frame idle() { return new Frame(Mode.NONE, false, 0, 0, 0); }
    private void clearSurface() {
        mode=Mode.NONE;normalX=normalZ=runSpeed=0;lastY=Double.NaN;leapTicks=arcTicks=stallTicks=chargeTicks=0;leapCharge=0;
    }
    public void reset() { clearSurface();previousJump=false;detachTicks=0; }
}
